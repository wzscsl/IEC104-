package com.openmuc.j60870;

import com.openmuc.j60870.ie.IeBinaryCounterReading;
import com.openmuc.j60870.ie.IeDoublePointWithQuality;
import com.openmuc.j60870.ie.IeNormalizedValue;
import com.openmuc.j60870.ie.IeQuality;
import com.openmuc.j60870.ie.IeScaledValue;
import com.openmuc.j60870.ie.IeShortFloat;
import com.openmuc.j60870.ie.IeSinglePointWithQuality;
import com.openmuc.j60870.ie.InformationObject;
import com.openmuc.j60870.internal.ExtendedDataInputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ASDU 编解码核心逻辑测试。
 * 放在 com.openmuc.j60870 包下以访问包级私有的 ASdu.encode/decode 和 ConnectionSettings。
 */
class AsduEncodeDecodeTest {

    private ASdu roundTrip(ASdu original) throws IOException {
        ConnectionSettings settings = new ConnectionSettings();
        byte[] buffer = new byte[253];
        int length = original.encode(buffer, 0, settings);
        ExtendedDataInputStream is =
                new ExtendedDataInputStream(new ByteArrayInputStream(buffer, 0, length));
        return ASdu.decode(is, settings, length);
    }

    @Test
    void testMspNa1EncodeDecode() throws IOException {
        InformationObject io = new InformationObject(1234,
                new IeSinglePointWithQuality(true, false, false, false, false));
        ASdu original = new ASdu(ASduType.M_SP_NA_1, false, CauseOfTransmission.SPONTANEOUS,
                false, false, 0, 1, io);

        ASdu decoded = roundTrip(original);

        assertEquals(ASduType.M_SP_NA_1, decoded.getTypeIdentification());
        assertEquals(CauseOfTransmission.SPONTANEOUS, decoded.getCauseOfTransmission());
        assertEquals(1, decoded.getCommonAddress());
        assertFalse(decoded.isSequenceOfElements());

        InformationObject[] ios = decoded.getInformationObjects();
        assertEquals(1, ios.length);
        assertEquals(1234, ios[0].getInformationObjectAddress());

        IeSinglePointWithQuality sp =
                (IeSinglePointWithQuality) ios[0].getInformationElements()[0][0];
        assertTrue(sp.isOn());
        assertFalse(sp.isBlocked());
        assertFalse(sp.isSubstituted());
        assertFalse(sp.isNotTopical());
        assertFalse(sp.isInvalid());
    }

    @Test
    void testMMeNc1EncodeDecode() throws IOException {
        InformationObject io = new InformationObject(16385,
                new IeShortFloat(123.456f),
                new IeQuality(true, false, false, false, false));
        ASdu original = new ASdu(ASduType.M_ME_NC_1, false, CauseOfTransmission.PERIODIC,
                false, false, 0, 1, io);

        ASdu decoded = roundTrip(original);

        assertEquals(ASduType.M_ME_NC_1, decoded.getTypeIdentification());
        assertEquals(CauseOfTransmission.PERIODIC, decoded.getCauseOfTransmission());
        assertEquals(1, decoded.getCommonAddress());

        InformationObject[] ios = decoded.getInformationObjects();
        assertEquals(1, ios.length);
        assertEquals(16385, ios[0].getInformationObjectAddress());

        IeShortFloat sf = (IeShortFloat) ios[0].getInformationElements()[0][0];
        assertEquals(123.456f, sf.getValue(), 0.0001f);

        IeQuality q = (IeQuality) ios[0].getInformationElements()[0][1];
        assertTrue(q.isOverflow());
        assertFalse(q.isInvalid());
    }

