package com.iec104tester.capture;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Saves and loads packet records to/from files.
 * Supports JSON Lines format for saving and CSV for export.
 */
public class PacketStorage {

    /**
     * 用于 JSON Lines 输出的 Gson 实例。
     * 注意：不能使用 pretty printing，否则一个对象会被输出为多行，破坏 JSON Lines "每行一个 JSON" 的格式约定。
     */
    private static final Gson gson = new Gson();
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    /**
     * Save packets to a JSON Lines file.
     */
    public static void saveToJsonLines(List<PacketRecord> packets, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            for (PacketRecord record : packets) {
                JsonObject json = recordToJson(record);
                writer.write(gson.toJson(json));
                writer.newLine();
            }
        }
    }

    /**
     * Load packets from a JSON Lines file.
     */
    public static List<PacketRecord> loadFromJsonLines(File file) throws IOException {
        List<PacketRecord> packets = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                    PacketRecord record = jsonToRecord(json);
                    if (record != null) {
                        packets.add(record);
                    }
                } catch (Exception e) {
                    // Skip malformed lines
                }
            }
        }
        return packets;
    }

    /**
     * Export packets to CSV file.
     */
    public static void exportToCsv(List<PacketRecord> packets, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            // BOM for Excel
            writer.write('\ufeff');

            writer.write("时间,方向,帧类型,ASDU类型,类型ID,传送原因,公共地址,源发地址,信息对象,摘要,原始HEX");
            writer.newLine();

            for (PacketRecord r : packets) {
                writer.write(escapeCsv(r.getTimeStr()));
                writer.write(",");
                writer.write(escapeCsv(r.getDirectionStr()));
                writer.write(",");
                writer.write(escapeCsv(r.getFrameType()));
                writer.write(",");
                writer.write(escapeCsv(r.getAsduType() != null ? r.getAsduType().toString() : ""));
                writer.write(",");
                writer.write(String.valueOf(r.getTypeId()));
                writer.write(",");
                writer.write(escapeCsv(r.getCauseOfTransmission()));
                writer.write(",");
                writer.write(String.valueOf(r.getCommonAddress()));
                writer.write(",");
                writer.write(String.valueOf(r.getOriginatorAddress()));
                writer.write(",");
                writer.write(escapeCsv(r.getInfoObjectSummary()));
                writer.write(",");
                writer.write(escapeCsv(r.getRawHex()));
                writer.newLine();
            }
        }
    }

    /**
     * Generate a default capture file name.
     */
    public static String generateFileName() {
        return "iec104_capture_" + FILE_DATE_FORMAT.format(new Date()) + ".jsonl";
    }

    private static JsonObject recordToJson(PacketRecord r) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", r.getTimestamp());
        json.addProperty("timeStr", r.getTimeStr());
        json.addProperty("isReceived", r.isReceived());
        json.addProperty("frameType", r.getFrameType());
        json.addProperty("sendSeqNum", r.getSendSeqNum());
        json.addProperty("receiveSeqNum", r.getReceiveSeqNum());
        json.addProperty("asduType", r.getAsduType() != null ? r.getAsduType().toString() : null);
        json.addProperty("typeId", r.getTypeId());
        json.addProperty("causeOfTransmission", r.getCauseOfTransmission());
        json.addProperty("commonAddress", r.getCommonAddress());
        json.addProperty("originatorAddress", r.getOriginatorAddress());
        json.addProperty("infoObjectSummary", r.getInfoObjectSummary());
        json.addProperty("rawHex", r.getRawHex());
        json.addProperty("decodedText", r.getDecodedText());
        return json;
    }

    private static PacketRecord jsonToRecord(JsonObject json) {
        try {
            long timestamp = json.get("timestamp").getAsLong();
            boolean isReceived = json.get("isReceived").getAsBoolean();
            String frameType = json.get("frameType").getAsString();
            int sendSeqNum = json.has("sendSeqNum") ? json.get("sendSeqNum").getAsInt() : 0;
            int receiveSeqNum = json.has("receiveSeqNum") ? json.get("receiveSeqNum").getAsInt() : 0;
            int typeId = json.has("typeId") ? json.get("typeId").getAsInt() : 0;
            String cot = json.has("causeOfTransmission") ? json.get("causeOfTransmission").getAsString() : "";
            int commonAddress = json.has("commonAddress") ? json.get("commonAddress").getAsInt() : 0;
            int originatorAddress = json.has("originatorAddress") ? json.get("originatorAddress").getAsInt() : 0;
            String infoSummary = json.has("infoObjectSummary") ? json.get("infoObjectSummary").getAsString() : "";
            String rawHex = json.has("rawHex") ? json.get("rawHex").getAsString() : "";
            String decodedText = json.has("decodedText") ? json.get("decodedText").getAsString() : "";

            byte[] rawBytes = hexToBytes(rawHex);

            return new PacketRecord(timestamp, isReceived, frameType, sendSeqNum, receiveSeqNum,
                    null, typeId, cot, commonAddress, originatorAddress, infoSummary,
                    rawBytes, decodedText);
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) return new byte[0];
        String[] parts = hex.split(" ");
        byte[] bytes = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) {
            bytes[i] = (byte) Integer.parseInt(parts[i], 16);
        }
        return bytes;
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
