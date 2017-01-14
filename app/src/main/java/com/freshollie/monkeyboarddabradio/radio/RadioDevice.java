package com.freshollie.monkeyboarddabradio.radio;

import android.content.Context;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Freshollie on 12/01/2017.
 */

public class RadioDevice {
    public static final int PRODUCT_ID = 1002;
    public static final int VENDOR_ID = 1240;
    public static final int BAUD_RATE = 57600;

    public static class ByteValues {
        static byte RESPONSE_TYPE_ACK = 0x00;
        static byte CMD_ACK = 0x01;
        static byte CMD_NAK = 0x02;

        static byte START_BYTE = (byte) 0xFE;
        static byte END_BYTE = (byte) 0xFD;

        static byte EMPTY_SERIAL_NUMBER = 0x00;

        static byte CLASS_STREAM = 0x01;
        static byte STREAM_Play = 0x00;
        static byte STREAM_Stop = 0x01;
        static byte STREAM_AutoSearch = 0x03;
        static byte STREAM_StopSearch = 0x04;
        static byte STREAM_GetPlayStatus = 0x05;
        static byte STREAM_GetPlayIndex = 0x07;
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


        static byte STREAM_MODE_DAB = 0x00;
    }

    /*
    public static class StringValues {
        static String[] genres = new String[]{};
        static String[] stereoModes = new String[]{};
        static String[] playStatus = new String[]{};

        public static String getGenreName(int genreId) {
            if (genreId > genres.length) {
                return "";
            } else {
                return genres[genreId];
            }
        }

        public static String getStereoName(int stereoId) {
            if (stereoId > stereoModes.length) {
                return "";
            } else {
                return genres[stereoId];
            }
        }
    };
    */

    private final String TAG = this.getClass().getSimpleName();

    private DeviceConnection connection;
    private Context context;

    private ListenerManager listenerManager = new ListenerManager();

    private ArrayList<RadioStation> stationList = new ArrayList<>();

