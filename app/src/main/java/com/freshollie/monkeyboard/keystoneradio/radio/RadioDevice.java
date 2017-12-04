/*
 * Created by Oliver Bell on 12/01/2017
 * Copyright (c) 2017. by Oliver bell <freshollie@gmail.com>
 *
 * Last modified 13/06/17 16:10
 */

package com.freshollie.monkeyboard.keystoneradio.radio;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.freshollie.monkeyboard.keystoneradio.R;
import com.freshollie.monkeyboard.keystoneradio.playback.RadioDeviceListenerManager;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Used as an API to interact with the monkeyboard
 */
public class RadioDevice {
    public static final int PRODUCT_ID = 10;
    public static final int VENDOR_ID = 1240;
    public static final int BAUD_RATE = 57600;

    private static final int COMMAND_ATTEMPTS_TIMEOUT = 300;

    private boolean DEBUG_OUT_COMMANDS = false;


    public static class ByteValues {
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

        static byte CLASS_MOT = 0x03;
        static byte MOT_GetMOTData = 0x00;
    }

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

    public static final byte WITH_APPLICATION_TYPE = 0x01;

    private final String TAG = this.getClass().getSimpleName();

    private DeviceConnection connection;

    private RadioDeviceListenerManager listenerManager;



    public RadioDevice(DeviceConnection deviceConnection) {
        connection = deviceConnection;
    }

    public static byte[] getBytesFromInt(int integer, int numBytes) {
        byte[] bytes = new byte[numBytes];
        ByteBuffer wrapped = ByteBuffer.wrap(bytes);
        wrapped.putInt(integer);
        return bytes;
    }

    public static int getIntFromBytes(byte[] bytes){
        ByteBuffer wrapped = ByteBuffer.wrap(bytes);
        int value;
        if (bytes.length < 4) {
            value = wrapped.getShort();
        } else {
            value = wrapped.getInt();
        }
        return value;
    }

    public static String getStringFromBytes(byte[] bytes) {
        try {
            return new String(bytes, "UTF-16BE").trim();
        } catch (UnsupportedEncodingException e) {
            return "";
        }
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
                connection.isConnectionOpen()) {

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
        if (frequency >= MIN_FM_FREQUENCY) {
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
        if (frequency >= MIN_FM_FREQUENCY) {
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
            byte[] programNameBytes = Arrays.copyOfRange(response, 6, response.length - 1);

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
            byte[] programTextBytes = Arrays.copyOfRange(response, 6, response.length - 1);

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

            byte[] ensembleNameBytes = Arrays.copyOfRange(response, 6, response.length - 1);

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

        byte[] response = null;

        while (response == null && isConnected()) {
            response = call(
                    ByteValues.CLASS_STREAM,
                    ByteValues.STREAM_GetTotalProgram,
                    new byte[]{}
            );
        }

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

    public byte[] getMOTData() {
        if (DEBUG_OUT_COMMANDS) {
            Log.v(TAG, "getMotData()");
        }

        byte[] response = call(
                ByteValues.CLASS_MOT,
                ByteValues.MOT_GetMOTData,
                new byte[]{
                        WITH_APPLICATION_TYPE
                }
        );

        // Ensure that the response contains at least 1 piece of data
        if (response != null && response.length > 8) {
            return Arrays.copyOfRange(response, 6, response.length - 1);
        } else {
            return new byte[0];
        }
    }

    private RadioStation getRadioStation(int channelId) {
        String name = getProgramName(channelId, false);
        int genre = getProgramType(channelId);
        String ensemble = getEnsembleName(channelId, false);

        return new RadioStation(name, channelId, genre, ensemble);
    }

    public void waitForReady() {
        // After a reset we need to wait for the board to respond to this command;
        long startTime = SystemClock.currentThreadTimeMillis();
        while ((SystemClock.currentThreadTimeMillis() - startTime) < 5000 &&
                connection.isConnectionOpen()) {
            if (getSysReady()) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
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

    public boolean isAttached() {
        return connection.isDeviceAttached();
    }

    public boolean isConnected() {
        return connection.isConnectionOpen();
    }
}
