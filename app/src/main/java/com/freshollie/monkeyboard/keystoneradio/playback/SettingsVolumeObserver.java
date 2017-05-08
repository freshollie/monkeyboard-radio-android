package com.freshollie.monkeyboard.keystoneradio.playback;

/**
 * Created by freshollie on 08/05/17.
 */

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;

/**
 * Created by freshollie on 08/05/17.
 */

public class SettingsVolumeObserver extends ContentObserver {
    Context context;

    private int lastVolume = 0;

    private SettingsVolumeChangeListener listener;

    public interface SettingsVolumeChangeListener {
        void onChange(int newVolume);
    }

    public SettingsVolumeObserver(Context c, Handler handler, SettingsVolumeChangeListener callback) {
        super(handler);
        context = c;
        listener = callback;

        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        lastVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

    }

    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

        if (listener != null && currentVolume != lastVolume) {
            listener.onChange(currentVolume);
            lastVolume = currentVolume;
        }
    }
}