    private Thread pollLoop = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                if (!poll()) {
                    break;
                }
            }
        }
    });

    public RadioDevice(Context appContext) {
        context = appContext;
        connection = new DeviceConnection(appContext);
    }

    public ListenerManager getListenerManager() {
        return listenerManager;
    }

    public void connect() {
        connection.setConnectionStateListener(new DeviceConnection.ConnectionStateListener() {
            @Override
            public void onStart() {
                listenerManager.onConnectionStart();
            }

            @Override
            public void onStop() {
                listenerManager.onConnectionStop();
            }
        });

        if (!connection.isConnected()) {
            connection.connect();
        }
    }

    public void disconnect() {
        connection.disconnect();
    }

    public void startPollLoop() {
        pollLoop.start();
    }

    private int getIntFromBytes(byte[] bytes){
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
        for (byte value: bytes) {
            byteBuffer.put(value);
        }

        byteBuffer.flip();
        return byteBuffer.getInt();
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
        SERIAL_NUMBER,
        0,
        NUM_PARAMETERS,
        PARAMETER,
        ...
        ...
        END_BYTE
     */

    /**
     * Calls the given function and returns the values using the serial connection
     *
     * @param functionType
     * @param function
     * @param parameters
     * @return
     */
    private byte[] call(byte functionType, byte function, byte[] parameters) {
        byte [] buffer = new byte[DeviceConnection.MAX_PACKET_LENGTH];
        buffer[0] = ByteValues.START_BYTE;
        buffer[1] = functionType;
        buffer[2] = function;
        buffer[3] = ByteValues.EMPTY_SERIAL_NUMBER; // Written when command is sent
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

        byte[] response = connection.sendForResponse(Arrays.copyOfRange(buffer, 0, lastByteNum + 1));

        if (response.length > 5) {
            if (isResponse(response) &&
                    (isCommandAck(response) || isCorrectResponse(response, functionType, function))) {
                return response;
            }
        }
        return null;
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

    public boolean setChannel(int channelNum) {
        return play(channelNum);
    }

    public boolean play(int channelNum) {
        return call(
                ByteValues.CLASS_STREAM,
                ByteValues.STREAM_Play,
                new byte[]{
                        ByteValues.STREAM_MODE_DAB,
                        0x00, 0x00, 0x00, (byte) channelNum, // 0x000000NN
                }
        ) != null;
    }

    public boolean stop() {
        return call(
                ByteValues.CLASS_STREAM,
                ByteValues.STREAM_Stop,
                new byte[]{}
        ) != null;
    }

    public boolean startChannelScan() {
        return call(
                ByteValues.CLASS_STREAM,
                ByteValues.STREAM_AutoSearch,
                new byte[]{}
        ) != null;
    }

    public boolean stopChannelScan() {
        return call(
                ByteValues.CLASS_STREAM,
                ByteValues.STREAM_StopSearch,
                new byte[]{}
        ) != null;
    }

    public int getPlayStatus() {
        Log.v(TAG, "getPlayStatus()");
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
            return response[9];
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

    public int getProgramType(int channelId) {
        Log.v(TAG, "getProgramType(" + String.valueOf(channelId) + ")");
        byte[] response =
                call(
                        ByteValues.CLASS_STREAM,
                        ByteValues.STREAM_GetProgramType,
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

    public String getProgramName(int channelId, boolean abbreviated) {
        Log.v(TAG, "getProgramName(" + String.valueOf(channelId) + ", " +
                ""+ String.valueOf(abbreviated) +")");
        byte[] response =
                call(
                        ByteValues.CLASS_STREAM,
                        ByteValues.STREAM_GetProgramName,
                        new byte[]{
                                0,0,0,
                                (byte) channelId,
                                (byte) ((abbreviated) ? 0: 1)
                        }
                );

        if (response != null) {
            byte[] programNameBytes = Arrays.copyOfRange(response, 6, response.length-3);

            Log.v(TAG, Arrays.toString("talkSPORT".getBytes(Charset.forName("UTF-16"))));
            Log.v(TAG, Arrays.toString(programNameBytes));

            try {
                return getStringFromBytes(programNameBytes);
            } catch (Exception e) {
                e.printStackTrace();
                Log.v(TAG, "Error in encoding program name");
                return null;
            }
        } else {
            return null;
        }
    }

    public String getProgramText() {
        byte[] response =
                call(
                        ByteValues.CLASS_STREAM,
                        ByteValues.STREAM_GetProgramText,
                        new byte[]{}
                );

        if (response != null) {
            byte[] programTextBytes = Arrays.copyOfRange(response, 6, response.length-3);

            try {
                return getStringFromBytes(programTextBytes);
            } catch (Exception e) {
                Log.v(TAG, "Error in encoding program name");
                return null;
            }
        } else {
            return null;
        }
    }

    public int getDataRate() {
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
        Log.v(TAG, "getSignalQuality()");
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

    public int getFrequency(int channelId) {
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

    public String getEnsembleName(int channelId, boolean abbreviated) {
        Log.v(TAG, "getEnsembleName(" + String.valueOf(channelId) + ", " +
                ""+ String.valueOf(abbreviated) +")");
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
        Log.v(TAG, "getTotalPrograms()");
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

    public ArrayList<RadioStation> getStationList() {
        return stationList;
    }

    public void refreshStationList() {
        stationList.clear();
        int totalPrograms = getTotalPrograms();
        for (int channelId = 0; channelId < totalPrograms; channelId++) {
            String name = getProgramName(channelId, false);
            int genre = getProgramType(channelId);
            String ensemble = getEnsembleName(channelId, false);

            Log.v(TAG, "Adding " + name + " to station list");

            stationList.add(new RadioStation(name, channelId, genre, ensemble));
        }
    }

    private boolean isCorrectResponse(byte[] response, byte type, byte command) {
        return response[1] == type && response[2] == command;
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

    /**
     * Runs all of the commands needed to poll a dab radio.
     *
     * @return
     */
    public boolean poll() {
        try {

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
