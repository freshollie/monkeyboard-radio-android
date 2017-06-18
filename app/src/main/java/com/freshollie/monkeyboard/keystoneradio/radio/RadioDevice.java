/*
 * Created by Oliver Bell on 12/01/2017
 * Copyright (c) 2017. by Oliver bell <freshollie@gmail.com>
 *
 * Last modified 13/06/17 16:10
 */

package com.freshollie.monkeyboard.keystoneradio.radio;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.freshollie.monkeyboard.keystoneradio.R;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Used as an API to interact with the monkeyboard
 */

public class RadioDevice {
    public static final int PRODUCT_ID = 10;
    public static final int VENDOR_ID = 1240;
    public static final int BAUD_RATE = 57600;

    private boolean DEBUG_OUT_COMMANDS = false;

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

    private int pollNumber = 0;

    private int COMMAND_ATTEMPTS_TIMEOUT = 300;

    static class ByteValues {
        static byte RESPONSE_TYPE_ACK = 0x00;
        static byte CMD_ACK = 0x01;
        static byte CMD_NAK = 0x02;

        static byte START_BYTE = (byte) 0xFE;
        static byte END_BYTE = (byte) 0xFD;

        static byte EMPTY_SERIAL_NUMBER = 0x00;

        static byte CLASS_SYSTEM = 0x00;
        static byte SYSTEM_GetSysRdy = 0x00;
        static byte SYSTEM_Reset = 0x01;

        static byte CLASS_STREAM = 0x01;
        static byte STREAM_Play = 0x00;
        static byte STREAM_Stop = 0x01;
        static byte STREAM_SEARCH = 0x02;
        static byte STREAM_AutoSearch = 0x03;
        static byte STREAM_StopSearch = 0x04;
        static byte STREAM_GetPlayStatus = 0x05;
        static byte STREAM_GetPlayMode = 0x06;
        static byte STREAM_GetPlayIndex = 0x07;
        static byte STREAM_GetSignalStrength = 0x08;
        static byte STREAM_SetStereoMode = 0x09;
        static byte STREAM_GetStereo = 0x0B;
        static byte STREAM_SetVolume = 0x0C;
        static byte STREAM_GetVolume = 0x0D;
        static byte STREAM_GetProgramType = 0X0E;
        static byte STREAM_GetProgramName = 0x0F;
        static byte STREAM_GetProgramText = 0x10;
        static byte STREAM_GetDataRate = 0x12;
        static byte STREAM_GetSignalQuality = 0x13;
        static byte STREAM_GetFrequency = 0x14; // Get current search frequency
        static byte STREAM_GetEnsembleName = 0x15; // Name of the DAB collection
        static byte STREAM_GetTotalProgram = 0x16;
        static byte STREAM_GetSearchProgram = 0x1B;
    }

    public static class Values {
        public static final int PLAY_STATUS_PLAYING = 0;
        public static final int PLAY_STATUS_SEARCHING = 1;
        public static final int PLAY_STATUS_TUNING = 2;
        public static final int PLAY_STATUS_STREAM_STOP = 3;

        public static final int RESET_TYPE_REBOOT = 0;
        public static final int RESET_TYPE_CLEAR_REBOOT = 1;
        public static final int RESET_TYPE_CLEAR = 2;

        public static final int MAX_CHANNEL_BAND = 70; // Includes china

        public static final byte STREAM_MODE_DAB = 0x00;
        public static final byte STREAM_MODE_FM = 0x01;

        public static final int MAX_FM_FREQUENCY = 108000;
        public static final int MIN_FM_FREQUENCY = 87500;

        public static final int SEARCH_BACKWARDS = 0;
        public static final int SEARCH_FORWARDS = 1;
    }


    public static class StringValues {
        static String[] genres = new String[]{};
        static String[] stereoModes = new String[]{};
        static String[] playStatusValues = new String[]{};

        public static String getGenreFromId(int genreId) {
            if (genreId > genres.length - 1 || genreId < 0) {
                return "";
            } else {
                return genres[genreId];
            }
        }

        public static String getStereoModeFromId(int stereoModeId) {
            if (stereoModeId > stereoModes.length - 1  || stereoModeId < 0) {
                return "Unknown";
            } else {
                return stereoModes[stereoModeId];
            }
        }

        public static String getPlayStatusFromId(int playStatusId) {
            if (playStatusId > playStatusValues.length - 1  || playStatusId < 0) {
                return "N/A";
            } else {
                return playStatusValues[playStatusId];
            }
        }
    }


