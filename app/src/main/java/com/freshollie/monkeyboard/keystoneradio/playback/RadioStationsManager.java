package com.freshollie.monkeyboard.keystoneradio.playback;

import com.freshollie.monkeyboard.keystoneradio.radio.RadioStation;

/**
 * Created by freshollie on 27.11.17.
 */

public class RadioStationsManager {
    public interface CopyProgramsListener {
        void onProgressUpdate(int progress, int max);
        void onComplete(RadioStation[] stationList);
    }

    public interface DABSearchListener {
        void onStarted();
        void onProgressUpdate(int numPrograms, int progress);
        void onComplete(int numPrograms);
    }

    public RadioStationsManager(RadioPlayback communicationManager) {

    }
}
