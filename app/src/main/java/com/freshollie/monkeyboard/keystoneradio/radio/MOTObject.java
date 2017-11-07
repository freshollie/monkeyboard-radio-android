package com.freshollie.monkeyboard.keystoneradio.radio;

import android.util.Log;
import android.util.SparseArray;

/**
 * Created by freshollie on 06.11.17.
 */

public class MOTObject {

    private static byte[] concatBytes(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];

        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);

        return c;
    }

    private class Segment {
        SparseArray<byte[]> packets = new SparseArray<>();
        int finalPacketId = -2;

        public boolean isComplete() {
            return packets.size() == finalPacketId + 1;
        }

        public void addPacket(int packetId, boolean finalPacket, byte[] packet) {
            if (finalPacket) {
                finalPacketId = packetId;
            }
            packets.put(packetId, packet);
        }

        public byte[] getData() {
            byte[] data = new byte[]{};

            for (int i = 0; i < packets.size(); i++) {
                data = concatBytes(data, packets.get(i));
            }

            return data;
        }

        public void dumpPacket(int packetId) {
            if (packets.get(packetId, null) != null) {
                packets.delete(packetId);
            }
        }
    }

    private final int id;
    private final int type;

    int finalBodySegmentId = -1;
    int finalHeaderSegmentId = -1;

    SparseArray<Segment> headerSegments = new SparseArray<>();
    SparseArray<Segment> bodySegments = new SparseArray<>();

    Segment lastSegment;

    public MOTObject(int id, int type) {
        this.id = id;
        this.type = type;
    }

    public boolean isComplete() {
        // Every segment should have a value up to the last
        if (finalBodySegmentId == -1 || finalHeaderSegmentId == -1) {
            return false;
        }

        for (int i = 0; i < finalHeaderSegmentId + 1; i++) {
            Segment currentSegment = headerSegments.get(i,null);
            if (currentSegment == null || !currentSegment.isComplete()) {
                Log.v("MOTOBJECT", "Header segment " + String.valueOf(i) + " is not complete");
                return false;
            }
        }

        for (int i = 0; i < finalBodySegmentId + 1; i++) {
            Segment currentSegment = bodySegments.get(i,null);
            if (currentSegment == null || !currentSegment.isComplete()) {
                Log.v("MOTOBJECT", "Body segment " + String.valueOf(i) + " is not complete");
                return false;
            }
        }

        return true;
    }

    public byte[] getHeader() {
        byte[] data = new byte[]{};

        for (int i = 0; i < headerSegments.size(); i++) {
            data = concatBytes(data, headerSegments.get(i).getData());
        }

        return data;
    }

    public byte[] getBody() {
        byte[] data = new byte[]{};

        for (int i = 0; i < bodySegments.size(); i++) {
            data = concatBytes(data, bodySegments.get(i).getData());
        }

        return data;
    }

    public int getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public void dumpLastPacket(int packetId) {
        if (lastSegment != null) {
            lastSegment.dumpPacket(packetId);
        }
    }

    public void addData(boolean isHeaderData, int segmentId, boolean finalSegment, int packetId, boolean finalPacket, byte[] data) {
        SparseArray<Segment> segments = isHeaderData? headerSegments: bodySegments;

        Segment segment = segments.get(segmentId, null);

        // A new segment
        if (segment == null) {
            segment = new Segment();
            segments.put(segmentId, segment);
        }

        // add the packet to the segment
        segment.addPacket(packetId, finalPacket, data);

        if (finalSegment) {
            if (isHeaderData) {
                finalHeaderSegmentId = segmentId;
            } else {
                finalBodySegmentId = segmentId;
            }
        }

        lastSegment = segment;
    }
}
