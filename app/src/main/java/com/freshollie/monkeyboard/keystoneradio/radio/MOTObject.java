package com.freshollie.monkeyboard.keystoneradio.radio;

import android.util.Log;
import android.util.SparseArray;

import java.util.BitSet;

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
    /**
     * An MSC data group is created from packets received from the board.
     *
     * Once complete relevant information can be extracted from the packet
     */
    private class MSCDataGroup {
        // As we don't know how many packets we will get, we use a sparse array
        // to order them as they come in
        SparseArray<MOTDataPacket> packets = new SparseArray<>();

        // When we receive a notice that this is the final packet
        // we can store it's ID
        int finalPacketId = -2;

        // When we receive a packet we check if we have all of them
        // and set this flag if so
        boolean complete = false;

        // When the first packet is received this data is associated with the
        // packet and is used when the data is complete
        int dataType;
        int segmentId;
        boolean isFinal;

        public MSCDataGroup(int dataType, int segmentId, boolean isFinal) {
            this.dataType = dataType;
            this.segmentId = segmentId;
            this.isFinal = isFinal;
        }

        /**
         * Add this packet to the group
         *
         * returns true if the group is now complete and ready to extra data
         */
        public boolean addPacket(MOTDataPacket packet) {
            if (packet.isFinalPacket) {
                finalPacketId = packet.id;
            }

            packets.put(packet.id, packet);

            complete = packets.size() == finalPacketId + 1;

            return complete;
        }

        public boolean isComplete() {
            return complete;
        }

        public void dumpPacket(int packetId) {
            if (packets.get(packetId) != null) {
                packets.delete(packetId);
            }
            if (packetId == finalPacketId) {
                finalPacketId = -2;
            }
            complete = false;
        }

        /**
         * If the group is complete, this will return the segment from the data group
         * into order and perform checks on the data to make sure it is valid.
         *
         * If not we delete all data and return false;
         */
        private byte[] getSegment() {
            byte[] data = new byte[]{};

            for (int i = 0; i < packets.size(); i++) {
                data = MOTDataHandler.concatBytes(data, packets.get(i).data);
            }

            /*
            MSC_data_group_header() {
                extension_flag 1 bslbf
                CRC_flag 1 bslbf
                segment_flag 1 bslbf
                user_access_flag 1 bslbf
                data_group_type 4 uimsbf
                continuity_index 4 uimsbf
                repetition_index 4 uimsbf
                if (extension_flag == 1) {
                    extension_field 16 bslbf
                }
            }
            */
            int byteNum = 0;
            byte currentByte = data[byteNum];

            boolean extensionFlag = MOTDataHandler.isBitSet(currentByte, 0);
            boolean CRCFlag = MOTDataHandler.isBitSet(currentByte, 1);
            boolean segmentFlag = MOTDataHandler.isBitSet(currentByte, 2);
            boolean userAccessFlag = MOTDataHandler.isBitSet(currentByte, 3);
            int dataGroupType = MOTDataHandler.intFromBitsRange(currentByte, 4, 4);

            currentByte = data[byteNum++];

            int continuityIndex = MOTDataHandler.intFromBitsRange(currentByte, 0, 4);
            int repetitionIndex = MOTDataHandler.intFromBitsRange(currentByte, 4, 4);

            currentByte = data[byteNum++];

            String extensionField;
            if (extensionFlag) {
                extensionField = Integer.toBinaryString(
                        RadioDevice.getIntFromBytes(new byte[]{data[byteNum++], data[byteNum++]})
                );
            }

            /*
            session_header() {
                if (segment_flag == 1) {
                    last 1 bslbf
                    segment_number 15 uimsbf
                }
                if (user_access_flag == 1) {
                    user_access_field () {
                        rfa 3 bslbf
                        tranport_id_flag 1 bslbf
                        length_indicator 4 uimsbf
                        if (transport_id_flag == 1) {
                            transport_id 16 uimsbf
                        }
                        end_user_address_field() {
                            for (n=0;n<length_indicator-2;n++) {
                                end_user_address_byte 8 uimsbf
                            }
                        }
                    }
                }
            }
            */

            boolean lastSegment;
            int segmentNumber;
            if (segmentFlag) {
                currentByte = data[byteNum++];

                lastSegment = MOTDataHandler.isBitSet(currentByte, 0);

                segmentNumber = RadioDevice.getIntFromBytes(
                        new byte[]{currentByte, data[byteNum++]}
                );

                if (lastSegment) {
                    segmentNumber =+ 0xFFFF;
                }

                Log.i("SEGMENT NUMBER", String.valueOf(segmentNumber));
            }

            Log.i("CRC FLAG", String.valueOf(CRCFlag));

            currentByte = data[byteNum++];

            String rfa;
            boolean transportIdFlag;
            int lengthIndicator;
            int transportId;
            byte[] endUserAddessBytes;

            if (userAccessFlag) {
                currentByte = data[byteNum++];

                rfa = Integer.toBinaryString(
                        MOTDataHandler.intFromBitsRange(currentByte, 0, 3)
                );

                transportIdFlag = MOTDataHandler.isBitSet(currentByte, 4);
                lengthIndicator = MOTDataHandler.intFromBitsRange(currentByte, 0,4);


            }

            /*
            MSC_data_group_data_field() {
                for (i=0;i<data_group_length;i++) {
                    data_group_data_byte 8 bits uimsbf
                }
            }
            if (CRC_flag==1) {
                MSC_data_group_CRC
            }
            */

            return null;
        }

        public boolean isFinal() {
            return isFinal;
        }
    }

    private final int id;
    private final int type;

    int finalBodySegmentId = -1;
    int finalHeaderSegmentId = -1;

    // Used to hold MSC objects as the packets come in until the MOT Object is ready to be built
    SparseArray<MSCDataGroup> dataGroupsPool = new SparseArray<>();

    SparseArray<MSCDataGroup> headerSegments = new SparseArray<>();
    SparseArray<MSCDataGroup> bodySegments = new SparseArray<>();

    MSCDataGroup lastMSCDataGroup;

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
            MSCDataGroup currentMSCDataGroup = headerSegments.get(i,null);
            if (currentMSCDataGroup == null || !currentMSCDataGroup.isComplete()) {
                Log.v("MOTOBJECT", "Header segment " + String.valueOf(i) + " is not complete");
                return false;
            }
        }

        for (int i = 0; i < finalBodySegmentId + 1; i++) {
            MSCDataGroup currentMSCDataGroup = bodySegments.get(i,null);
            if (currentMSCDataGroup == null || !currentMSCDataGroup.isComplete()) {
                Log.v("MOTOBJECT", "Body segment " + String.valueOf(i) + " is not complete");
                return false;
            }
        }

        return true;
    }

    public byte[] getHeader() {
        return null;

        /*
        byte[] data = new byte[]{};
        for (int i = 0; i < headerSegments.size(); i++) {
            data = MOTDataHandler.concatBytes(data, headerSegments.get(i).getData());
        }


        return data;
        */
    }

    public byte[] getBody() {
        return null;
        /*
        byte[] data = new byte[]{};

        for (int i = 0; i < bodySegments.size(); i++) {
            data = MOTDataHandler.concatBytes(data, bodySegments.get(i).getData());
        }

        return data;
        */
    }

    public int getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public void dumpLastPacket(int packetId) {
        if (lastMSCDataGroup != null) {
            lastMSCDataGroup.dumpPacket(packetId);
        }
    }

    public void addPacket(MOTDataPacket packet) {
        // We get this packets MSC Data Group from the pool
        // using a number generated by the data type and the segment id
        int mscObjectId = (packet.segmentId + 1)  * (packet.mscDataGroupType + 1);
        MSCDataGroup mscDataGroup = dataGroupsPool.get(mscObjectId);

        // This packet is the start of a new data group
        // so make a new object
        if (mscDataGroup == null) {
            mscDataGroup = new MSCDataGroup(
                    packet.mscDataGroupType,
                    packet.segmentId,
                    packet.isFinalSegment
            );
            dataGroupsPool.put(mscObjectId, mscDataGroup);
        }

        // add the packet to the MSCDataGroup and check for completion
        if (mscDataGroup.addPacket(packet)) {
            // This MSC data group is now complete
            if (mscDataGroup.getSegment() != null) {

            }
        };
    }
}
