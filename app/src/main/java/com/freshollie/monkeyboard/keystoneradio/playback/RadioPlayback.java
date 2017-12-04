package com.freshollie.monkeyboard.keystoneradio.playback;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.freshollie.monkeyboard.keystoneradio.radio.RadioDevice;
import com.freshollie.monkeyboard.keystoneradio.radio.util.mot.MOTObject;
import com.freshollie.monkeyboard.keystoneradio.radio.util.mot.MOTObjectsManager;
import com.freshollie.monkeyboard.keystoneradio.radio.RadioStation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by freshollie on 27.11.17.
 */

class RadioPlayback {
    private static int ATTACH_TIMEOUT = 10000; // Radio will stop trying to connect after 10 seconds

    private RadioDevice radio;

    private int lastVolume;
    private int lastProgramDataRate;
    private int lastPlayStatus;
    private String lastProgramText;
    private int lastSignalQuality; // For DAB
    private int lastStereoState;
    private int lastFmSignalStrength; // For FM
    private int lastFmFrequency; // For FM
    private String lastFmProgramName;
    private int lastFmProgramType;

    private int lastStreamMode;

    private int currentStereoMode = -1;

    private ExecutorService requestExecutor = Executors.newSingleThreadExecutor();

    private MOTObjectsManager motObjectsManager = new MOTObjectsManager();

    public void setVolume(int volume) {

    }

    public void connect() {
        Log.v(TAG, "Connecting");
        connection.setConnectionStateListener(new RadioDeviceListenerManager.ConnectionStateChangeListener() {
            @Override
            public void onStart() {
                Log.v(TAG, "Connection opened");
                startPollLoop();
                listenerManager.notifyConnectionStart();
            }

            @Override
            public void onFail() {
                listenerManager.notifyConnectionFail();
            }

            @Override
            public void onStop() {
                listenerManager.notifyConnectionStop();
                disconnect();
            }
        });

        if (!connection.isConnectionOpen()) {
            connection.start();
        }
    }

    public void disconnect() {
        if (isConnected()) {
            stopPollLoop();
            connection.stop();
            Log.v(TAG, "Disconnected");
        }
    }

    public void startPollLoop() {
        Log.v(TAG, "startPollLoop()");

        if (pollThread != null) {
            pollThread.interrupt();
        }

        Log.v(TAG, "Starting poll loop");
        pollThread = new Thread(pollLoop);
        pollThread.start();
    }

    public void stopPollLoop() {
        Log.v(TAG, "Stopping poll loop");
        pollThread.interrupt();
    }

    private class CopyStationsTask extends AsyncTask<RadioDevice.CopyProgramsListener, Integer, RadioStation[]> {
        private RadioDevice.CopyProgramsListener copyProgramsListener;

        @Override
        protected RadioStation[] doInBackground(RadioDevice.CopyProgramsListener... copyListener) {
            boolean pollLoopWasRunning = pollThread.isAlive();
            if (pollLoopWasRunning) {
                stopPollLoop();
            }

            copyProgramsListener = copyListener[0];

            int totalPrograms = getTotalPrograms();
            if (totalPrograms < 0) {
                totalPrograms = 0;
            }

            RadioStation[] stationList = new RadioStation[totalPrograms];

            for (int channelId = 0; channelId < totalPrograms; channelId++) {
                RadioStation radioStation = getRadioStation(channelId);
                publishProgress(channelId, totalPrograms - 1);
                stationList[channelId] = radioStation;
            }

            if (pollLoopWasRunning) {
                startPollLoop();
            }
            return stationList;
        }

        protected void onProgressUpdate(Integer... progress) {
            copyProgramsListener.onProgressUpdate(progress[0], progress[1]);
        }

        protected void onPostExecute(RadioStation[] stationList) {
            copyProgramsListener.onComplete(stationList);
        }
    }

