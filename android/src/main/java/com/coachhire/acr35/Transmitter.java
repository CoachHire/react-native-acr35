package com.coachhire.acr35;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioDeviceInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.util.Log;

import com.acs.audiojack.AesTrackData;
import com.acs.audiojack.AudioJackReader;
import com.acs.audiojack.DukptTrackData;
import com.acs.audiojack.Result;
import com.acs.audiojack.TrackData;

import java.lang.Thread;

/**
 * This class sets up an independent thread for card polling, and is linked to the
 * <code>setOnPiccResponseApduAvailableListener</code> callback function
 */
public class Transmitter implements Runnable {
    private static final String TAG = "RNAcr35Transmitter";
    private AudioJackReader mReader;
    private AudioManager mAudioManager;

    private boolean killMe = false;
    /**
     * Stop the polling thread?
     */
    private int itersWithoutResponse = 0;
    /**
     * The number of iterations that have passed with no
     * response from the reader
     */
    private boolean readerConnected = true;
    /**
     * Is the reader currently connected?
     */

    private final int cardType;
    private int timeout;
    private byte[] apdu;

    /**
     * @param mReader:       AudioJack reader service
     * @param mAudioManager: system audio service
     * @param timeout:       time in <b>seconds</b> to wait for commands to complete
     * @param apdu:          byte array containing the command to be sent
     * @param cardType:      the integer representing card type
     */
    public Transmitter(AudioJackReader mReader, AudioManager mAudioManager, int timeout, byte[] apdu, int cardType) {
        this.mReader = mReader;
        this.mAudioManager = mAudioManager;
        this.timeout = timeout;
        this.apdu = apdu;
        this.cardType = cardType;
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
     * Check for plugged in audio device
     */
    private boolean hasWiredHeadset() {
        final AudioDeviceInfo[] devices = mAudioManager.getDevices(AudioManager.GET_DEVICES_ALL);
        for (AudioDeviceInfo device : devices) {
            final int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stops the polling thread
     */
    public void kill() {
        killMe = true;
    }

    /**
     * Updates the connection status of the reader (links to APDU response callback)
     */
    public void updateStatus(boolean status) {
        readerConnected = status;
    }

    /**
     * Sends the APDU command for reading a card UID every second
     */
    @Override
    public void run() {
        try {
            /* Wait one second for stability */
            Thread.sleep(1000);

            while (!killMe) {
              Log.d(TAG, "Reading...");
              /* Power on the PICC */
              mReader.piccPowerOn(timeout, cardType);
              /* Transmit the APDU */
              mReader.piccTransmit(timeout, apdu);
              /* Repeat every second */
              Thread.sleep(1000);
            }
            /* Power off the PICC */
            mReader.piccPowerOff();
            /* Set the reader asleep */
            mReader.sleep();
            /* Stop the reader service */
            mReader.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
            // TODO: add exception handling
        }
    }

}