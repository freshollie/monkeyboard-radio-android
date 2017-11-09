package com.freshollie.monkeyboard.keystoneradio.radio;

import android.util.Log;
import android.util.SparseArray;

import java.util.Arrays;

/**
 * Created by freshollie on 06.11.17.
 */


public class MOTDataHandler {

    private static final String TAG = MOTDataHandler.class.getSimpleName();

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

    SparseArray<MOTObject> channelObjectMap = new SparseArray<>();

    public MOTObject parseSentence(int channelId, byte[] data) {
        MOTDataPacket packet = MOTDataPacket.fromSentence(data);

        MOTObject channelObject = channelObjectMap.get(channelId);

        if (channelObject == null || channelObject.getId() != packet.motObjectId) {
            Log.i(TAG, "Started downloading new object " + String.valueOf(packet.motObjectId));
            channelObject = new MOTObject(packet.motObjectId, packet.applicationType);
            channelObjectMap.put(channelId, channelObject);
        }
        channelObject.addPacket(packet);
        return null;
    }

    public void reset() {
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