    /**
     * Task resets the boards Database and performs a channel search giving updates of the
     * search process.
     */
    private class DABSearchTask extends AsyncTask<RadioDevice.DABSearchListener, Integer, Integer> {
        private RadioDevice.DABSearchListener dabSearchListener;

        @Override
        protected Integer doInBackground(RadioDevice.DABSearchListener... searchListeners) {
            boolean pollLoopWasRunning = pollThread.isAlive();
            if (pollLoopWasRunning) {
                stopPollLoop();
            }
            dabSearchListener = searchListeners[0];
            dabSearchListener.onStarted();

            if (getTotalPrograms() > 0) {
                reset(RadioDevice.Values.RESET_TYPE_CLEAR);
            }

            waitForReady();


            int lastProgress = -1;
            int lastNumChannels = -1;
            int progress;
            int numChannels = 0;
            if (autoSearch(0, RadioDevice.Values.MAX_CHANNEL_BAND)) {
                while (getFrequency(0) != -1 && getPlayStatus() == RadioDevice.Values.PLAY_STATUS_SEARCHING) {
                    progress = getFrequency(0); // Gets the current frequency of the search
                    numChannels = getSearchProgram();
                    if (lastNumChannels != numChannels || lastProgress != progress) {
                        publishProgress(numChannels, progress);
                    }
                }
            }

            if (pollLoopWasRunning) {
                startPollLoop();
            }
            return numChannels;
        }

        protected void onProgressUpdate(Integer... progress) {
            dabSearchListener.onProgressUpdate(progress[0], progress[1]);
        }

        protected void onPostExecute(Integer numChannels) {
            dabSearchListener.onComplete(numChannels);
        }

    }

