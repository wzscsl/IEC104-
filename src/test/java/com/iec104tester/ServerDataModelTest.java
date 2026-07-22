package com.iec104tester;

import com.iec104tester.core.ServerDataModel;
import com.iec104tester.model.DataPointInfo;
import com.openmuc.j60870.ASduType;
import com.openmuc.j60870.ie.InformationObject;
import com.openmuc.j60870.ie.IeBinaryCounterReading;
import com.openmuc.j60870.ie.IeDoublePointWithQuality;
import com.openmuc.j60870.ie.IeQuality;
import com.openmuc.j60870.ie.IeShortFloat;
import com.openmuc.j60870.ie.IeSinglePointWithQuality;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ServerDataModel 单元测试。
 * 覆盖 add/remove/update、buildInformationObject、变更追踪。
 */
class ServerDataModelTest {

    @Test
    void testAddAndGetDataPoint() {
        ServerDataModel model = new ServerDataModel();
        DataPointInfo point = new DataPointInfo(100, "YX_100", ASduType.M_SP_NA_1, 0);
        point.setQualityOk(true);
        model.addDataPoint(point);

        assertEquals(1, model.getPointCount());
        assertEquals(point, model.getDataPoint(100));
        assertNull(model.getDataPoint(999));
    }

    @Test
    void testRemoveDataPoint() {
        ServerDataModel model = new ServerDataModel();
        model.addDataPoint(new DataPointInfo(100, "YX_100", ASduType.M_SP_NA_1, 0));
        assertEquals(1, model.getPointCount());

        model.removeDataPoint(100);
        assertEquals(0, model.getPointCount());
        assertNull(model.getDataPoint(100));
    }

    @Test
    void testAddSameAddressReplaces() {
        ServerDataModel model = new ServerDataModel();
        model.addDataPoint(new DataPointInfo(100, "YX_100", ASduType.M_SP_NA_1, 0));
        model.addDataPoint(new DataPointInfo(100, "YX_100_v2", ASduType.M_SP_NA_1, 1));

        // 同一地址应覆盖
        assertEquals(1, model.getPointCount());
        assertEquals("YX_100_v2", model.getDataPoint(100).getName());
    }

    @Test
    void testUpdateValue() {
        ServerDataModel model = new ServerDataModel();
        model.addDataPoint(new DataPointInfo(100, "YX_100", ASduType.M_SP_NA_1, 0));

        model.updateValue(100, 1.0);
        assertEquals(1.0, model.getDataPoint(100).getCurrentValue(), 0.0001);

        // 不存在的地址应被忽略
        model.updateValue(999, 1.0);
        assertEquals(1, model.getPointCount());
    }

    @Test
    void testUpdateValueWithQuality() {
        ServerDataModel model = new ServerDataModel();
        model.addDataPoint(new DataPointInfo(100, "YX_100", ASduType.M_SP_NA_1, 0));

        model.updateValue(100, 1.0, false);
        assertFalse(model.getDataPoint(100).isQualityOk());
        assertEquals(1.0, model.getDataPoint(100).getCurrentValue(), 0.0001);
    }

    @Test
    void testGetAllPointsSortedByAddress() {
        ServerDataModel model = new ServerDataModel();
        model.addDataPoint(new DataPointInfo(300, "P3", ASduType.M_SP_NA_1, 0));
        model.addDataPoint(new DataPointInfo(100, "P1", ASduType.M_SP_NA_1, 0));
        model.addDataPoint(new DataPointInfo(200, "P2", ASduType.M_SP_NA_1, 0));

        List<DataPointInfo> all = model.getAllPoints();
        assertEquals(3, all.size());
        // 应按地址升序排序
        assertEquals(100, all.get(0).getAddress());
        assertEquals(200, all.get(1).getAddress());
        assertEquals(300, all.get(2).getAddress());
    }

