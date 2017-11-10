package com.freshollie.monkeyboard.keystoneradio.radio.mot;

import android.util.Log;
import android.util.SparseArray;

/**
 * Created by freshollie on 06.11.17.
 */

/**
 * An MOT object is made up of a header object and a body object.
 *
 * They are both made from collecting and combining ordered data "Segments", which are
 * extracted from MSC Objects created by the incoming packets from the slave
 */
public class MOTObject {
    private final int id;
    private final int applicationType;

    private SparseArray<MSCDataGroup> mscDataGroupObjectsPool = new SparseArray<>();

    private MOTObjectBody body = new MOTObjectBody();
    private MOTObjectHeader header = new MOTObjectHeader();

    public MOTObject(int id, int applicationType) {
        this.id = id;
        this.applicationType = applicationType;
    }

    public boolean isComplete() {
        return header.isComplete() && body.isComplete();
    }

    public byte[] getHeader() {
        return null;

        /*
        byte[] data = new byte[]{};
        for (int i = 0; i < headerSegments.size(); i++) {
            data = MOTObjectsManager.concatBytes(data, headerSegments.get(i).getData());
        }


        return data;
        */
    }

    public byte[] getBody() {
        return null;
        /*
        byte[] data = new byte[]{};

        for (int i = 0; i < bodySegments.size(); i++) {
            data = MOTObjectsManager.concatBytes(data, bodySegments.get(i).getData());
        }

        return data;
        */
    }

    public int getApplicationType() {
        return applicationType;
    }

    public int getId() {
        return id;
    }

    public void dumpLastPacket(int packetId) {

    }

    private MSCDataGroup getMSCObjectForPacket(Packet packet) {
        int mscDataGroupObjectId = (packet.segmentId + 1) * (packet.mscDataGroupType + 1);

        MSCDataGroup mscDataGroupObject = mscDataGroupObjectsPool.get(mscDataGroupObjectId);

        if (mscDataGroupObject == null) {
            mscDataGroupObject = new MSCDataGroup();
            mscDataGroupObjectsPool.put(mscDataGroupObjectId, mscDataGroupObject);
        }

        return mscDataGroupObject;
    }

    public void addPacket(Packet packet) {
        MSCDataGroup mscDataGroupObject = getMSCObjectForPacket(packet);

        mscDataGroupObject.addPacket(packet);

        if (mscDataGroupObject.isComplete()) {
            // Check that the object we have made is correct
            if (mscDataGroupObject.crc == MOTObjectsManager.computeCRC(mscDataGroupObject.data)) {
                if (mscDataGroupObject.segmentNumber == packet.segmentId) {
                    Segment extractedSegment =
                            new Segment(
                                    mscDataGroupObject.segmentNumber,
                                    mscDataGroupObject.lastSegment,
                                    mscDataGroupObject.data
                            );

                    if (mscDataGroupObject.dataGroupType == 3) {
                        header.addSegment(extractedSegment);
                    } else {
                        body.addSegment(extractedSegment);
                    }
                } else {
                    Log.i("MOTOBJECT", "Wrong segment ID in header: " + String.valueOf(mscDataGroupObject.segmentNumber));
                }
            } else {
                Log.i("MOTOBJECT", "COMPUTED CRC IS DIFFERENT " + String.valueOf(MOTObjectsManager.computeCRC(mscDataGroupObject.data)));
            }
        }
    }
}
