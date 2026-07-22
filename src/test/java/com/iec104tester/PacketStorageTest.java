package com.iec104tester;

import com.iec104tester.capture.PacketRecord;
import com.iec104tester.capture.PacketStorage;
import com.openmuc.j60870.ASduType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PacketStorage 单元测试。
 * 覆盖 JSON Lines 往返一致性、CSV 导出格式、文件名生成。
 */
class PacketStorageTest {

    @TempDir
    File tempDir;

    @Test
    void testJsonLinesRoundTrip() throws IOException {
        File file = new File(tempDir, "test.jsonl");

        List<PacketRecord> originals = new ArrayList<>();
        originals.add(createRecord(1000L, true, "I_FORMAT", ASduType.M_SP_NA_1,
                1, "SPONTANEOUS", 1, 0, "IOA=100",
                new byte[]{0x68, 0x04, 0x07, 0x00, 0x00, 0x00}, "decoded1"));
        originals.add(createRecord(2000L, false, "S_FORMAT", ASduType.C_IC_NA_1,
                100, "ACTIVATION", 1, 0, "IOA=0",
                new byte[]{0x68, 0x04, 0x01, 0x00, 0x02, 0x00}, "decoded2"));

        PacketStorage.saveToJsonLines(originals, file);
        List<PacketRecord> loaded = PacketStorage.loadFromJsonLines(file);

        assertEquals(2, loaded.size());

        // 第一条
        PacketRecord r1 = loaded.get(0);
        assertEquals(1000L, r1.getTimestamp());
        assertTrue(r1.isReceived());
        assertEquals("I_FORMAT", r1.getFrameType());
        assertEquals(1, r1.getTypeId());
        assertEquals("SPONTANEOUS", r1.getCauseOfTransmission());
        assertEquals(1, r1.getCommonAddress());
        assertEquals(0, r1.getOriginatorAddress());
        assertEquals("IOA=100", r1.getInfoObjectSummary());
        assertEquals("68 04 07 00 00 00", r1.getRawHex());
        assertEquals("decoded1", r1.getDecodedText());

        // 第二条
        PacketRecord r2 = loaded.get(1);
        assertEquals(2000L, r2.getTimestamp());
        assertFalse(r2.isReceived());
        assertEquals("S_FORMAT", r2.getFrameType());
        assertEquals(100, r2.getTypeId());
        assertEquals("ACTIVATION", r2.getCauseOfTransmission());
    }

    @Test
    void testLoadFromJsonLinesSkipsMalformedLines() throws IOException {
        File file = new File(tempDir, "malformed.jsonl");
        List<String> lines = new ArrayList<>();
        // 缺字段：代码用 json.has() 兜底缺字段，所以这行实际可解析
        lines.add("{\"timestamp\":1000,\"isReceived\":true,\"frameType\":\"I_FORMAT\"}");
        lines.add("not-a-json");  // 这行应被跳过
        lines.add("");  // 空行跳过
        lines.add("");  // 空行跳过
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        List<PacketRecord> loaded = PacketStorage.loadFromJsonLines(file);
        // 缺字段那行能解析（has 兜底），not-a-json 被跳过，空行跳过
        assertEquals(1, loaded.size());
        assertEquals(1000L, loaded.get(0).getTimestamp());
        assertEquals("I_FORMAT", loaded.get(0).getFrameType());
    }

    @Test
    void testExportToCsvContainsHeaderAndBom() throws IOException {
        File file = new File(tempDir, "test.csv");
        List<PacketRecord> records = new ArrayList<>();
        records.add(createRecord(1000L, true, "I_FORMAT", ASduType.M_SP_NA_1,
                1, "SPONTANEOUS", 1, 0, "IOA=100",
                new byte[]{0x01}, "decoded"));

        PacketStorage.exportToCsv(records, file);

        byte[] bytes = Files.readAllBytes(file.toPath());
        // UTF-8 BOM
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);

        String content = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        // 表头
        assertTrue(content.startsWith("时间,方向,帧类型"));
        assertTrue(content.contains("收"));
        assertTrue(content.contains("I_FORMAT"));
    }

    @Test
    void testExportToCsvEscapesSpecialChars() throws IOException {
        File file = new File(tempDir, "escape.csv");
        List<PacketRecord> records = new ArrayList<>();
        // 信息对象摘要包含逗号，需要引号包裹
        records.add(createRecord(1000L, true, "I_FORMAT", ASduType.M_SP_NA_1,
                1, "SPONTANEOUS", 1, 0, "IOA=100,with,comma",
                new byte[]{0x01}, "decoded"));

        PacketStorage.exportToCsv(records, file);

        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        // 应该有引号包裹的值
        assertTrue(content.contains("\"IOA=100,with,comma\""));
    }

    @Test
    void testGenerateFileNameFormat() {
        String name = PacketStorage.generateFileName();
        // 格式: iec104_capture_yyyyMMdd_HHmmss.jsonl
        assertTrue(name.startsWith("iec104_capture_"), "name=" + name);
        assertTrue(name.endsWith(".jsonl"), "name=" + name);
        // 长度: 前缀 15 + 时间戳 15 + 后缀 6 = 36
        assertEquals(36, name.length());
    }

    @Test
    void testSaveEmptyList() throws IOException {
        File file = new File(tempDir, "empty.jsonl");
        PacketStorage.saveToJsonLines(new ArrayList<>(), file);
        List<PacketRecord> loaded = PacketStorage.loadFromJsonLines(file);
        assertTrue(loaded.isEmpty());
    }

    /** 辅助：构造一个 PacketRecord */
    private PacketRecord createRecord(long ts, boolean isReceived, String frameType,
                                       ASduType asduType, int typeId, String cot,
                                       int commonAddr, int origAddr, String infoSummary,
                                       byte[] raw, String decodedText) {
        return new PacketRecord(ts, isReceived, frameType, 0, 0,
                asduType, typeId, cot, commonAddr, origAddr, infoSummary,
                raw, decodedText);
    }
}
