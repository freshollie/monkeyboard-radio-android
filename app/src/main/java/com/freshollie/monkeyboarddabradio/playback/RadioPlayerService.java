package com.freshollie.monkeyboarddabradio.playback;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.freshollie.monkeyboarddabradio.radio.ListenerManager;
import com.freshollie.monkeyboarddabradio.radio.RadioDevice;

/**
 * Created by Freshollie on 14/01/2017.
 */

/**
 * Radio Player Service manages the control of the radio.
 *
 */
public class RadioPlayerService extends Service implements AudioManager.OnAudioFocusChangeListener {
    private String TAG = this.getClass().getSimpleName();
    static int ATTACH_TIMEOUT = 20000; // Radio will stop trying to connect after 20 seconds

    private IBinder binder = new RadioPlayerBinder();

    private RadioPlayerNotification playerNotification;
    private RadioDevice radio;

    private ListenerManager listenerManager;

    private AudioManager audioManager;

    private int volume = 13;
    private int duckVolume = 3;

    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }

    private AudioFocus audioFocusState = AudioFocus.NoFocusNoDuck;

    private int currentChannel = 0;

    public class RadioPlayerBinder extends Binder {
        RadioPlayerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return RadioPlayerService.this;
        }
    }

    private ListenerManager.ConnectionStateChangeListener connectionStateListener =
            new ListenerManager.ConnectionStateChangeListener() {
        @Override
        public void onStart() {
            onPlay();
        }

        @Override
        public void onStop() {

        }
    };

    private Thread connectThread =
            new Thread(new Runnable() {
                @Override
                public void run() {
                    long startTime = SystemClock.currentThreadTimeMillis();

                    while (!radio.isAttached()
                            && (startTime - SystemClock.currentThreadTimeMillis()) < ATTACH_TIMEOUT) {

                    }

                    if (radio.isAttached()) {
                        new Handler(getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                radio.connect();
                            }
                        });
                    } else {
                        new Handler(getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                onAttachTimeout();
                            }
                        });
                    }
                }
            });

    @Override
    public void onCreate(){
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        radio = new RadioDevice(getApplicationContext());
        listenerManager = radio.getListenerManager();

        playerNotification = new RadioPlayerNotification(this);
    }

    @Override
    public IBinder onBind(Intent intent){
        return binder;
    }

    public RadioDevice getRadio() {
        return radio;
    }

    public int getVolume() {
        return volume;
    }

    private void startConnection() {
        if (!connectThread.isAlive()) {
            connectThread.start();
        }
    }

    public void onAttachTimeout() {
        Log.v(TAG, "Timed out waiting for device to attach");
    }

    /**
     * returns text information about the current channel
     */

    public void onPlay() {
        if (radio.isConnected()) {
            if (!isPlaying()) {

                if (!hasFocus()) {
                    requestAudioFocus();
                }

                if (hasFocus()) {
                    radio.play(currentChannel);
                }
            }
        } else {
            startConnection();
        }
    }

    public void onSetVolume(int newVolume) {
        volume = newVolume;
    }

    public boolean onMute() {
        return radio.setVolume(0);
    }

    public void onUnMute() {
        radio.setVolume(volume);
    }

    public void onStop() {
        onMute();
    }

    public void onFocusDuck() {
        radio.setVolume(duckVolume);
    }

    public void onFocusGain() {
        radio.setVolume(volume);
    }

    public void onFocusLost() {
        onStop();
    }

    public boolean hasFocus () {
        return audioFocusState == AudioFocus.Focused;
    }

    public boolean isPlaying() {
        return radio.getPlayStatus() == RadioDevice.Values.PLAY_STATUS_STREAM_STOP;
    }


    public void requestAudioFocus() {
        int result = audioManager.requestAudioFocus(this,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            audioFocusState = AudioFocus.Focused;
        } else {
            audioFocusState = AudioFocus.NoFocusCanDuck;
        }
    }

    public void abandonAudioFocus() {
        if (hasFocus()) {
            audioManager.abandonAudioFocus(this);
        }
        audioFocusState = AudioFocus.NoFocusNoDuck;
    }

    @Override
    public void onAudioFocusChange(int focus){
        switch(focus) {
            case AudioManager.AUDIOFOCUS_LOSS:
                // Another app has gained focus;
                onFocusLost();
                break;
            case AudioManager.AUDIOFOCUS_REQUEST_FAILED:

        }
    }

    @Override
    public void onDestroy() {

    }
}
