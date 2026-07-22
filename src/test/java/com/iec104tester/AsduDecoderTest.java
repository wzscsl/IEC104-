package com.iec104tester;

import com.iec104tester.core.AsduDecoder;
import com.openmuc.j60870.ASduType;
import com.openmuc.j60870.CauseOfTransmission;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AsduDecoder 单元测试。
 * 覆盖中文类型名、中文传送原因、HEX dump 格式化、null 输入降级。
 */
class AsduDecoderTest {

    @Test
    void testGetTypeNameCnForCommonTypes() {
        assertEquals("单点信息", AsduDecoder.getTypeNameCn(ASduType.M_SP_NA_1));
        assertEquals("双点信息", AsduDecoder.getTypeNameCn(ASduType.M_DP_NA_1));
        assertEquals("测量值-标幺化值", AsduDecoder.getTypeNameCn(ASduType.M_ME_NA_1));
        assertEquals("测量值-标度化值", AsduDecoder.getTypeNameCn(ASduType.M_ME_NB_1));
        assertEquals("测量值-短浮点数", AsduDecoder.getTypeNameCn(ASduType.M_ME_NC_1));
        assertEquals("累计量", AsduDecoder.getTypeNameCn(ASduType.M_IT_NA_1));
        assertEquals("单点命令", AsduDecoder.getTypeNameCn(ASduType.C_SC_NA_1));
        assertEquals("总召唤命令", AsduDecoder.getTypeNameCn(ASduType.C_IC_NA_1));
        assertEquals("时钟同步", AsduDecoder.getTypeNameCn(ASduType.C_CS_NA_1));
    }

    @Test
    void testGetTypeNameCnForTimeTaggedTypes() {
        assertEquals("单点信息(带CP56时标)", AsduDecoder.getTypeNameCn(ASduType.M_SP_TB_1));
        assertEquals("双点信息(带CP56时标)", AsduDecoder.getTypeNameCn(ASduType.M_DP_TB_1));
    }

    @Test
    void testGetTypeNameCnNullReturnsEmpty() {
        assertEquals("", AsduDecoder.getTypeNameCn(null));
    }

    @Test
    void testGetCotNameCnForCommonCauses() {
        assertEquals("周期上送", AsduDecoder.getCotNameCn(CauseOfTransmission.PERIODIC));
        assertEquals("背景扫描", AsduDecoder.getCotNameCn(CauseOfTransmission.BACKGROUND_SCAN));
        assertEquals("突发(自发)", AsduDecoder.getCotNameCn(CauseOfTransmission.SPONTANEOUS));
        assertEquals("激活", AsduDecoder.getCotNameCn(CauseOfTransmission.ACTIVATION));
        assertEquals("激活确认", AsduDecoder.getCotNameCn(CauseOfTransmission.ACTIVATION_CON));
        assertEquals("激活终止", AsduDecoder.getCotNameCn(CauseOfTransmission.ACTIVATION_TERMINATION));
        assertEquals("被站召唤", AsduDecoder.getCotNameCn(CauseOfTransmission.INTERROGATED_BY_STATION));
    }

    @Test
    void testGetCotNameCnNullReturnsEmpty() {
        assertEquals("", AsduDecoder.getCotNameCn(null));
    }

    @Test
    void testToHexDumpBasicLayout() {
        byte[] bytes = {0x68, 0x04, 0x07, 0x00, 0x00, 0x00};
        String dump = AsduDecoder.toHexDump(bytes);

        // 偏移地址、hex 字节、ASCII 分隔符
        assertTrue(dump.startsWith("0000  "));
        assertTrue(dump.contains("68 04 07 00 00 00"));
        assertTrue(dump.contains("|"));
        // ASCII 段：可打印字符应原样输出，非可打印字符用 .
        assertTrue(dump.contains("h"));  // 0x68 = 'h'
    }

    @Test
    void testToHexDumpMultiLine() {
        // 17 字节，应分两行（16 + 1）
        byte[] bytes = new byte[17];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) i;
        String dump = AsduDecoder.toHexDump(bytes);

        String[] lines = dump.split("\n");
        assertEquals(2, lines.length);
        assertTrue(lines[0].startsWith("0000  "));
        assertTrue(lines[1].startsWith("0010  "));
    }

    @Test
    void testToHexDumpNullOrEmpty() {
        assertEquals("", AsduDecoder.toHexDump(null));
        assertEquals("", AsduDecoder.toHexDump(new byte[0]));
    }

    @Test
    void testToHexDumpNonPrintableChars() {
        byte[] bytes = {0x00, 0x01, 0x1F, 0x20, 0x7E, 0x7F, (byte) 0xFF};
        String dump = AsduDecoder.toHexDump(bytes);
        // 非可打印字符应被替换为 .
        assertTrue(dump.contains("."));
        // 0x20 (空格) ~ 0x7E 应保留为字符
        assertTrue(dump.contains("~"));
    }

    @Test
    void testDecodeAsduNullReturnsPlaceholder() {
        assertEquals("(无ASDU内容 - U/S格式帧)", AsduDecoder.decodeAsdu(null));
    }
}
