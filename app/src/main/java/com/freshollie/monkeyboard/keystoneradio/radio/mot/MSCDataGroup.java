package com.freshollie.monkeyboard.keystoneradio.radio.mot;

import android.util.Log;
import android.util.SparseArray;

import com.freshollie.monkeyboard.keystoneradio.radio.RadioDevice;

import java.util.Arrays;

/**
 * Created by freshollie on 09.11.17.
 */

/**
 * MSC Data Group, automatically builds its self from packets.
 *
 * Written to the specification via:
 * http://www.etsi.org/deliver/etsi_ts/101700_101799/101759/01.01.01_60/ts_101759v010101p.pdf
 */
public class MSCDataGroup {
    private static final String TAG = MSCDataGroup.class.getSimpleName();
    // As we don't know how many packets we will get, we use a sparse array
    // to order them as they come in
    SparseArray<Packet> packets = new SparseArray<>();

    // When we receive the final packet, we can store it's ID
    private int finalPacketId = -2;

    // When we receive a packet, we check if we are complete and flag
    // this if true
    private boolean complete = false;

    // This data is used when the data group is built

    // MSC Data group header
    public boolean extensionFlag = false;
    public boolean crcFlag = false;
    public boolean segmentFlag = false;
    public boolean userAccessFlag = false;
    public int dataGroupType = -1;
    public int continuityIndex = -1;
    public int repetitionIndex = -1;
    public String extension = "";

    // Session header
    public boolean lastSegment = false;
    public int segmentNumber = -1;

    // Session header user access
    public String rfa = null;
    public boolean transportIdFlag = false;
    public int lengthIndicator = -1;
    public int transportId = -1;
    public byte[] endUserAddressBytes = new byte[]{};

    // Data
    public byte[] data = new byte[0];

    // CRC
    public int crc = -1;


    /**
     * Sometimes removes the given packet ID
     */
    public void dumpPacket(int packetId) {
        if (packets.get(packetId) != null) {
            packets.delete(packetId);
        }

        if (packetId == finalPacketId) {
            finalPacketId = -2;
        }

        complete = false;
    }

    private boolean hasAllPackets() {
        return packets.size() == finalPacketId + 1;
    }

    /**
     * Order the packets and populate the fields based on
     * the data in the packets
     */
    private void buildFromPackets() {
        // If we don't even have all the packets, we know we are in complete

        byte[] bytes = new byte[0];

        for (int i = 0; i < packets.size(); i++) {
            bytes = MOTObjectsManager.concatBytes(bytes, packets.get(i).data);
        }

        int byteNum = 0;


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

        byte currentByte = bytes[byteNum++];

        extensionFlag = MOTObjectsManager.isBitSet(currentByte, 8);
        crcFlag = MOTObjectsManager.isBitSet(currentByte, 6);
        segmentFlag = MOTObjectsManager.isBitSet(currentByte, 5);
        userAccessFlag = MOTObjectsManager.isBitSet(currentByte, 4);
        dataGroupType = MOTObjectsManager.intFromBitsRange(currentByte, 0, 4);


        currentByte = bytes[byteNum++];

        continuityIndex = MOTObjectsManager.intFromBitsRange(currentByte, 4, 4);
        repetitionIndex = MOTObjectsManager.intFromBitsRange(currentByte, 0, 4);


        if (extensionFlag) {
            extension = Integer.toBinaryString(
                    RadioDevice.getIntFromBytes(new byte[]{bytes[byteNum++], bytes[byteNum++]})
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

        if (segmentFlag) {
            currentByte = bytes[byteNum++];

            segmentNumber = RadioDevice.getIntFromBytes(
                    new byte[]{currentByte, bytes[byteNum++]}
            ) & ~(1 << 15) & 0xFFFF;

            lastSegment = MOTObjectsManager.isBitSet(currentByte, 7);
        }

        if (userAccessFlag) {
            currentByte = bytes[byteNum++];

            rfa = Integer.toBinaryString(
                    MOTObjectsManager.intFromBitsRange(currentByte, 5, 3)
            );

            transportIdFlag = MOTObjectsManager.isBitSet(currentByte, 4);
            lengthIndicator = MOTObjectsManager.intFromBitsRange(currentByte, 0,4);

            if (transportIdFlag) {
                transportId = RadioDevice.getIntFromBytes(new byte[] {bytes[byteNum++], bytes[byteNum++]});
            }

            for (int i = 0; i < lengthIndicator - 2; i++) {
                MOTObjectsManager.concatBytes(endUserAddressBytes, new byte[] {bytes[byteNum++]});
            }
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

        // The data should be the rest of the bytes
        data = Arrays.copyOfRange(bytes, byteNum, bytes.length);

        if (crcFlag) {
            // However if there is a CRC bytes the last 2 bytes are the CRC checksum
            crc =
                    RadioDevice.getIntFromBytes(
                            Arrays.copyOfRange(
                                    data,
                                    data.length - 2,
                                    data.length)
                    ) & 0xFFFF;
            data = Arrays.copyOfRange(bytes, 0, bytes.length - 2);
        }
    }

    public void addPacket(Packet packet) {
        if (packet.isFinalPacket) {
            finalPacketId = packet.id;
        }

        packets.put(packet.id, packet);

        if (hasAllPackets()) {
            buildFromPackets();
            complete = true;
        }
    }

    public boolean isComplete() {
        return complete;
    }
}
