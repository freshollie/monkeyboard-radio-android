package com.freshollie.monkeyboard.keystoneradio.radio;

import android.util.Log;

import java.util.Arrays;

/**
 * Created by freshollie on 08.11.17.
 */

public class MOTDataPacket {
    private static final String TAG = MOTDataPacket.class.getSimpleName();
    public final int motObjectId;
    public final int applicationType;
    public final int mscDataGroupType;
    public final int segmentId;
    public final boolean isFinalSegment;
    public final int id;
    public final boolean isFinalPacket;
    public final byte[] data;

    public static MOTDataPacket fromSentence(byte[] sentence) {
        Log.i(TAG, Arrays.toString(sentence));

        int applicationType = sentence[7];
        int dataGroupType = sentence[8];

        int preParsedSegmentId = RadioDevice.getIntFromBytes(new byte[] {sentence[9], sentence[10]});
        int objectId = RadioDevice.getIntFromBytes(new byte[] {sentence[11], sentence[12]});
        int packetId = sentence[13];

        boolean lastSegment = false;
        if (Integer.toBinaryString(preParsedSegmentId).length() > 15) {
            // if there is a 16th bit then it means this is the last segment
            lastSegment = true;
            // And we want to make sure this is a positive int
            preParsedSegmentId += 32768;
        }

        int segmentId = preParsedSegmentId;

        boolean lastPacket = false;
        if (Integer.toBinaryString(sentence[13]).length() > 6) {
            lastPacket = true;
            packetId += 128;
        }

        byte[] data = Arrays.copyOfRange(sentence, 14, sentence.length -1);

        return new MOTDataPacket(
                objectId,
                applicationType,
                dataGroupType,
                segmentId,
                lastSegment,
                packetId,
                lastPacket,
                data
        );
    }

    public MOTDataPacket(
            int motObjectId,
            int applicationType,
            int mscDataGroupType,
            int segmentId,
            boolean isFinalSegment,
            int packetId,
            boolean isFinalPacket,
            byte[] data) {
        this.motObjectId = motObjectId;
        this.applicationType = applicationType;
        this.mscDataGroupType = mscDataGroupType;
        this.segmentId = segmentId;
        this.isFinalSegment = isFinalSegment;
        this.id = packetId;
        this.isFinalPacket = isFinalPacket;
        this.data = data;
    }
}