    @Test
    void testGetPointsByType() {
        ServerDataModel model = new ServerDataModel();
        model.addDataPoint(new DataPointInfo(1, "YX_1", ASduType.M_SP_NA_1, 0));
        model.addDataPoint(new DataPointInfo(2, "YX_2", ASduType.M_SP_NA_1, 0));
        model.addDataPoint(new DataPointInfo(100, "YC_1", ASduType.M_ME_NC_1, 0));

        List<DataPointInfo> spPoints = model.getPointsByType(ASduType.M_SP_NA_1);
        assertEquals(2, spPoints.size());

        List<DataPointInfo> sfPoints = model.getPointsByType(ASduType.M_ME_NC_1);
        assertEquals(1, sfPoints.size());
        assertEquals("YC_1", sfPoints.get(0).getName());
    }

    @Test
    void testGetUsedTypes() {
        ServerDataModel model = new ServerDataModel();
        model.addDataPoint(new DataPointInfo(1, "YX_1", ASduType.M_SP_NA_1, 0));
        model.addDataPoint(new DataPointInfo(2, "YX_2", ASduType.M_DP_NA_1, 0));
        model.addDataPoint(new DataPointInfo(100, "YC_1", ASduType.M_ME_NC_1, 0));

        Set<ASduType> types = model.getUsedTypes();
        assertEquals(3, types.size());
        assertTrue(types.contains(ASduType.M_SP_NA_1));
        assertTrue(types.contains(ASduType.M_DP_NA_1));
        assertTrue(types.contains(ASduType.M_ME_NC_1));
    }

    @Test
    void testBuildInformationObjectSinglePoint() {
        ServerDataModel model = new ServerDataModel();
        DataPointInfo point = new DataPointInfo(100, "YX_100", ASduType.M_SP_NA_1, 1);
        point.setQualityOk(true);
        model.addDataPoint(point);

        InformationObject obj = model.buildInformationObject(point);
        assertNotNull(obj);
        assertEquals(100, obj.getInformationObjectAddress());

        IeSinglePointWithQuality sp = (IeSinglePointWithQuality) obj.getInformationElements()[0][0];
        assertTrue(sp.isOn());
        assertFalse(sp.isInvalid());
    }

    @Test
    void testBuildInformationObjectShortFloat() {
        ServerDataModel model = new ServerDataModel();
        DataPointInfo point = new DataPointInfo(16385, "YC_1", ASduType.M_ME_NC_1, 3.14);
        point.setQualityOk(true);
        model.addDataPoint(point);

        InformationObject obj = model.buildInformationObject(point);
        assertNotNull(obj);

        IeShortFloat sf = (IeShortFloat) obj.getInformationElements()[0][0];
        assertEquals(3.14f, sf.getValue(), 0.0001f);

        IeQuality q = (IeQuality) obj.getInformationElements()[0][1];
        assertFalse(q.isInvalid());
    }

    @Test
    void testBuildInformationObjectDoublePointStates() {
        ServerDataModel model = new ServerDataModel();

        // state=1 -> OFF
        DataPointInfo p1 = new DataPointInfo(1, "DP1", ASduType.M_DP_NA_1, 1);
        model.addDataPoint(p1);
        InformationObject obj1 = model.buildInformationObject(p1);
        IeDoublePointWithQuality dp1 = (IeDoublePointWithQuality) obj1.getInformationElements()[0][0];
        assertEquals(IeDoublePointWithQuality.DoublePointInformation.OFF, dp1.getDoublePointInformation());

        // state=2 -> ON
        DataPointInfo p2 = new DataPointInfo(2, "DP2", ASduType.M_DP_NA_1, 2);
        model.addDataPoint(p2);
        InformationObject obj2 = model.buildInformationObject(p2);
        IeDoublePointWithQuality dp2 = (IeDoublePointWithQuality) obj2.getInformationElements()[0][0];
        assertEquals(IeDoublePointWithQuality.DoublePointInformation.ON, dp2.getDoublePointInformation());

        // 其他 -> INDETERMINATE
        DataPointInfo p3 = new DataPointInfo(3, "DP3", ASduType.M_DP_NA_1, 0);
        model.addDataPoint(p3);
        InformationObject obj3 = model.buildInformationObject(p3);
        IeDoublePointWithQuality dp3 = (IeDoublePointWithQuality) obj3.getInformationElements()[0][0];
        assertEquals(IeDoublePointWithQuality.DoublePointInformation.INDETERMINATE_OR_INTERMEDIATE, dp3.getDoublePointInformation());
    }

