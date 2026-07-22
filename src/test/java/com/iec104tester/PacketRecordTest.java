package com.iec104tester;

import com.iec104tester.capture.PacketRecord;
import com.openmuc.j60870.ASduType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PacketRecord 单元测试。
 * 覆盖构造方法、方向字符串、HEX 转换、字段存取。
 */
class PacketRecordTest {

    @Test
    void testConstructorFieldsPreserved() {
        byte[] raw = {0x68, 0x04, 0x07, 0x00, 0x00, 0x00};
        PacketRecord r = new PacketRecord(1700000000000L, true, "I_FORMAT",
                5, 3, ASduType.M_SP_NA_1, 1, "SPONTANEOUS",
                1, 0, "IOA=100",
                raw, "decoded");

        assertEquals(1700000000000L, r.getTimestamp());
        assertTrue(r.isReceived());
        assertEquals("I_FORMAT", r.getFrameType());
        assertEquals(5, r.getSendSeqNum());
        assertEquals(3, r.getReceiveSeqNum());
        assertEquals(ASduType.M_SP_NA_1, r.getAsduType());
        assertEquals(1, r.getTypeId());
        assertEquals("SPONTANEOUS", r.getCauseOfTransmission());
        assertEquals(1, r.getCommonAddress());
        assertEquals(0, r.getOriginatorAddress());
        assertEquals("IOA=100", r.getInfoObjectSummary());
        assertEquals("decoded", r.getDecodedText());
    }

    @Test
    void testDirectionStrReceived() {
        PacketRecord r = new PacketRecord(0, true, "I_FORMAT", 0, 0,
                null, 0, "", 0, 0, "", new byte[0], "");
        assertEquals("收", r.getDirectionStr());
    }

    @Test
    void testDirectionStrSent() {
        PacketRecord r = new PacketRecord(0, false, "I_FORMAT", 0, 0,
                null, 0, "", 0, 0, "", new byte[0], "");
        assertEquals("发", r.getDirectionStr());
    }

    @Test
    void testRawHexConversion() {
        byte[] raw = {0x68, (byte) 0xE4, 0x07, 0x00, 0x00, (byte) 0xAA};
        PacketRecord r = new PacketRecord(0, true, "I_FORMAT", 0, 0,
                null, 0, "", 0, 0, "", raw, "");

        // HEX 应使用大写、空格分隔
        assertEquals("68 E4 07 00 00 AA", r.getRawHex());
    }

    @Test
    void testRawHexEmptyBytes() {
        PacketRecord r = new PacketRecord(0, true, "U_FORMAT", 0, 0,
                null, 0, "", 0, 0, "", new byte[0], "");
        assertEquals("", r.getRawHex());
    }

    @Test
    void testRawHexNullBytes() {
        PacketRecord r = new PacketRecord(0, true, "U_FORMAT", 0, 0,
                null, 0, "", 0, 0, "", null, "");
        assertEquals("", r.getRawHex());
    }

    @Test
    void testTimeStrFormat() {
        // 时间戳固定，timeStr 应为 HH:mm:ss.SSS 格式
        PacketRecord r = new PacketRecord(1700000000000L, true, "I_FORMAT",
                0, 0, null, 0, "", 0, 0, "", new byte[0], "");
        // 长度 12 (HH:mm:ss.SSS)
        assertEquals(12, r.getTimeStr().length());
        assertTrue(r.getTimeStr().matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
    }

    @Test
    void testRawBytesNotCloned() {
        // 内部存储的是原始引用，外部修改应可见
        // 这是当前实现的行为，测试以记录该约定
        byte[] raw = {0x01, 0x02};
        PacketRecord r = new PacketRecord(0, true, "I_FORMAT", 0, 0,
                null, 0, "", 0, 0, "", raw, "");
        assertArrayEquals(new byte[]{0x01, 0x02}, r.getRawBytes());

        raw[0] = 0x09;
        // 外部修改后内部也变（按当前实现）
        assertEquals(0x09, r.getRawBytes()[0]);
    }
}
