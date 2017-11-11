package com.freshollie.monkeyboard.keystoneradio.radio.mot;

import android.util.Log;
import android.util.SparseArray;

import java.util.Arrays;

/**
 * Created by freshollie on 06.11.17.
 */


public class MOTObjectsManager {

    private static final String TAG = MOTObjectsManager.class.getSimpleName();

    public static boolean DEBUG = false;

    public static int intFromBitsRange(byte b, int from, int count) {
        StringBuilder bitStringBuilder = new StringBuilder();
        for (int i = from; i < from + count; i++) {
            bitStringBuilder.insert(0, isBitSet(b, i) ? 1 : 0);
        }
        return Integer.valueOf(bitStringBuilder.toString(), 2);
    }

    public static Boolean isBitSet(byte b, int bit) {
        return (b & (1 << bit)) != 0;
    }

    public static byte[] concatBytes(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];

        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);

        return c;
    }

    /**
     * Get CRC checksum from the given bytes, algorithm by
     * https://introcs.cs.princeton.edu/java/61data/CRC16CCITT.java
     *
     */
    public static int computeCRC(byte[] bytes) {
        int crc = 0xFFFF;          // initial value
        int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12)


        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b   >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15    & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        crc &= 0xffff;

        // I don't know why but these are the values I get from the packets
        crc = 0xFFFF - crc;
        return crc;
    }

    SparseArray<MOTObject> channelObjectMap = new SparseArray<>();

    public void onNewData(int channelId, byte[] data) {
        Packet packet = Packet.fromBytes(data);

        if (packet.applicationType == MOTObject.APPLICATION_TYPE_SLIDESHOW) {

            MOTObject channelObject = channelObjectMap.get(channelId);

            if (channelObject == null || channelObject.getId() != packet.motObjectId) {
                if (DEBUG) {
                    Log.d(TAG, "Started downloading new object " + String.valueOf(packet.motObjectId));
                }
                channelObject = new MOTObject(packet.motObjectId, packet.applicationType);
                channelObjectMap.put(channelId, channelObject);
            }

            channelObject.addPacket(packet);
        }
    }

    public void removeChannelObject(int channelId) {
        channelObjectMap.clear();
    }

    public MOTObject getChannelObject(int channelId) {
        MOTObject channelObject = channelObjectMap.get(channelId, null);
        if (channelObject != null && channelObject.isComplete()) {
            return channelObject;
        }
        return null;
    }
}
