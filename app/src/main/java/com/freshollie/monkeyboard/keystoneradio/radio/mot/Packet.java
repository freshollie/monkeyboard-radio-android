package com.freshollie.monkeyboard.keystoneradio.radio.mot;

import android.util.Log;

import com.freshollie.monkeyboard.keystoneradio.radio.RadioDevice;

import java.util.Arrays;

/**
 * Created by freshollie on 08.11.17.
 */

public class Packet {
    private static final String TAG = Packet.class.getSimpleName();
    public final int motObjectId;
    public final int applicationType;
    public final int mscDataGroupType;
    public final int segmentId;
    public final boolean isFinalSegment;
    public final int id;
    public final boolean isFinalPacket;
    public final byte[] data;

    public static Packet fromBytes(byte[] bytes) {
        int applicationType = bytes[1];
        int dataGroupType = bytes[2];

        int preParsedSegmentId = RadioDevice.getIntFromBytes(new byte[] {bytes[3], bytes[4]});

        int objectId = RadioDevice.getIntFromBytes(new byte[] {bytes[5], bytes[6]});
        int packetId = bytes[7];

        boolean lastSegment = false;
        if (Integer.toBinaryString(preParsedSegmentId).length() > 15) {
            // if there is a 16th bit then it means this is the last segment
            lastSegment = true;
            // And we want to make sure this is a positive int
            preParsedSegmentId += 32768;
        }

        int segmentId = preParsedSegmentId;

        boolean lastPacket = false;
        if (Integer.toBinaryString(bytes[7]).length() > 6) {
            lastPacket = true;
            packetId += 128;
        }

        byte[] data = Arrays.copyOfRange(bytes, 8, bytes.length);

        return new Packet(
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

    public Packet(
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
