
package com.coachhire.acr35;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.acs.audiojack.AudioJackReader;
import com.acs.audiojack.ReaderException;

import android.media.AudioManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import java.lang.Override;
import java.lang.Runnable;
import java.lang.System;
import java.lang.Thread;
import java.util.Locale;

public class RNAcr35Module extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    public RNAcr35Module(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    private Transmitter transmitter;
    private AudioManager mAudioManager;
    private AudioJackReader mReader;

    private boolean firstRun = true;
    /**
     * Is this plugin being initialised?
     */
    private boolean firstReset = true;  /** Is this the first reset of the reader? */

    /**
     * APDU command for reading a card's UID
     */
    private final byte[] apdu = {(byte) 0xFF, (byte) 0xCA, (byte) 0x00, (byte) 0x00, (byte) 0x00};
    /**
     * Timeout for APDU response (in <b>seconds</b>)
     */
    private final int timeout = 1;

    @Override
    public String getName() {
        return "RNAcr35";
    }

    /**
     * Converts raw data into a hexidecimal string
     *
     * @param buffer: raw data in the form of a byte array
     * @return a string containing the data in hexidecimal form
     */
    private String bytesToHex(byte[] buffer) {
        String bufferString = "";
        if (buffer != null) {
            for (int i = 0; i < buffer.length; i++) {
                String hexChar = Integer.toHexString(buffer[i] & 0xFF);
                if (hexChar.length() == 1) {
                    hexChar = "0" + hexChar;
                }
                bufferString += hexChar.toUpperCase(Locale.US) + " ";
            }
        }
        return bufferString;
    }

    /**
     * Checks if the device media volume is set to 100%
     *
     * @return true if media volume is at 100%
     */
    private boolean maxVolume() {
        int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        if (currentVolume < maxVolume) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Sets the ACR35 reader to continuously poll for the presence of a card. If a card is found,
     * the UID will be returned
     *
     * @param cardType: the integer representing card type
     * @param promise:  the promise to resolve to the bridge
     */
    @ReactMethod
    private void read(final int cardType, final Promise promise) {
        try {
            System.out.println("setting up for reading...");
            firstReset = true;

            /* If no device is plugged into the audio socket or the media volume is < 100% */
            if (!mAudioManager.isWiredHeadsetOn()) {
                /* Communicate to the application that the reader is unplugged */
                promise.reject("Device is unplugged");
            } else if (!maxVolume()) {
                /* Communicate to the application that the media volume is low */
                promise.reject("Volume is too low");
            }

            /* Set the PICC response APDU callback */
            mReader.setOnPiccResponseApduAvailableListener
                    (new AudioJackReader.OnPiccResponseApduAvailableListener() {
                        @Override
                        public void onPiccResponseApduAvailable(AudioJackReader reader,byte[] responseApdu) {
                            /* Update the connection status of the transmitter */
                            transmitter.updateStatus(true);
                            /* Send the card UID to the application */
                            promise.resolve(bytesToHex(responseApdu));
                            /* Print out the UID */
                            System.out.println(bytesToHex(responseApdu));
                        }
                    });

            /* Set the reset complete callback */
            mReader.setOnResetCompleteListener(new AudioJackReader.OnResetCompleteListener() {
                @Override
                public void onResetComplete(AudioJackReader reader) {
                    System.out.println("reset complete");

                /* If this is the first reset, the ACR35 reader must be turned off and back on again
                   to work reliably... */
                    if (firstReset) {
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    /* Set the reader asleep */
                                    mReader.sleep();
                                    /* Wait one second */
                                    Thread.sleep(1000);
                                    /* Reset the reader */
                                    mReader.reset();

                                    firstReset = false;
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    // TODO: add exception handling
                                }
                            }
                        }).start();
                    } else {
                        /* Create a new transmitter for the UID read command */
                        transmitter = new Transmitter(mReader, mAudioManager, timeout,
                                apdu, cardType, promise);
                        /* has its own thread management system */
                        new Thread(transmitter).start();
                    }
                }
            });

            mReader.start();
            mReader.reset();
            System.out.println("setup complete");
        } catch (Exception ex) {
            promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
    }

}