    @Test
    void testMMeNa1EncodeDecode() throws IOException {
        InformationObject io = new InformationObject(16386,
                new IeNormalizedValue(16000),
                new IeQuality(false, false, false, false, false));
        ASdu original = new ASdu(ASduType.M_ME_NA_1, false, CauseOfTransmission.BACKGROUND_SCAN,
                false, false, 0, 1, io);

        ASdu decoded = roundTrip(original);

        assertEquals(ASduType.M_ME_NA_1, decoded.getTypeIdentification());
        assertEquals(CauseOfTransmission.BACKGROUND_SCAN, decoded.getCauseOfTransmission());

        InformationObject[] ios = decoded.getInformationObjects();
        assertEquals(1, ios.length);
        assertEquals(16386, ios[0].getInformationObjectAddress());

        IeNormalizedValue nv = (IeNormalizedValue) ios[0].getInformationElements()[0][0];
        assertEquals(16000, nv.getUnnormalizedValue());
        assertEquals(16000.0 / 32768.0, nv.getNormalizedValue(), 0.0000001);

        IeQuality q = (IeQuality) ios[0].getInformationElements()[0][1];
        assertFalse(q.isOverflow());
        assertFalse(q.isInvalid());
    }

    @Test
    void testMMeNb1EncodeDecode() throws IOException {
        InformationObject io = new InformationObject(16387,
                new IeScaledValue(-500),
                new IeQuality(false, false, false, false, false));
        ASdu original = new ASdu(ASduType.M_ME_NB_1, false, CauseOfTransmission.SPONTANEOUS,
                false, false, 0, 1, io);

        ASdu decoded = roundTrip(original);

        assertEquals(ASduType.M_ME_NB_1, decoded.getTypeIdentification());
        assertEquals(CauseOfTransmission.SPONTANEOUS, decoded.getCauseOfTransmission());

        InformationObject[] ios = decoded.getInformationObjects();
        assertEquals(1, ios.length);
        assertEquals(16387, ios[0].getInformationObjectAddress());

        IeScaledValue sv = (IeScaledValue) ios[0].getInformationElements()[0][0];
        assertEquals(-500, sv.getUnnormalizedValue());
    }

    @Test
    void testMDpNa1EncodeDecode() throws IOException {
        InformationObject io = new InformationObject(8193,
                new IeDoublePointWithQuality(
                        IeDoublePointWithQuality.DoublePointInformation.ON,
                        false, false, false, false));
        ASdu original = new ASdu(ASduType.M_DP_NA_1, false, CauseOfTransmission.SPONTANEOUS,
                false, false, 0, 1, io);

        ASdu decoded = roundTrip(original);

        assertEquals(ASduType.M_DP_NA_1, decoded.getTypeIdentification());
        assertEquals(CauseOfTransmission.SPONTANEOUS, decoded.getCauseOfTransmission());

        InformationObject[] ios = decoded.getInformationObjects();
        assertEquals(1, ios.length);
        assertEquals(8193, ios[0].getInformationObjectAddress());

        IeDoublePointWithQuality dp =
                (IeDoublePointWithQuality) ios[0].getInformationElements()[0][0];
        assertEquals(IeDoublePointWithQuality.DoublePointInformation.ON, dp.getDoublePointInformation());
    }

    @Test
    void testMItNa1EncodeDecode() throws IOException {
        InformationObject io = new InformationObject(16435,
                new IeBinaryCounterReading(123456, 7));
        ASdu original = new ASdu(ASduType.M_IT_NA_1, false,
                CauseOfTransmission.REQUESTED_BY_GENERAL_COUNTER,
                false, false, 0, 1, io);

        ASdu decoded = roundTrip(original);

        assertEquals(ASduType.M_IT_NA_1, decoded.getTypeIdentification());
        assertEquals(CauseOfTransmission.REQUESTED_BY_GENERAL_COUNTER, decoded.getCauseOfTransmission());

        InformationObject[] ios = decoded.getInformationObjects();
        assertEquals(1, ios.length);
        assertEquals(16435, ios[0].getInformationObjectAddress());

        IeBinaryCounterReading bcr =
                (IeBinaryCounterReading) ios[0].getInformationElements()[0][0];
        assertEquals(123456, bcr.getCounterReading());
        assertEquals(7, bcr.getSequenceNumber());
    }

