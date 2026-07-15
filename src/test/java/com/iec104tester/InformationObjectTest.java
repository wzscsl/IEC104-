package com.iec104tester;

import com.openmuc.j60870.ASduType;
import com.openmuc.j60870.ie.IeBinaryCounterReading;
import com.openmuc.j60870.ie.IeDoublePointWithQuality;
import com.openmuc.j60870.ie.IeNormalizedValue;
import com.openmuc.j60870.ie.IeQuality;
import com.openmuc.j60870.ie.IeScaledValue;
import com.openmuc.j60870.ie.IeShortFloat;
import com.openmuc.j60870.ie.IeSinglePointWithQuality;
import com.openmuc.j60870.ie.InformationElement;
import com.openmuc.j60870.ie.InformationObject;
import com.openmuc.j60870.internal.ExtendedDataInputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * InformationObject 编解码测试。
 * InformationObject.encode/decode 均为 public，可从外部包访问。
 */
class InformationObjectTest {

    private InformationObject roundTrip(InformationObject original, ASduType type, int ioaFieldLength)
            throws IOException {
        byte[] buffer = new byte[256];
        int length = original.encode(buffer, 0, ioaFieldLength);
        ExtendedDataInputStream is =
                new ExtendedDataInputStream(new ByteArrayInputStream(buffer, 0, length));
        return InformationObject.decode(is, type, 1, ioaFieldLength);
    }

    // ===== IOA 编码测试 =====

    @Test
    void testIoaEncoding1Byte() throws IOException {
        int ioa = 100;
        InformationObject io = new InformationObject(ioa,
                new IeSinglePointWithQuality(true, false, false, false, false));
        byte[] buffer = new byte[256];
        int length = io.encode(buffer, 0, 1);

        assertEquals(2, length);
        assertEquals(100, buffer[0] & 0xFF);

        ExtendedDataInputStream is =
                new ExtendedDataInputStream(new ByteArrayInputStream(buffer, 0, length));
        InformationObject decoded = InformationObject.decode(is, ASduType.M_SP_NA_1, 1, 1);
        assertEquals(ioa, decoded.getInformationObjectAddress());
    }

    @Test
    void testIoaEncoding2Byte() throws IOException {
        int ioa = 500;
        InformationObject io = new InformationObject(ioa,
                new IeSinglePointWithQuality(true, false, false, false, false));
        byte[] buffer = new byte[256];
        int length = io.encode(buffer, 0, 2);

        assertEquals(3, length);
        assertEquals(500 & 0xFF, buffer[0] & 0xFF);
        assertEquals((500 >> 8) & 0xFF, buffer[1] & 0xFF);

        ExtendedDataInputStream is =
                new ExtendedDataInputStream(new ByteArrayInputStream(buffer, 0, length));
        InformationObject decoded = InformationObject.decode(is, ASduType.M_SP_NA_1, 1, 2);
        assertEquals(ioa, decoded.getInformationObjectAddress());
    }

    @Test
    void testIoaEncoding3Byte() throws IOException {
        int ioa = 70000;
        InformationObject io = new InformationObject(ioa,
                new IeSinglePointWithQuality(true, false, false, false, false));
        byte[] buffer = new byte[256];
        int length = io.encode(buffer, 0, 3);

        assertEquals(4, length);
        assertEquals(70000 & 0xFF, buffer[0] & 0xFF);
        assertEquals((70000 >> 8) & 0xFF, buffer[1] & 0xFF);
        assertEquals((70000 >> 16) & 0xFF, buffer[2] & 0xFF);

        ExtendedDataInputStream is =
                new ExtendedDataInputStream(new ByteArrayInputStream(buffer, 0, length));
        InformationObject decoded = InformationObject.decode(is, ASduType.M_SP_NA_1, 1, 3);
        assertEquals(ioa, decoded.getInformationObjectAddress());
    }

    // ===== 信息元素 encode/decode 往返一致性测试 =====

    @Test
    void testSinglePointWithQualityRoundTrip() throws IOException {
        InformationObject io = new InformationObject(1234,
                new IeSinglePointWithQuality(true, false, true, false, true));
        InformationObject decoded = roundTrip(io, ASduType.M_SP_NA_1, 3);

        assertEquals(1234, decoded.getInformationObjectAddress());
        IeSinglePointWithQuality sp =
                (IeSinglePointWithQuality) decoded.getInformationElements()[0][0];
        assertTrue(sp.isOn());
        assertFalse(sp.isBlocked());
        assertTrue(sp.isSubstituted());
        assertFalse(sp.isNotTopical());
        assertTrue(sp.isInvalid());
    }

    @Test
    void testShortFloatRoundTrip() throws IOException {
        InformationObject io = new InformationObject(16385,
                new IeShortFloat(-3.14f),
                new IeQuality(false, false, false, false, false));
        InformationObject decoded = roundTrip(io, ASduType.M_ME_NC_1, 3);

        assertEquals(16385, decoded.getInformationObjectAddress());
        IeShortFloat sf = (IeShortFloat) decoded.getInformationElements()[0][0];
        assertEquals(-3.14f, sf.getValue(), 0.0001f);

        IeQuality q = (IeQuality) decoded.getInformationElements()[0][1];
        assertFalse(q.isOverflow());
    }

    @Test
    void testNormalizedValueRoundTrip() throws IOException {
        InformationObject io = new InformationObject(16386,
                new IeNormalizedValue(-32768),
                new IeQuality(true, false, false, false, false));
        InformationObject decoded = roundTrip(io, ASduType.M_ME_NA_1, 3);

        assertEquals(16386, decoded.getInformationObjectAddress());
        IeNormalizedValue nv = (IeNormalizedValue) decoded.getInformationElements()[0][0];
        assertEquals(-32768, nv.getUnnormalizedValue());

        IeQuality q = (IeQuality) decoded.getInformationElements()[0][1];
        assertTrue(q.isOverflow());
    }

    @Test
    void testScaledValueRoundTrip() throws IOException {
        InformationObject io = new InformationObject(16387,
                new IeScaledValue(32767),
                new IeQuality(false, false, false, false, false));
        InformationObject decoded = roundTrip(io, ASduType.M_ME_NB_1, 3);

        assertEquals(16387, decoded.getInformationObjectAddress());
        IeScaledValue sv = (IeScaledValue) decoded.getInformationElements()[0][0];
        assertEquals(32767, sv.getUnnormalizedValue());
    }

    @Test
    void testDoublePointWithQualityRoundTrip() throws IOException {
        InformationObject io = new InformationObject(8193,
                new IeDoublePointWithQuality(
                        IeDoublePointWithQuality.DoublePointInformation.OFF,
                        false, false, false, false));
        InformationObject decoded = roundTrip(io, ASduType.M_DP_NA_1, 3);

        assertEquals(8193, decoded.getInformationObjectAddress());
        IeDoublePointWithQuality dp =
                (IeDoublePointWithQuality) decoded.getInformationElements()[0][0];
        assertEquals(IeDoublePointWithQuality.DoublePointInformation.OFF, dp.getDoublePointInformation());
    }

    @Test
    void testBinaryCounterReadingRoundTrip() throws IOException {
        InformationObject io = new InformationObject(16435,
                new IeBinaryCounterReading(999999, 15));
        InformationObject decoded = roundTrip(io, ASduType.M_IT_NA_1, 3);

        assertEquals(16435, decoded.getInformationObjectAddress());
        IeBinaryCounterReading bcr =
                (IeBinaryCounterReading) decoded.getInformationElements()[0][0];
        assertEquals(999999, bcr.getCounterReading());
        assertEquals(15, bcr.getSequenceNumber());
    }
}