    /**
     * Runs all of the commands needed to poll information from a DAB/FM radio.
     */
    private boolean poll() {
        try {
            int currentStreamMode = getPlayMode();

            // If we change mode we might as well reset the previous data so we send new data
            if (currentStreamMode != lastStreamMode) {
                lastPlayStatus = -1;
                lastStereoState = -1;
                lastProgramDataRate = -1;
                lastFmProgramName = "";
                lastFmProgramType = -1;
                lastFmSignalStrength = -1;
                lastFmFrequency = -1;
                lastProgramDataRate = -1;
                lastProgramText = "";
                lastSignalQuality = -1;
            }

            lastStreamMode = currentStreamMode;

            int newVolume = getVolume();
            if (newVolume != lastVolume && newVolume != -1) {
                listenerManager.notifyVolumeChanged(newVolume);
                lastVolume = newVolume;
            }

            // Only poll this every 20 polls
            if (pollNumber % 20 == 0) {
                if (getPlayMode() == RadioDevice.Values.STREAM_MODE_DAB) {
                    int newProgramDataRate = getProgramDataRate();
                    if (newProgramDataRate != lastProgramDataRate && newProgramDataRate != -1) {
                        listenerManager.notifyDabProgramDataRateChanged(newProgramDataRate);
                        lastProgramDataRate = newProgramDataRate;
                    }
                }

                int newFmProgramType = getProgramType(RadioDevice.Values.MIN_FM_FREQUENCY);
                if (newFmProgramType != lastFmProgramType && newFmProgramType != -1) {
                    listenerManager.notifyFmProgramTypeUpdated(newFmProgramType);
                    lastFmProgramType = newFmProgramType;

                }

                int newStereoState = getStereo();
                if (newStereoState != lastStereoState) {
                    lastStereoState = newStereoState;
                    if (newStereoState != -1) {
                        listenerManager.notifyStereoStateChanged(newStereoState);
                    }
                }
            }

            if (currentStreamMode == RadioDevice.Values.STREAM_MODE_DAB) {
                int newSignalQuality = getSignalQuality();
                if (newSignalQuality != lastSignalQuality && newSignalQuality != -1) {
                    listenerManager.notifyDabSignalQualityChanged(newSignalQuality);
                    lastSignalQuality = newSignalQuality;
                }

                int channelId = getPlayIndex();

                byte[] motData = getMOTData();
                if (motData.length > 0) {
                    motObjectsManager.onNewData(channelId, motData);
                    MOTObject channelObject = motObjectsManager.getChannelObject(channelId);

                    if (channelObject != null && channelObject.isComplete()) {
                        if (MOTObjectsManager.DEBUG) Log.i(TAG, "MOT object completed");
                        if (channelObject.getApplicationType() == MOTObject.APPLICATION_TYPE_SLIDESHOW) {
                            byte[] body = motObjectsManager.getChannelObject(channelId).getBodyData();
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(body, 0, body.length, options);
                            if (options.outWidth < 1000 && options.outHeight < 1000 && bitmap != null) {
                                if (MOTObjectsManager.DEBUG) Log.i(TAG, "Notifying new slideshow image");
                                listenerManager.notifyNewSlideshowImage(
                                        bitmap
                                );
                            } else {
                                if (MOTObjectsManager.DEBUG) Log.e(TAG, "Made bad image");
                            }
                        }
                        motObjectsManager.removeChannelObject(channelId);
                    }
                }

            } else {
                // We only need to search this stuff for FM

                int newSignalStrength = getSignalStrength();
                if (newSignalStrength != lastFmSignalStrength && newSignalStrength != -1) {
                    listenerManager.notifyFmSignalStrengthChanged(newSignalStrength);
                    lastFmSignalStrength = newSignalStrength;
                }

                String newFmProgramName = getProgramName(RadioDevice.Values.MIN_FM_FREQUENCY, false);
                if (newFmProgramName != null &&
                        !newFmProgramName.equals(lastFmProgramName) &&
                        !newFmProgramName.isEmpty()) {
                    listenerManager.notifyFmProgramNameUpdated(newFmProgramName);
                    lastFmProgramName = newFmProgramName;
                }


                // We are currently searching in FM mode so show the frequency
                if (lastPlayStatus == RadioDevice.Values.PLAY_STATUS_SEARCHING) {
                    int newSearchFrequency = getPlayIndex();
                    if (newSearchFrequency != lastFmFrequency && newSearchFrequency != -1) {
                        listenerManager.notifyFmSearchFrequencyChanged(newSearchFrequency);
                        lastFmFrequency = newSearchFrequency;
                    }
                }
            }


            String newProgramText = getProgramText();
            if (newProgramText != null) {
                if (!newProgramText.isEmpty()) {
                    listenerManager.notifyProgramTextChanged(newProgramText);
                }
            }

            int newPlayStatus = getPlayStatus();

            if (newPlayStatus != lastPlayStatus && newPlayStatus != -1) {
                listenerManager.notifyPlayStatusChanged(newPlayStatus);
                lastPlayStatus = newPlayStatus;
            }
        } catch (Exception e) {
            if (connection.isDeviceAttached() && connection.isConnectionOpen()) {
                e.printStackTrace();
            }
            return false;
        }
        pollNumber += 1;
        pollNumber %= 255;

        return true;
    }

    public void copyDabStationList(final RadioDevice.CopyProgramsListener copyListener) {
        Log.v(TAG, "copyDabStationList");
        new CopyStationsTask().execute(copyListener);
    }

    public boolean startDABSearch(RadioDevice.DABSearchListener searchListener) {
        Log.v(TAG, "startDABSearch()");
        if (getPlayStatus() != RadioDevice.Values.PLAY_STATUS_SEARCHING) {
            dabSearchTask = new DABSearchTask();
            dabSearchTask.execute(searchListener);
            return true;
        }
        return false;
    }

    public boolean stopDabSearch() {
        Log.v(TAG, "stopDabSearch()");
        if (getPlayStatus() == RadioDevice.Values.PLAY_STATUS_SEARCHING ||
                (dabSearchTask != null && !dabSearchTask.isCancelled())) {
            dabSearchTask.cancel(true);
            dabSearchTask = null;
            stopSearch();
            return true;
        }
        return false;
    }
}

