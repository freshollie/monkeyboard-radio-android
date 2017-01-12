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
        public static byte CMD_ACK = 0x01;
        public static byte START_BYTE = (byte) 0xFE;
        public static byte END_BYTE = (byte) 0xFD;

        public static byte SERIAL_NUMBER = 0x01;

        public static byte CATEGORY_STREAM = 0x01;
        public static byte STREAM_Play = 0x00;
        public static byte STREAM_Stop = 0x01;
        public static byte STREAM_AutoSearch = 0x03;
        public static byte STREAM_StopSearch = 0x04;

        public static byte STREAM_GetPlayStatus = 0x05;


        public static byte STREAM_SetVolume = 0x0C;

        public static byte STREAM_MODE_DAB = 0x00;

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
     * @param functionCategory
     * @param function
     * @param parameters
     * @return
     */
    private byte[] call(byte functionCategory, byte function, byte[] parameters) {
        byte [] buffer = new byte[64];
        buffer[0] = ByteValues.START_BYTE;
        buffer[1] = functionCategory;
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

        return connection.sendForResponse(buffer);
    }

    public boolean setVolume(int volume) {
        return isCommandAck(
                call(
                        ByteValues.CATEGORY_STREAM,
                        ByteValues.STREAM_SetVolume,
                        new byte[]{
                                (byte) volume
                        }

                )
        );
    }

    public boolean setChannel(int channelNum) {
        return play(channelNum);
    }

    public boolean play(int channelNum) {
        return isCommandAck(
                call(
                        ByteValues.CATEGORY_STREAM,
                        ByteValues.STREAM_Play,
                        new byte[]{
                                ByteValues.STREAM_MODE_DAB,
                                0x00, 0x00, 0x00, (byte) channelNum, // 0x000000NN
                        }
                )
        );
    }

    public boolean stop() {
        return isCommandAck(
                call(
                        ByteValues.CATEGORY_STREAM,
                        ByteValues.STREAM_Stop,
                        new byte[]{}
                )
        );
    }

    public boolean channelScan() {
        return isCommandAck(
                call(
                        ByteValues.CATEGORY_STREAM,
                        ByteValues.STREAM_AutoSearch,
                        new byte[]{}
                )
        );
    }

    public boolean stopScan() {
        return isCommandAck(
                call(
                        ByteValues.CATEGORY_STREAM,
                        ByteValues.STREAM_AutoSearch,
                        new byte[]{}
                )
        );
    }

    public int getPlayStatus() {
        byte[] response =
                call(
                        ByteValues.CATEGORY_STREAM,
                        ByteValues.STREAM_GetPlayStatus,
                        new byte[]{}
                );

        if (isResponse(response) && isCommandType(response, ByteValues.CATEGORY_STREAM)) {
            return response[6];
        } else {
            return -1;
        }
    }

    private boolean isCommandType(byte[] response, byte type) {
        return response[1] == type;
    }
    private boolean isResponse(byte[] response) {
        return response[0] == ByteValues.START_BYTE;
    }

    private boolean isCommandAck(byte[] response) {
        return isResponse(response) && response[2] == ByteValues.CMD_ACK;
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
