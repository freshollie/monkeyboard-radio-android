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
    private static final String TAG = MOTObject.class.getSimpleName();

    public static final int APPLICATION_TYPE_SLIDESHOW = 0;

    private final int id;
    private final int applicationType;

    private SparseArray<MSCDataGroup> mscDataGroupObjectsPool = new SparseArray<>();

    private SegmentedObject body = new SegmentedObject();
    private MOTObjectHeader header = new MOTObjectHeader();

    public MOTObject(int id, int applicationType) {
        this.id = id;
        this.applicationType = applicationType;
    }

    public boolean isComplete() {
        return header.isComplete() && body.isComplete();
    }

    public MOTObjectHeader getHeader() {
        return header;
    }

    public byte[] getBodyData() {
        return body.getOrderedData();
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
                                    mscDataGroupObject.last,
                                    mscDataGroupObject.data
                            );

                    if (mscDataGroupObject.dataGroupType == MSCDataGroup.TYPE_HEADER) {
                        if (MOTObjectsManager.DEBUG) Log.i(TAG, "Header segment " + String.valueOf(extractedSegment.id) + " completed");
                        header.addSegment(extractedSegment);
                    } else {
                        if (MOTObjectsManager.DEBUG) Log.i(TAG, "Body segment " + String.valueOf(extractedSegment.id) + " completed");
                        body.addSegment(extractedSegment);
                    }
                } else {
                    if (MOTObjectsManager.DEBUG) Log.e(TAG, "Wrong segment ID in header: " + String.valueOf(mscDataGroupObject.segmentNumber));
                }
            } else {
                if (MOTObjectsManager.DEBUG) Log.e("MOTOBJECT", "COMPUTED CRC IS DIFFERENT " + String.valueOf(mscDataGroupObject.crc) + " : " + String.valueOf(MOTObjectsManager.computeCRC(mscDataGroupObject.data)));
            }
        }
    }
}
