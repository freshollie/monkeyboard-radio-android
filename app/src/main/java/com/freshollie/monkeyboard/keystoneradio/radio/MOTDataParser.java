package com.freshollie.monkeyboard.keystoneradio.radio;

import android.util.Log;
import android.util.SparseArray;

import java.util.Arrays;

/**
 * Created by freshollie on 06.11.17.
 */


public class MOTDataParser {
    private static final String TAG = MOTDataParser.class.getSimpleName();

    SparseArray<MOTObject> channelObjects = new SparseArray<>();

    public void parseData(int channelId, byte[] data) {

        Log.v(TAG, Arrays.toString(data));

        boolean isHeaderData = data[8] == 3;

        int packetId = data[13];
        boolean lastPacket = false;
        if (Integer.toBinaryString(data[13]).length() > 6) {
            lastPacket = true;
            packetId += 128;
        }

        int segmentValue = RadioDevice.getIntFromBytes(new byte[] {data[9], data[10]});
        boolean lastSegment = false;

        if (Integer.toBinaryString(segmentValue).length() > 15) {
            // if there is a 16th bit then it means this is the last segment
            lastSegment = true;
            // And we want to make sure this is a positive int
            segmentValue += 32768;
        }

        int segmentId = segmentValue;

        int objectType = data[7];

        int objectId = RadioDevice.getIntFromBytes(new byte[] {data[11], data[12]});

        MOTObject channelObject = channelObjects.get(channelId, null);
        if (channelObject == null || channelObject.getId() != objectId) {
            Log.v(TAG, "Started downloading new object " + String.valueOf(objectId));
            channelObject = new MOTObject(objectId, objectType);
            channelObjects.put(channelId, channelObject);
        }

        channelObject.addData(isHeaderData, segmentId, lastSegment, packetId, lastPacket, Arrays.copyOfRange(data, 14, data.length - 1));
    }

    public void reset() {
        channelObjects.clear();
    }

    public MOTObject getChannelObject(int channelId) {
        MOTObject channelObject = channelObjects.get(channelId, null);
        if (channelObject != null && channelObject.isComplete()) {
            return channelObject;
        }
        return null;
    }
}