    @Test
    void testBuildInformationObjectCounterWithInvalidFlag() {
        ServerDataModel model = new ServerDataModel();
        DataPointInfo point = new DataPointInfo(4096, "DD_1", ASduType.M_IT_NA_1, 12345);
        point.setQualityOk(false);  // 无效品质
        model.addDataPoint(point);

        InformationObject obj = model.buildInformationObject(point);
        assertNotNull(obj);

        IeBinaryCounterReading bcr = (IeBinaryCounterReading) obj.getInformationElements()[0][0];
        assertEquals(12345, bcr.getCounterReading());
        // 无效品质时应携带 INVALID 标志
        assertTrue(bcr.getFlags().contains(IeBinaryCounterReading.Flag.INVALID));
    }

    @Test
    void testBuildInformationObjectNullPoint() {
        ServerDataModel model = new ServerDataModel();
        assertNull(model.buildInformationObject(null));
    }

    @Test
    void testBuildInformationObjectUnsupportedType() {
        ServerDataModel model = new ServerDataModel();
        // C_CS_NA_1 时钟同步类型不在 buildInformationObject 支持列表内
        DataPointInfo point = new DataPointInfo(0, "Clock", ASduType.C_CS_NA_1, 0);
        model.addDataPoint(point);

        assertNull(model.buildInformationObject(point));
    }

    @Test
    void testChangedTrackingAndGetAndClear() {
        ServerDataModel model = new ServerDataModel();
        model.addDataPoint(new DataPointInfo(1, "P1", ASduType.M_SP_NA_1, 0));
        model.addDataPoint(new DataPointInfo(2, "P2", ASduType.M_SP_NA_1, 0));

        // 添加时已标记为变更
        assertTrue(model.hasChanges());

        List<DataPointInfo> changed = model.getAndClearChangedPoints();
        assertEquals(2, changed.size());
        assertFalse(model.hasChanges());

        // 再次获取应为空
        List<DataPointInfo> changed2 = model.getAndClearChangedPoints();
        assertTrue(changed2.isEmpty());
    }

    @Test
    void testUpdateValueMarksAsChanged() {
        ServerDataModel model = new ServerDataModel();
        DataPointInfo point = new DataPointInfo(1, "P1", ASduType.M_SP_NA_1, 0);
        model.addDataPoint(point);
        model.getAndClearChangedPoints();  // 清空

        assertFalse(model.hasChanges());
        model.updateValue(1, 1.0);
        assertTrue(model.hasChanges());

        List<DataPointInfo> changed = model.getAndClearChangedPoints();
        assertEquals(1, changed.size());
        assertEquals(1.0, changed.get(0).getCurrentValue(), 0.0001);
    }

    @Test
    void testRemoveDataPointClearsChangeFlag() {
        ServerDataModel model = new ServerDataModel();
        model.addDataPoint(new DataPointInfo(1, "P1", ASduType.M_SP_NA_1, 0));
        model.removeDataPoint(1);

        // 删除后变更列表也应清掉该地址
        List<DataPointInfo> changed = model.getAndClearChangedPoints();
        assertTrue(changed.isEmpty());
    }

    @Test
    void testBuildInterrogationObjectsByType() {
        ServerDataModel model = new ServerDataModel();
        model.addDataPoint(new DataPointInfo(1, "YX_1", ASduType.M_SP_NA_1, 0));
        model.addDataPoint(new DataPointInfo(2, "YX_2", ASduType.M_SP_NA_1, 1));
        model.addDataPoint(new DataPointInfo(100, "YC_1", ASduType.M_ME_NC_1, 3.14));

        List<InformationObject> spObjs = model.buildInterrogationObjects(ASduType.M_SP_NA_1);
        assertEquals(2, spObjs.size());

        List<InformationObject> sfObjs = model.buildInterrogationObjects(ASduType.M_ME_NC_1);
        assertEquals(1, sfObjs.size());

        // 无该类型数据时返回空
        List<InformationObject> empty = model.buildInterrogationObjects(ASduType.C_SC_NA_1);
        assertTrue(empty.isEmpty());
    }
}