    @Test
    void testMultipleInformationObjects() throws IOException {
        InformationObject io1 = new InformationObject(1001,
                new IeSinglePointWithQuality(true, false, false, false, false));
        InformationObject io2 = new InformationObject(1002,
                new IeSinglePointWithQuality(false, false, false, false, false));
        InformationObject io3 = new InformationObject(1003,
                new IeSinglePointWithQuality(true, true, false, false, false));

        ASdu original = new ASdu(ASduType.M_SP_NA_1, false, CauseOfTransmission.SPONTANEOUS,
                false, false, 0, 1, io1, io2, io3);

        ASdu decoded = roundTrip(original);

        assertEquals(ASduType.M_SP_NA_1, decoded.getTypeIdentification());
        assertEquals(3, decoded.getSequenceLength());

        InformationObject[] ios = decoded.getInformationObjects();
        assertEquals(3, ios.length);
        assertEquals(1001, ios[0].getInformationObjectAddress());
        assertEquals(1002, ios[1].getInformationObjectAddress());
        assertEquals(1003, ios[2].getInformationObjectAddress());

        IeSinglePointWithQuality sp1 = (IeSinglePointWithQuality) ios[0].getInformationElements()[0][0];
        assertTrue(sp1.isOn());
        assertFalse(sp1.isBlocked());

        IeSinglePointWithQuality sp2 = (IeSinglePointWithQuality) ios[1].getInformationElements()[0][0];
        assertFalse(sp2.isOn());

        IeSinglePointWithQuality sp3 = (IeSinglePointWithQuality) ios[2].getInformationElements()[0][0];
        assertTrue(sp3.isOn());
        assertTrue(sp3.isBlocked());
    }

    @Test
    void testDifferentCauseOfTransmission() throws IOException {
        CauseOfTransmission[] cots = {
                CauseOfTransmission.PERIODIC,
                CauseOfTransmission.SPONTANEOUS,
                CauseOfTransmission.ACTIVATION,
                CauseOfTransmission.ACTIVATION_CON,
                CauseOfTransmission.INTERROGATED_BY_STATION,
                CauseOfTransmission.REQUEST
        };

        for (CauseOfTransmission cot : cots) {
            InformationObject io = new InformationObject(1,
                    new IeSinglePointWithQuality(true, false, false, false, false));
            ASdu original = new ASdu(ASduType.M_SP_NA_1, false, cot,
                    false, false, 0, 1, io);

            ASdu decoded = roundTrip(original);

            assertEquals(cot, decoded.getCauseOfTransmission(),
                    "COT mismatch for: " + cot);
            assertEquals(ASduType.M_SP_NA_1, decoded.getTypeIdentification());
        }
    }

    @Test
    void testDifferentCommonAddress() throws IOException {
        int[] addresses = {1, 255, 1000, 65534};

        for (int ca : addresses) {
            InformationObject io = new InformationObject(1,
                    new IeSinglePointWithQuality(true, false, false, false, false));
            ASdu original = new ASdu(ASduType.M_SP_NA_1, false, CauseOfTransmission.SPONTANEOUS,
                    false, false, 0, ca, io);

            ASdu decoded = roundTrip(original);

            assertEquals(ca, decoded.getCommonAddress(),
                    "Common address mismatch for: " + ca);
            assertEquals(ASduType.M_SP_NA_1, decoded.getTypeIdentification());
        }
    }

    @Test
    void testTestFrameAndNegativeConfirm() throws IOException {
        InformationObject io = new InformationObject(1,
                new IeSinglePointWithQuality(true, false, false, false, false));
        ASdu original = new ASdu(ASduType.M_SP_NA_1, false, CauseOfTransmission.SPONTANEOUS,
                true, true, 3, 1, io);

        ASdu decoded = roundTrip(original);

        assertTrue(decoded.isTestFrame());
        assertTrue(decoded.isNegativeConfirm());
        assertEquals(3, decoded.getOriginatorAddress());
    }
}
