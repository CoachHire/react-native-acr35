
#import "RNAcr35.h"
#import <AVFoundation/AVFoundation.h>

@implementation RNAcr35

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

RCT_EXPORT_MODULE();

- (NSArray<NSString *> *)supportedEvents
{
    return @[@"NfcId"];
}

/** AudioJackReader object */
ACRAudioJackReader *_reader;

/** Is this plugin being initialised? */
bool firstRun = true;
/** Is this the first reset of the reader? */
bool firstReset = true;

/** APDU command for reading a card's UID */
uint8_t commandApdu[] = { 0xFF, 0xCA, 0x00, 0x00, 0x00 };
/** the integer representing card type */
NSUInteger cardType;
/** Timeout for APDU response (in <b>seconds</b>) */
NSUInteger timeout = 1;

/** Stop the polling thread? */
bool killThread = false;
/** Is the reader currently connected? */
bool readerConnected = true;
/** The number of iterations that have passed with no response from the reader */
int itersWithoutResponse = 0;

- (NSString *)hexStringFromByteArray:(const uint8_t *)buffer length:(NSUInteger)length {
    
    NSString *hexString = @"";
    NSUInteger i = 0;
    
    for (i = 0; i < length; i++) {
        if (i == 0) {
            hexString = [hexString stringByAppendingFormat:@"%02X", buffer[i]];
        } else {
            hexString = [hexString stringByAppendingFormat:@" %02X", buffer[i]];
        }
    }
    
    return hexString;
}

NSError * acr35Error(NSString *errMsg)
{
    NSError *error = [NSError errorWithDomain:@"RNAcr35" code:200 userInfo:@{@"reason": errMsg}];
    return error;
}

RCT_EXPORT_METHOD(sleep:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    @try {
        /* Kill the polling thread */
        killThread = true;
        /* Send a success message back to Cordova */
        resolve(@"asleep");
    }
    @catch (NSException *exception) {
        NSString* errorMessage = [NSString stringWithFormat:@"{\"message\":\"Failed to sleep\",\"actual-error\":%@}", exception];
        reject(@"-1", errorMessage, nil);
    }
}

- (BOOL)isHeadsetPluggedIn {
    AVAudioSessionRouteDescription* route = [[AVAudioSession sharedInstance] currentRoute];
    for (AVAudioSessionPortDescription* desc in [route outputs]) {
        if ([[desc portType] isEqualToString:AVAudioSessionPortHeadphones])
            return YES;
    }
    return NO;
}

RCT_EXPORT_METHOD(read: (NSString *)type resolve:(RCTPromiseResolveBlock) resolve
                  rejecter:(RCTPromiseRejectBlock) reject) {
    
    bool *headsetPluggedIn = [self isHeadsetPluggedIn];
    if(!headsetPluggedIn){
        NSString* errorMessage = [NSString stringWithFormat:@"No reader connected"];
        return reject(@"1", errorMessage, nil);
    }
    
    float vol = [[AVAudioSession sharedInstance] outputVolume];
    if(vol < 1){
        NSString* errorMessage = [NSString stringWithFormat:@"Volume must set to maximum"];
        return reject(@"2", errorMessage, nil);
    }
    
    /* Class variables require initialisation on first launch */
    if(firstRun){
        // Initialize ACRAudioJackReader object.
        _reader = [[ACRAudioJackReader alloc] init];
        [_reader setDelegate:self];
        firstRun = false;
    }
    firstReset = true;
    
    /* Set the card type */
    cardType = [type intValue];
    
    /* Reset the reader */
    [_reader reset];
    
    resolve(@"started");
}

- (void)transmit {
    NSLog(@"iteration");
    // Power on the PICC.
    [_reader piccPowerOnWithTimeout:timeout cardType:cardType];
    // Transmit the APDU.
    [_reader piccTransmitWithTimeout:timeout commandApdu:commandApdu length:sizeof(commandApdu)];
}

- (void)resetFunction {
    if (firstReset){
        /* Set the reader asleep */
        [_reader sleep];
        /* Wait one second */
        [NSThread sleepForTimeInterval:1.0];
        /* Reset the reader */
        [_reader reset];
        
        firstReset = false;
    }
    /* Sends the APDU command for reading a card UID every second */
    else {
        /* Wait one second for stability */
        [NSThread sleepForTimeInterval:1.0];
        
        while(!killThread){
            /* If the reader is not connected, increment no. of iterations without response */
            if(!readerConnected){
                itersWithoutResponse++;
            }
            /* Else, reset the number of iterations without a response */
            else{
                itersWithoutResponse = 0;
            }
            /* Reset the connection state */
            readerConnected = false;
            
            /* If we have waited 3 seconds without a response */
            // TODO: check media volume and whether any device is plugged into audio socket
            if(itersWithoutResponse == 4) {
                /* Communicate to the application that the reader is disconnected */
                // some sort of error event in event stream
                /* Kill this thread */
                killThread = true;
                
                NSLog(@"disconnected");
                itersWithoutResponse = 0;
            }
            else{
                [self transmit];
                /* Repeat every second */
                [NSThread sleepForTimeInterval:1.0];
            }
        }
        /* Power off the PICC */
        [_reader piccPowerOff];
        /* Set the reader asleep */
        [_reader sleep];
        
        killThread = false;
    }
}

- (void)readerDidReset:(ACRAudioJackReader *)reader {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        [self resetFunction];
    });
    
}

- (void)reader:(ACRAudioJackReader *)reader didSendPiccResponseApdu:(const uint8_t *)responseApdu
        length:(NSUInteger)length {
    /* Update the connection status */
    readerConnected = true;
    NSString *uid = [self hexStringFromByteArray:responseApdu length:(sizeof(responseApdu)*2) - 1];
    
    /* Send the card UID to the application */
    [self sendEventWithName:@"NfcId" body:@{@"cardId": uid}];
    /* Print out the UID */
    NSLog( @"%@",uid);
}

@end
