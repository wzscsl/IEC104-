package com.iec104tester.capture;

import com.openmuc.j60870.APdu;
import com.openmuc.j60870.ASdu;
import com.openmuc.j60870.ASduType;
import com.openmuc.j60870.CauseOfTransmission;
import com.openmuc.j60870.ie.InformationObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Represents a single captured packet (APDU) with metadata.
 */
public class PacketRecord {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    private final long timestamp;
    private final boolean isReceived;
    private final String frameType;
    private final int sendSeqNum;
    private final int receiveSeqNum;
    private final ASduType asduType;
    private final int typeId;
    private final String causeOfTransmission;
    private final int commonAddress;
    private final int originatorAddress;
    private final String infoObjectSummary;
    private final byte[] rawBytes;
    private final String rawHex;
    private final String decodedText;
    private final String timeStr;

    public PacketRecord(long timestamp, boolean isReceived, String frameType,
                        int sendSeqNum, int receiveSeqNum, ASduType asduType,
                        int typeId, String causeOfTransmission, int commonAddress,
                        int originatorAddress, String infoObjectSummary,
                        byte[] rawBytes, String decodedText) {
        this.timestamp = timestamp;
        this.isReceived = isReceived;
        this.frameType = frameType;
        this.sendSeqNum = sendSeqNum;
        this.receiveSeqNum = receiveSeqNum;
        this.asduType = asduType;
        this.typeId = typeId;
        this.causeOfTransmission = causeOfTransmission;
        this.commonAddress = commonAddress;
        this.originatorAddress = originatorAddress;
        this.infoObjectSummary = infoObjectSummary;
        this.rawBytes = rawBytes;
        this.rawHex = bytesToHex(rawBytes);
        this.decodedText = decodedText;
        this.timeStr = DATE_FORMAT.format(new Date(timestamp));
    }

    public static PacketRecord fromFrame(APdu apdu, byte[] rawBytes, boolean isReceived, long timestamp) {
        String frameType = apdu.getApciType().toString();
        int sendSeqNum = apdu.getSendSeqNumber();
        int receiveSeqNum = apdu.getReceiveSeqNumber();

        ASduType asduType = null;
        int typeId = 0;
        String cot = "";
        int commonAddress = 0;
        int originatorAddress = 0;
        String infoSummary = "";
        String decodedText = "";

        ASdu asdu = apdu.getASdu();
        if (asdu != null) {
            asduType = asdu.getTypeIdentification();
            typeId = asduType != null ? asduType.getId() : 0;
            CauseOfTransmission cause = asdu.getCauseOfTransmission();
            cot = cause != null ? cause.toString() : "";
            commonAddress = asdu.getCommonAddress();
            originatorAddress = asdu.getOriginatorAddress();
            infoSummary = buildInfoSummary(asdu);
            decodedText = asdu.toString();
        }

        return new PacketRecord(timestamp, isReceived, frameType, sendSeqNum, receiveSeqNum,
                asduType, typeId, cot, commonAddress, originatorAddress, infoSummary,
                rawBytes, decodedText);
    }

    private static String buildInfoSummary(ASdu asdu) {
        InformationObject[] objects = asdu.getInformationObjects();
        if (objects == null || objects.length == 0) {
            return "";
        }
        if (objects.length == 1) {
            return "IOA=" + objects[0].getInformationObjectAddress();
        }
        return "IOA=" + objects[0].getInformationObjectAddress() + "~" +
               objects[objects.length - 1].getInformationObjectAddress() +
               " (" + objects.length + " objects)";
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(String.format("%02X", bytes[i] & 0xFF));
        }
        return sb.toString();
    }

    // Getters
    public long getTimestamp() { return timestamp; }
    public boolean isReceived() { return isReceived; }
    public String getFrameType() { return frameType; }
    public int getSendSeqNum() { return sendSeqNum; }
    public int getReceiveSeqNum() { return receiveSeqNum; }
    public ASduType getAsduType() { return asduType; }
    public int getTypeId() { return typeId; }
    public String getCauseOfTransmission() { return causeOfTransmission; }
    public int getCommonAddress() { return commonAddress; }
    public int getOriginatorAddress() { return originatorAddress; }
    public String getInfoObjectSummary() { return infoObjectSummary; }
    public byte[] getRawBytes() { return rawBytes; }
    public String getRawHex() { return rawHex; }
    public String getDecodedText() { return decodedText; }
    public String getTimeStr() { return timeStr; }

    public String getDirectionStr() {
        return isReceived ? "收" : "发";
    }
}
