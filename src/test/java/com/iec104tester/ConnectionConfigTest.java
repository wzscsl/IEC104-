package com.iec104tester;

import com.iec104tester.model.ConnectionConfig;
import com.iec104tester.model.ConnectionConfig.DataCategory;
import com.iec104tester.model.ServerConfig;
import com.openmuc.j60870.ASduType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ConnectionConfig 和 ServerConfig 配置类的测试。
 */
class ConnectionConfigTest {

    @Test
    void testConnectionConfigCopy() {
        ConnectionConfig original = new ConnectionConfig();
        original.setHost("192.168.1.100");
        original.setPort(2404);
        original.setOriginatorAddress(5);
        original.setCommonAddress(255);
        original.setT1(20000);
        original.setT2(15000);
        original.setT3(30000);
        original.setK(24);
        original.setW(16);
        original.setCotFieldLength(2);
        original.setCommonAddressFieldLength(2);
        original.setIoaFieldLength(3);
        original.setIoaRange(DataCategory.TELESIGNALING, 1, 100);
        original.setIoaRange(DataCategory.TELEMETRY, 16385, 50);

        ConnectionConfig copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getHost(), copy.getHost());
        assertEquals(original.getPort(), copy.getPort());
        assertEquals(original.getOriginatorAddress(), copy.getOriginatorAddress());
        assertEquals(original.getCommonAddress(), copy.getCommonAddress());
        assertEquals(original.getT1(), copy.getT1());
        assertEquals(original.getT2(), copy.getT2());
        assertEquals(original.getT3(), copy.getT3());
        assertEquals(original.getK(), copy.getK());
        assertEquals(original.getW(), copy.getW());
        assertEquals(original.getCotFieldLength(), copy.getCotFieldLength());
        assertEquals(original.getCommonAddressFieldLength(), copy.getCommonAddressFieldLength());
        assertEquals(original.getIoaFieldLength(), copy.getIoaFieldLength());

        assertEquals(original.getIoaStart(DataCategory.TELESIGNALING), copy.getIoaStart(DataCategory.TELESIGNALING));
        assertEquals(original.getIoaCount(DataCategory.TELESIGNALING), copy.getIoaCount(DataCategory.TELESIGNALING));
        assertEquals(original.getIoaStart(DataCategory.TELEMETRY), copy.getIoaStart(DataCategory.TELEMETRY));
        assertEquals(original.getIoaCount(DataCategory.TELEMETRY), copy.getIoaCount(DataCategory.TELEMETRY));

        copy.setHost("10.0.0.1");
        copy.setCommonAddress(1);
        copy.setIoaRange(DataCategory.TELESIGNALING, 500, 200);

        assertFalse("192.168.1.100".equals(copy.getHost()));
        assertEquals("192.168.1.100", original.getHost());
        assertEquals(255, original.getCommonAddress());
        assertEquals(100, original.getIoaCount(DataCategory.TELESIGNALING));
    }

    @Test
    void testIsIoaInRange() {
        ConnectionConfig config = new ConnectionConfig();
        config.setIoaRange(DataCategory.TELESIGNALING, 1, 100);
        config.setIoaRange(DataCategory.TELEMETRY, 16385, 50);
        config.setIoaRange(DataCategory.TELECOMMAND, 0, 0);

        assertTrue(config.isIoaInRange(1, DataCategory.TELESIGNALING));
        assertTrue(config.isIoaInRange(50, DataCategory.TELESIGNALING));
        assertTrue(config.isIoaInRange(100, DataCategory.TELESIGNALING));
        assertFalse(config.isIoaInRange(101, DataCategory.TELESIGNALING));
        assertFalse(config.isIoaInRange(0, DataCategory.TELESIGNALING));

        assertTrue(config.isIoaInRange(16385, DataCategory.TELEMETRY));
        assertTrue(config.isIoaInRange(16434, DataCategory.TELEMETRY));
        assertFalse(config.isIoaInRange(16435, DataCategory.TELEMETRY));
        assertFalse(config.isIoaInRange(16384, DataCategory.TELEMETRY));

        assertTrue(config.isIoaInRange(99999, DataCategory.TELECOMMAND));
        assertTrue(config.isIoaInRange(0, DataCategory.TELECOMMAND));
    }

    @Test
    void testIoaRangeDefaultsAreRestoredWhenMissing() throws Exception {
        ConnectionConfig config = new ConnectionConfig();
        Field field = ConnectionConfig.class.getDeclaredField("ioaRanges");
        field.setAccessible(true);
        field.set(config, null);

        assertEquals(0x0001, config.getIoaStart(DataCategory.TELESIGNALING));
        assertEquals(0, config.getIoaCount(DataCategory.TELESIGNALING));
        assertTrue(config.isIoaInRange(0, DataCategory.TELESIGNALING));
        assertTrue(config.isIoaInRange(99999, DataCategory.TELESIGNALING));
    }

    @Test
    void testServerConfigGetStartAddressAndGetCount() {
        ServerConfig config = new ServerConfig();

        assertEquals(0x0001, config.getStartAddress(ASduType.M_SP_NA_1));
        assertEquals(20, config.getCount(ASduType.M_SP_NA_1));

        assertEquals(0x2001, config.getStartAddress(ASduType.M_DP_NA_1));
        assertEquals(10, config.getCount(ASduType.M_DP_NA_1));

        assertEquals(0x4001, config.getStartAddress(ASduType.M_ME_NC_1));
        assertEquals(50, config.getCount(ASduType.M_ME_NC_1));

        assertEquals(0x4033, config.getStartAddress(ASduType.M_IT_NA_1));
        assertEquals(10, config.getCount(ASduType.M_IT_NA_1));

        assertEquals(0x4001, config.getStartAddress(ASduType.M_ME_NA_1));
        assertEquals(0, config.getCount(ASduType.M_ME_NA_1));

        assertEquals(0, config.getStartAddress(ASduType.C_SC_NA_1));
        assertEquals(0, config.getCount(ASduType.C_SC_NA_1));
    }

    @Test
    void testServerConfigCopy() {
        ServerConfig original = new ServerConfig();
        original.setBindAddress("192.168.1.200");
        original.setPort(2404);
        original.setCommonAddress(1);
        original.setTypeConfig(ASduType.M_SP_NA_1, 100, 200);
        original.setTypeConfig(ASduType.M_ME_NC_1, 20000, 100);

        ServerConfig copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getBindAddress(), copy.getBindAddress());
        assertEquals(original.getPort(), copy.getPort());
        assertEquals(original.getCommonAddress(), copy.getCommonAddress());
        assertEquals(100, copy.getStartAddress(ASduType.M_SP_NA_1));
        assertEquals(200, copy.getCount(ASduType.M_SP_NA_1));
        assertEquals(20000, copy.getStartAddress(ASduType.M_ME_NC_1));
        assertEquals(100, copy.getCount(ASduType.M_ME_NC_1));

        copy.setTypeConfig(ASduType.M_SP_NA_1, 500, 10);
        assertEquals(100, original.getStartAddress(ASduType.M_SP_NA_1));
        assertEquals(200, original.getCount(ASduType.M_SP_NA_1));
    }
}