    public interface CopyProgramsListener {
        void onProgressUpdate(int progress, int max);
        void onComplete(RadioStation[] stationList);
    }

    public interface DABSearchListener {
        void onStarted();
        void onProgressUpdate(int numPrograms, int progress);
        void onComplete(int numPrograms);
    }

    private final String TAG = this.getClass().getSimpleName();

    private DeviceConnection connection;
    private Context context;

    private RadioDeviceListenerManager listenerManager;

    private Runnable pollLoop = new Runnable() {
        @Override
        public void run() {
            Log.v(TAG, "Poll Loop started");
            while (true) {
                if (!poll() || !connection.isRunning() || Thread.currentThread().isInterrupted()) {
                    Log.v(TAG, "Poll Loop stopped");
                    break;
                }
            }
        }
    };

    private Thread pollThread = new Thread();

    public RadioDevice(Context serviceContext) {
        context = serviceContext;
        connection = new DeviceConnection(serviceContext);
        listenerManager = new RadioDeviceListenerManager(new Handler(serviceContext.getMainLooper()));

        StringValues.genres =
                context.getResources().getStringArray(R.array.STATION_GENRES);
        StringValues.stereoModes =
                context.getResources().getStringArray(R.array.STEREO_AUDIO_NAMES);
        StringValues.playStatusValues =
                context.getResources().getStringArray(R.array.PLAYSTATUS_VALUES);

    }

    public RadioDeviceListenerManager getListenerManager() {
        return listenerManager;
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

        if (!connection.isRunning()) {
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

    private byte[] getBytesFromInt(int integer, int numBytes) {
        byte[] bytes = new byte[numBytes];
        ByteBuffer wrapped = ByteBuffer.wrap(bytes);
        wrapped.putInt(integer);
        return bytes;
    }

    private int getIntFromBytes(byte[] bytes){
        ByteBuffer wrapped = ByteBuffer.wrap(bytes);
        int value;
        if (bytes.length < 4) {
            value = wrapped.getShort();
        } else {
            value = wrapped.getInt();
        }
        return value;
    }

    private String getStringFromBytes(byte[] bytes) {
        return new String(bytes, Charset.forName("UTF-16")).trim();
    }

    /*
    Commands section.

    A byte array containing a command is formatted as such:

    byte {
        START_BYTE,
        FUNCTION_CATEGORY,
        FUNCTION,
        UNIQUE_COMMAND_NUMBER,
        0,
        NUM_PARAMETERS,
        PARAMETER,
        ...
        ...
        END_BYTE
     }
     */

    /**
     * Calls the given function and returns the values using the serial connection
     *
     * @param functionType
     * @param function
     * @param parameters
     * @return
     */
    private byte[] call(byte functionType, byte function, byte[] parameters, boolean retry) {
        byte [] buffer = new byte[DeviceConnection.MAX_PACKET_LENGTH];
        buffer[0] = ByteValues.START_BYTE;
        buffer[1] = functionType;
        buffer[2] = function;
        buffer[3] = ByteValues.EMPTY_SERIAL_NUMBER; // Overwritten when command is sent
        buffer[4] = 0x00;
        buffer[5] = (byte) parameters.length;

        // Adds all parameters to buffer
        int lastByteNum = 6;

        int i = 0;
        for (; i < parameters.length; i++) {
            buffer[lastByteNum + i] = parameters[i];
        }
        lastByteNum += i;

        buffer[lastByteNum] = ByteValues.END_BYTE;

        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < COMMAND_ATTEMPTS_TIMEOUT &&
                connection.isRunning()) {

            byte[] response;

            try {
                response = connection.sendForResponse(Arrays.copyOfRange(buffer, 0, lastByteNum + 1));
            } catch (DeviceConnection.NotConnectedException|NullPointerException e) {
                break;
            }

            if (response.length > 5) {
                if (isResponse(response)) {
                    if (isCommandNak(response)) {
                        if (DEBUG_OUT_COMMANDS) {
                            Log.v(TAG, "Command not acknowledged");
                        }
                        break;
                    } else {
                        if (DEBUG_OUT_COMMANDS) {
                            Log.v(TAG, "Response: " + Arrays.toString(response));
                        }
                        return response;
                    }
                }
            }

            if (!retry) {
                break;
            }
        }

        return null;
    }

    private byte[] call(byte functionType, byte function, byte[] parameters) {
        return call(functionType, function, parameters, true);
    }

    public boolean setVolume(int volume) {

        return call(
                ByteValues.CLASS_STREAM,
                ByteValues.STREAM_SetVolume,
                new byte[]{
                        (byte) volume
                }
        ) != null;
    }

    public int getVolume() {
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, "getVolume()");
        }
        byte[] response =
                call(
                        ByteValues.CLASS_STREAM,
                        ByteValues.STREAM_GetVolume,
                        new byte[]{}
                );

