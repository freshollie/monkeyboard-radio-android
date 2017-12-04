package com.freshollie.monkeyboard.keystoneradio.playback;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.media.VolumeProviderCompat;

import com.freshollie.monkeyboard.keystoneradio.R;

/**
 * Created by freshollie on 27.11.17.
 */

/**
 * Volume manager handles the volume of the player
 */
public class VolumeManager  {
    // Default volume
    private int volume = 13;

    public static int MAX_VOLUME = 15;

    private Context context;
    private RadioPlaybackManager radioPlaybackManager;

    private ContentObserver volumeSettingsObserver;
    private VolumeProviderCompat volumeProvider;
    private SharedPreferences sharedPreferences;

    private AudioManager audioManager;

    // Listen for settings changes and see if volume changes
    // Only required before android 5.0
    private class VolumeSettingsObserver extends ContentObserver {
        VolumeSettingsObserver(Context context) {
            super(new Handler(context.getMainLooper()));
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            setVolume(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        }
    }

    // Listens for android 5.0+ volume changes on a special audio channel
    private class VolumeChangeListener extends VolumeProviderCompat {
        public VolumeChangeListener(Context context) {
            super(
                    VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE,
                    MAX_VOLUME,
                    volume
            );
        }

        @Override
        public void onSetVolumeTo(int volume) {
            super.onSetVolumeTo(volume);
            setVolume(volume);
        }

        @Override
        public void onAdjustVolume(int direction) {
            super.onAdjustVolume(direction);
            if (direction != 0) {
                setVolume(volume + direction);
            }
        }
    }


    public VolumeManager(Context c, RadioPlaybackManager playbackManager) {
        context = c;
        radioPlaybackManager = playbackManager;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (sharedPreferences.getBoolean(context.getString(R.string.pref_sync_volume_key), false)) {
            volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        } else {
            volume = sharedPreferences.getInt(context.getString(R.string.saved_volume_key), volume);
        }

        setupVolumeListener();
    }

    private void setupVolumeListener() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // Make an observer of the settings, which listens for if the volume changes
            volumeSettingsObserver = new VolumeSettingsObserver(context);

            // Register the observer
            context.getContentResolver()
                    .registerContentObserver(
                            android.provider.Settings.System.CONTENT_URI,
                            true,
                            volumeSettingsObserver
                    );

        } else {

            volumeProvider = new VolumeChangeListener(context);
        }
    }

    private void setVolume(int newVolume) {
        if (newVolume != volume) {
            if (newVolume < MAX_VOLUME && volume > -1) {
                volume = newVolume;
                radioPlaybackManager.handleSetVolume(newVolume);
            }
        }
    }

    public int getVolume() {
        return volume;
    }
}