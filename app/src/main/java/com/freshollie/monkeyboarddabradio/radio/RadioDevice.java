package com.freshollie.monkeyboarddabradio.radio;

import android.content.Context;

import java.util.ArrayList;

/**
 * Created by Freshollie on 12/01/2017.
 */

public class RadioDevice {
    public static final int PRODUCT_ID = 0xa;
    public static final int VENDOR_ID = 0x4D8;
    public static final int BAUD_RATE = 57600;

    private static class ByteValues {
        static byte RESPONSE_TYPE_ACK = 0x00;
        static byte CMD_ACK = 0x01;
        static byte CMD_NAK = 0x02;

        static byte START_BYTE = (byte) 0xFE;
        static byte END_BYTE = (byte) 0xFD;

        static byte SERIAL_NUMBER = 0x01;

        static byte FUNCTION_TYPE_STREAM = 0x01;
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
        static byte STREAM_GetEnsembleName = 0x15; // Name of the DAB collection
        static byte STREAM_GetTotalProgram = 0x16;


        static byte STREAM_MODE_DAB = 0x00;

        public static
    }

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

    private void connect() {
        if (!connection.isConnected()) {
            connection.openConnection();
        }
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

    }

    public void disconnect() {
        connection.closeConnection();
    }

    public void startPollLoop() {
        pollLoop.start();
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
        byte [] buffer = new byte[64];
        buffer[0] = ByteValues.START_BYTE;
        buffer[1] = functionType;
        buffer[2] = function;
        buffer[3] = ByteValues.SERIAL_NUMBER;
        buffer[4] = 0x00;
        buffer[5] = (byte) parameters.length;

        // Adds all parameters to buffer
        int i = 0;

        for (; i < parameters.length; i++) {
            buffer[5 + i] = parameters[i];
        }

        buffer[i + 1] = ByteValues.END_BYTE;

        byte[] response = connection.sendForResponse(buffer);

        if (isResponse(response) &&
                (isCommandAck(response) || isCorrectResponse(response, functionType, function))) {
            return response;
        } else {
            return null;
        }
    }

    public boolean setVolume(int volume) {
        return call(

                ByteValues.FUNCTION_TYPE_STREAM,
                ByteValues.STREAM_SetVolume,
                new byte[]{
                        (byte) volume
                }
        ) != null;
    }

    public int getVolume() {
        byte[] response =
                call(
                        ByteValues.FUNCTION_TYPE_STREAM,
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
                ByteValues.FUNCTION_TYPE_STREAM,
                ByteValues.STREAM_Play,
                new byte[]{
                        ByteValues.STREAM_MODE_DAB,
                        0x00, 0x00, 0x00, (byte) channelNum, // 0x000000NN
                }
        ) != null;
    }

    public boolean stop() {
        return call(
                ByteValues.FUNCTION_TYPE_STREAM,
                ByteValues.STREAM_Stop,
                new byte[]{}
        ) != null;
    }

    public boolean channelScan() {
        return call(
                ByteValues.FUNCTION_TYPE_STREAM,
                ByteValues.STREAM_AutoSearch,
                new byte[]{}
        ) != null;
    }

    public boolean stopScan() {
        return call(
                ByteValues.FUNCTION_TYPE_STREAM,
                ByteValues.STREAM_AutoSearch,
                new byte[]{}
        ) != null;
    }

    public int getPlayStatus() {
        byte[] response =
                call(
                        ByteValues.FUNCTION_TYPE_STREAM,
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
                        ByteValues.FUNCTION_TYPE_STREAM,
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
                ByteValues.FUNCTION_TYPE_STREAM,
                ByteValues.STREAM_SetStereoMode,
                new byte[]{
                        (byte) mode
                }
        ) != null;
    }

    public int getStereo() {
        byte[] response =
                call(
                        ByteValues.FUNCTION_TYPE_STREAM,
                        ByteValues.STREAM_GetStereo,
                        new byte[]{}
                );

        if (response != null) {
            return response[9];
        } else {
            return -1;
        }
    }

    public int getProgramType() {

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
