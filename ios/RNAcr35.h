#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import <React/RCTBridgeModule.h>
#endif
#import <React/RCTEventEmitter.h>

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import "AudioJack.h"

@interface RNAcr35 : RCTEventEmitter <RCTBridgeModule>


@end
  
