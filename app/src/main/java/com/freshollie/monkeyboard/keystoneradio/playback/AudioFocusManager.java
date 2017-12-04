package com.freshollie.monkeyboard.keystoneradio.playback;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

/**
 * Created by freshollie on 29.11.17.
 */

public class AudioFocusManager implements AudioManager.OnAudioFocusChangeListener {
    private static final String TAG = AudioFocusManager.class.getSimpleName();

    private boolean muted = false;
    private boolean ducked = false;

    private Context context;

    private RadioPlaybackManager radioPlaybackManager;

    private AudioManager audioManager;

    private enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }

    private AudioFocus audioFocusState = AudioFocus.NoFocusNoDuck;

    public AudioFocusManager(RadioPlaybackManager playbackManager) {
        radioPlaybackManager = playbackManager;
        audioManager = playbackManager.getAudioManager();
    }

    public AudioFocus requestAudioFocus() {
        Log.v(TAG, "Requesting Audio Focus");
        int result = audioManager.requestAudioFocus(this,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.v(TAG, "Request granted");
            audioFocusState = AudioFocus.Focused;
        } else {
            Log.v(TAG, "Could not gain full focus");
            audioFocusState = AudioFocus.NoFocusCanDuck;
        }

        return audioFocusState;
    }

    private void abandonAudioFocus() {
        Log.v(TAG, "Abandoning audio focus");
        if (hasFocus()) {
            audioManager.abandonAudioFocus(this);
        }
        audioFocusState = AudioFocus.NoFocusNoDuck;
    }

    @Override
    public void onAudioFocusChange(int focus){
        Log.v(TAG, "Audio focus changed");
        switch(focus) {
            case AudioManager.AUDIOFOCUS_LOSS:
                // Another app has gained focus;
                handleFocusLost();
                break;
            case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                handlePauseRequest();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (radio.isAttached()) {
                    handleFocusDuck();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (radio.isAttached()) {
                    handleMuteRequest();
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                handleFocusGain();
                break;
        }
    }
}
