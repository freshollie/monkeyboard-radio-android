package com.freshollie.monkeyboard.keystoneradio.radio;

import android.os.Handler;

import java.util.ArrayList;

/**
 * Created by Freshollie on 12/01/2017.
 *
 */

public class ListenerManager {

    /**
     * Manager used to notify given listeners when attributes about the connected radio change
     */

    public interface DataListener {
        void onProgramTextChanged(String programText);
        void onPlayStatusChanged(int playStatus);
        void onSignalQualityChanged(int signalStrength);
        void onProgramDataRateChanged(int dataRate);
        void onRadioVolumeChanged(int volume);
        void onStereoStateChanged(int stereoState);
        void onSignalStrengthChanged(int signalStrength);
        void onFmSearchFrequencyChanged(int frequency);
    }

    public interface ConnectionStateChangeListener {
        void onStart();
        void onFail();
        void onStop();
    }

    private Handler mainHandler;
    private final ArrayList<ConnectionStateChangeListener> connectionStateChangeListeners =
            new ArrayList<>();
    private final ArrayList<DataListener> dataListeners =
            new ArrayList<>();;

    public ListenerManager(Handler handler) {
        mainHandler = handler;
        unregisterAll();
    }

    public void unregisterAll() {
        connectionStateChangeListeners.clear();
        dataListeners.clear();
    }

    void notifyConnectionStop() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (connectionStateChangeListeners) {
                    for (ConnectionStateChangeListener listener : new ArrayList<>(connectionStateChangeListeners)) {
                        listener.onStop();
                    }
                }
            }
        });
    }

    void notifyConnectionFail() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (connectionStateChangeListeners) {
                    for (ConnectionStateChangeListener listener : new ArrayList<>(connectionStateChangeListeners)) {
                        listener.onFail();
                    }
                }
            }
        });
    }

    void notifyConnectionStart() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (connectionStateChangeListeners) {
                    for (ConnectionStateChangeListener listener : new ArrayList<>(connectionStateChangeListeners)) {
                        listener.onStart();
                    }
                }
            }
        });
    }

    public void registerConnectionStateChangedListener(ConnectionStateChangeListener listener) {
        connectionStateChangeListeners.add(listener);
    }

    public void unregisterConnectionStateChangedListener(ConnectionStateChangeListener listener) {
        connectionStateChangeListeners.remove(listener);
    }

    public void registerDataListener(DataListener dataListener) {
        dataListeners.add(dataListener);
    }

    public void unregisterDataListener(DataListener dataListener) {
        dataListeners.remove(dataListener);
    }

    void notifyProgramTextChanged(final String programText){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (dataListeners) {
                    for (DataListener dataListener : new ArrayList<>(dataListeners)) {
                        dataListener.onProgramTextChanged(programText);
                    }
                }
            }
        });
    }

    void notifySignalQualityChanged(final int signalQuality) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (dataListeners) {
                    for (DataListener dataListener : new ArrayList<>(dataListeners)) {
                        dataListener.onSignalQualityChanged(signalQuality);
                    }
                }
            }
        });
    }

    void notifySignalStrengthChanged(final int signalStrength) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (dataListeners) {
                    for (DataListener dataListener : new ArrayList<>(dataListeners)) {
                        dataListener.onSignalStrengthChanged(signalStrength);
                    }
                }
            }
        });
    }

    void notifyPlayStatusChanged(final int playStatus) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (dataListeners) {
                    for (DataListener dataListener : new ArrayList<>(dataListeners)) {
                        dataListener.onPlayStatusChanged(playStatus);
                    }
                }
            }
        });
    }

    void notifyFmSearchFrequencyChanged(final int frequency) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (dataListeners) {
                    for (DataListener dataListener : new ArrayList<>(dataListeners)) {
                        dataListener.onFmSearchFrequencyChanged(frequency);
                    }
                }
            }
        });
    }

    void notifyProgramDataRateChanged(final int dataRate) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (dataListeners) {
                    for (DataListener dataListener : new ArrayList<>(dataListeners)) {
                        dataListener.onProgramDataRateChanged(dataRate);
                    }
                }
            }
        });
    }

    void notifyVolumeChanged(final int volume) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (dataListeners) {
                    for (DataListener dataListener : new ArrayList<>(dataListeners)) {
                        dataListener.onRadioVolumeChanged(volume);
                    }
                }
            }
        });
    }

    void notifyStereoStateChanged(final int stereoState) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (dataListeners) {
                    for (DataListener dataListener : new ArrayList<>(dataListeners)) {
                        dataListener.onStereoStateChanged(stereoState);
                    }
                }
            }
        });
    }
}