        if (response != null) {
            return response[6];
        } else {
            return -1;
        }
    }

    public boolean setChannel(int playMode, int channel) {
        return play(playMode, channel);
    }

    public boolean play(int playMode, int channel) {
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, "play(" + playMode + ", " + channel + ")");
        }

        byte[] channelBytes = getBytesFromInt(channel, 4);
        return call(
                ByteValues.CLASS_STREAM,
                ByteValues.STREAM_Play,
                new byte[]{
                        (byte) playMode,
                        channelBytes[0],
                        channelBytes[1],
                        channelBytes[2],
                        channelBytes[3]
                }
        ) != null;
    }

    public int getPlayMode() {
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, "getPlayMode()");
        }
        byte[] response =
                call(
                        ByteValues.CLASS_STREAM,
                        ByteValues.STREAM_GetPlayMode,
                        new byte[]{}
                );

        if (response != null) {
            return response[6];
        } else {
            return -1;
        }
    }

    public boolean stop() {
        return call(
                ByteValues.CLASS_STREAM,
                ByteValues.STREAM_Stop,
                new byte[]{}
        ) != null;
    }

    public boolean autoSearch(int startBand, int stopBand) {
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, "autoSearch(" + String.valueOf(startBand) + ", " + String.valueOf(stopBand) + ")");
        }
        return call(
                ByteValues.CLASS_STREAM,
                ByteValues.STREAM_AutoSearch,
                new byte[]{
                        (byte) startBand,
                        (byte) stopBand
                }
        ) != null;
    }

    public boolean search(int direction) {
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, "search(" + direction + ")");
        }
        return call(
                ByteValues.CLASS_STREAM,
                ByteValues.STREAM_SEARCH,
                new byte[]{
                        (byte) direction
                }
        ) != null;
    }

    public boolean stopSearch() {
        Log.v(TAG, "stopSearch()");
        return call(
                ByteValues.CLASS_STREAM,
                ByteValues.STREAM_StopSearch,
                new byte[]{}
        ) != null;
    }

    public int getPlayStatus() {
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, "getPlayStatus()");
        }
        byte[] response =
                call(
                        ByteValues.CLASS_STREAM,
                        ByteValues.STREAM_GetPlayStatus,
                        new byte[]{}
                );

        if (response != null) {
            return response[6];
        } else {
            return -1;
        }
    }

    public int getPlayIndex() {
        byte[] response =
                call(
                        ByteValues.CLASS_STREAM,
                        ByteValues.STREAM_GetPlayIndex,
                        new byte[]{}
                );

        if (response != null) {
            return getIntFromBytes(Arrays.copyOfRange(response, 6, 10));
        } else {
            return -1;
        }
    }

    public boolean setStereoMode(int mode) {
        return call(
                ByteValues.CLASS_STREAM,
                ByteValues.STREAM_SetStereoMode,
                new byte[]{
                        (byte) mode
                }
        ) != null;
    }

    public int getStereo() {
        byte[] response =
                call(
                        ByteValues.CLASS_STREAM,
                        ByteValues.STREAM_GetStereo,
                        new byte[]{}
                );

        if (response != null) {
            return response[6];
        } else {
            return -1;
        }
    }

    public int getProgramType(int frequency) {
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, "getProgramType(" + String.valueOf(frequency) + ")");
        }

        byte[] frequencyBytes;
        if (frequency >= Values.MIN_FM_FREQUENCY) {
            frequencyBytes = new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        } else {
            frequencyBytes = new byte[] {0, 0, 0, (byte) frequency};
        }

        byte[] response =
                call(
                        ByteValues.CLASS_STREAM,
                        ByteValues.STREAM_GetProgramType,
                        frequencyBytes
                );

        if (response != null) {
            return response[6];
        } else {
            return -1;
        }
    }

    public String getProgramName(int frequency, boolean abbreviated) {
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, "getProgramName(" + String.valueOf(frequency) + ", " +
                    ""+ String.valueOf(abbreviated) +")");
        }
        byte[] frequencyBytes;
        if (frequency >= Values.MIN_FM_FREQUENCY) {
            frequencyBytes = new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        } else {
            frequencyBytes = new byte[] {0, 0, 0, (byte) frequency};
        }

        byte[] response =
                call(
                        ByteValues.CLASS_STREAM,
                        ByteValues.STREAM_GetProgramName,
                        new byte[] {
                                frequencyBytes[0],
                                frequencyBytes[1],
                                frequencyBytes[2],
                                frequencyBytes[3],
                                (byte) ((abbreviated) ? 0: 1)
                        }
                );

        if (response != null) {
            byte[] programNameBytes = Arrays.copyOfRange(response, 6, response.length-3);

            try {
                return getStringFromBytes(programNameBytes);
            } catch (Exception e) {
                e.printStackTrace();
                if (DEBUG_OUT_COMMANDS) {
                    Log.v(TAG, "Error in encoding program name");
                }
                return null;
            }
        } else {
            return null;
        }
    }

    public String getProgramText() {
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, "getProgramText()");
        }
        byte[] response =
                call(
                        ByteValues.CLASS_STREAM,
                        ByteValues.STREAM_GetProgramText,
                        new byte[]{}
                );

        if (response != null) {
            byte[] programTextBytes = Arrays.copyOfRange(response, 6, response.length - 3);

            try {
                return getStringFromBytes(programTextBytes);
            } catch (Exception e) {
                Log.v(TAG, "Error in encoding program name");
                Log.v(TAG, Arrays.toString(response));
                return null;
            }
        } else {
            return null;
        }
    }

    public int getProgramDataRate() {
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, "getProgramDataRate()");
        }
        byte[] response =
                call(
                        ByteValues.CLASS_STREAM,
                        ByteValues.STREAM_GetDataRate,
                        new byte[]{}
                );

        if (response != null) {
            return getIntFromBytes(new byte[]{response[6], response[7]});
        } else {
            return -1;
        }
    }

    public int getSignalQuality() {
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, "getSignalQuality()");
        }
        byte[] response =
                call(
                        ByteValues.CLASS_STREAM,
                        ByteValues.STREAM_GetSignalQuality,
                        new byte[]{}
                );

        if (response != null) {
            return response[6];
        } else {
            return -1;
        }
    }

    public int getSignalStrength() {
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, "getSignalStrength()");
        }

        byte[] response =
                call(
                        ByteValues.CLASS_STREAM,
                        ByteValues.STREAM_GetSignalStrength,
                        new byte[]{}
                );
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, Arrays.toString(response));
        }

        if (response != null) {
            return response[6];
        } else {
            return -1;
        }
    }

    /**
     * Used to get the frequency of the current program, or used to get the current
     * search frequency when performing a DAB auto search.
     * @param channelId
     * @return
     */
    public int getFrequency(int channelId) {
        if (DEBUG_OUT_COMMANDS){
            Log.v(TAG, "getFrequency(" + String.valueOf(channelId) + ")");
        }
        byte[] response =
                call(
                        ByteValues.CLASS_STREAM,
                        ByteValues.STREAM_GetFrequency,
                        new byte[]{
                                (byte) channelId
                        }
                );

        if (response != null) {
            return response[6];
        } else {
            return -1;
        }
    }

    /**
     * @return number of programs found so far during a search
     */
    public int getSearchProgram() {
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, "getSearchProgram()");
        }

        byte[] response =
                call(
                        ByteValues.CLASS_STREAM,
                        ByteValues.STREAM_GetSearchProgram,
                        new byte[]{}
                );

        if (response != null) {
            return response[6];
        } else {
            return -1;
        }
    }

    public String getEnsembleName(int channelId, boolean abbreviated) {
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, "getEnsembleName(" + String.valueOf(channelId) + ", " +
                    ""+ String.valueOf(abbreviated) +")");
        }
        byte[] response =
                call(
                        ByteValues.CLASS_STREAM,
                        ByteValues.STREAM_GetEnsembleName,
                        new byte[]{
                                0,0,0,
                                (byte) channelId,
                                (byte) ((abbreviated) ? 0: 1)
                        }
                );

        if (response != null) {

            byte[] ensembleNameBytes = Arrays.copyOfRange(response, 6, response.length - 3);

            try {
                return getStringFromBytes(ensembleNameBytes);
            } catch (Exception e) {
                Log.v(TAG, "Error in encoding ensemble name");
                return null;
            }
        } else {
            return null;
        }
    }

    public int getTotalPrograms() {
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, "getTotalPrograms()");
        }


        byte[] response =
                call(
                        ByteValues.CLASS_STREAM,
                        ByteValues.STREAM_GetTotalProgram,
                        new byte[]{}
                );

        if (response != null) {
            return getIntFromBytes(Arrays.copyOfRange(response, 6, 10));
        } else {
            return -1;
        }
    }

    public boolean getSysReady() {
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, "getSysReady()");
        }

        return call(
                ByteValues.CLASS_SYSTEM,
                ByteValues.SYSTEM_GetSysRdy,
                new byte[]{},
                false
        ) != null;
    }

    public boolean reset(int type) {
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, "reset(" + String.valueOf(type) + ")");
        }
        return call(
                ByteValues.CLASS_SYSTEM,
                ByteValues.SYSTEM_Reset,
                new byte[]{
                        (byte) type
                }
        ) != null;
    }

    private RadioStation getRadioStation(int channelId) {
        String name = getProgramName(channelId, false);
        int genre = getProgramType(channelId);
        String ensemble = getEnsembleName(channelId, false);

        return new RadioStation(name, channelId, genre, ensemble);
    }

    public RadioStation[] getStationList() {
        int totalPrograms = getTotalPrograms();
        RadioStation[] stationList = new RadioStation[totalPrograms];

        for (int channelId = 0; channelId < totalPrograms; channelId++) {
            RadioStation radioStation = getRadioStation(channelId);

            Log.v(TAG, "Adding " + radioStation.getName() + " to station list");

            stationList[channelId] = radioStation;
        }
        return stationList;
    }

    private class CopyStationsTask extends AsyncTask<CopyProgramsListener, Integer, RadioStation[]> {
        private CopyProgramsListener copyProgramsListener;

        @Override
        protected RadioStation[] doInBackground(CopyProgramsListener... copyListener) {
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
    private class DABSearchTask extends AsyncTask<DABSearchListener, Integer, Integer> {
        private DABSearchListener dabSearchListener;

        @Override
        protected Integer doInBackground(DABSearchListener... searchListeners) {
            boolean pollLoopWasRunning = pollThread.isAlive();
            if (pollLoopWasRunning) {
                stopPollLoop();
            }
            dabSearchListener = searchListeners[0];
            dabSearchListener.onStarted();

            if (getTotalPrograms() > 0) {
                reset(Values.RESET_TYPE_CLEAR);
            }

            // After a reset we need to wait for the board to respond to this command;
            long startTime = SystemClock.currentThreadTimeMillis();
            while ((SystemClock.currentThreadTimeMillis() - startTime) < 5000 &&
                    connection.isRunning()) {
                if (getSysReady()) {
                    break;
                }
            }


            int lastProgess = -1;
            int lastNumChannels = -1;
            int progress;
            int numChannels = 0;
            if (autoSearch(0, Values.MAX_CHANNEL_BAND)) {
                while (getFrequency(0) != -1 && getPlayStatus() == Values.PLAY_STATUS_SEARCHING) {
                    progress = getFrequency(0); // Gets the current frequency of the search
                    numChannels = getSearchProgram();
                    if (lastNumChannels != numChannels || lastProgess != progress) {
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

    public void copyDabStationList(final CopyProgramsListener copyListener) {
        Log.v(TAG, "copyDabStationList");
        new CopyStationsTask().execute(copyListener);
    }

    public boolean startDABSearch(DABSearchListener searchListener) {
        Log.v(TAG, "startDABSearch()");
        if (getPlayStatus() != Values.PLAY_STATUS_SEARCHING) {
            new DABSearchTask().execute(searchListener);
            return true;
        }
        return false;
    }

    private boolean isResponse(byte[] response) {
        return response[0] == ByteValues.START_BYTE;
    }

    /**
     * Checks if a response byte array is acknowledging completion of a command
     *
     * @param response buffer
     * @return
     */
    private boolean isCommandAck(byte[] response) {
        return isResponse(response) &&
                response[1] == ByteValues.RESPONSE_TYPE_ACK &&
                response[2] == ByteValues.CMD_ACK &&
                response[5] == 0;
    }

    private boolean isCommandNak(byte[] response) {
        return isResponse(response) &&
                response[1] == ByteValues.RESPONSE_TYPE_ACK &&
                response[2] == ByteValues.CMD_NAK;
    }

    /**
     * Runs all of the commands needed to poll information from a DAB/FM radio.
     *
     * @return
     */
    private boolean poll() {
        try {
            int currentStreamMode = getPlayMode();

            int newVolume = getVolume();
            if (newVolume != lastVolume && newVolume != -1) {
                Log.v(TAG, "new Volume: " + newVolume);
                listenerManager.notifyVolumeChanged(newVolume);
                lastVolume = newVolume;
            }

            // Only poll this every 20 polls
            if (pollNumber % 20 == 0) {
                if (getPlayMode() == Values.STREAM_MODE_DAB) {
                    int newProgramDataRate = getProgramDataRate();
                    if (newProgramDataRate != lastProgramDataRate && newProgramDataRate != -1) {
                        Log.v(TAG, "new DataRate: " + newProgramDataRate);
                        listenerManager.notifyDabProgramDataRateChanged(newProgramDataRate);
                        lastProgramDataRate = newProgramDataRate;
                    }
                }

                int newFmProgramType = getProgramType(Values.MIN_FM_FREQUENCY);
                if (newFmProgramType != lastFmProgramType && newFmProgramType != -1) {
                    Log.v(TAG, "new Program Type: " + newFmProgramType);
                    listenerManager.notifyFmProgramTypeUpdated(newFmProgramType);
                    lastFmProgramType = newFmProgramType;

                }

                int newStereoState = getStereo();
                if (newStereoState != lastStereoState && newStereoState != -1) {
                    Log.v(TAG, "new NewStereoState: " + newStereoState);
                    listenerManager.notifyStereoStateChanged(newStereoState);
                    lastStereoState = newStereoState;
                }
            }

            if (currentStreamMode == Values.STREAM_MODE_DAB) {
                int newSignalQuality = getSignalQuality();
                if (newSignalQuality != lastSignalQuality && newSignalQuality != -1) {
                    Log.v(TAG, "new Signal Quality: " + newSignalQuality);
                    listenerManager.notifyDabSignalQualityChanged(newSignalQuality);
                    lastSignalQuality = newSignalQuality;
                }

            } else {
                // We only need to search this stuff for FM

                int newSignalStrength = getSignalStrength();
                if (newSignalStrength != lastFmSignalStrength && newSignalStrength != -1) {
                    Log.v(TAG, "new Signal strength: " + newSignalStrength);
                    listenerManager.notifyFmSignalStrengthChanged(newSignalStrength);
                    lastFmSignalStrength = newSignalStrength;
                }

                String newFmProgramName = getProgramName(Values.MIN_FM_FREQUENCY, false);
                if (newFmProgramName != null &&
                        !newFmProgramName.equals(lastFmProgramName) &&
                        !newFmProgramName.isEmpty()) {
                    Log.v(TAG, "new Program Name: " + newFmProgramName);
                    listenerManager.notifyFmProgramNameUpdated(newFmProgramName);
                    lastFmProgramName = newFmProgramName;
                }


                // We are currently searching in FM mode so show the frequency
                if (lastPlayStatus == Values.PLAY_STATUS_SEARCHING) {
                    int newSearchFrequency = getPlayIndex();
                    if (newSearchFrequency != lastFmFrequency && newSearchFrequency != -1) {
                        Log.v(TAG, "new Frequency: " + newSearchFrequency);
                        listenerManager.notifyFmSearchFrequencyChanged(newSearchFrequency);
                        lastFmFrequency = newSearchFrequency;
                    }
                }




            }


            String newProgramText = getProgramText();
            if (newProgramText != null) {
                if (!newProgramText.equals(lastProgramText) && !newProgramText.isEmpty()) {
                    Log.v(TAG, "new ProgramText: " + newProgramText);
                    listenerManager.notifyProgramTextChanged(newProgramText);
                    lastProgramText = newProgramText;
                }
            }

            int newPlayStatus = getPlayStatus();

            if (newPlayStatus != lastPlayStatus && newPlayStatus != -1) {
                Log.v(TAG, "new PlayStatus: " + newPlayStatus);
                listenerManager.notifyPlayStatusChanged(newPlayStatus);
                lastPlayStatus = newPlayStatus;
            }
        } catch (Exception e) {
            if (connection.isDeviceAttached() && connection.isRunning()) {
                e.printStackTrace();
            }
            return false;
        }
        pollNumber += 1;
        pollNumber %= 255;

        return true;
    }

    public boolean isAttached() {
        return connection.isDeviceAttached();
    }

    public boolean isConnected() {
        return connection.isRunning();
    }
}
