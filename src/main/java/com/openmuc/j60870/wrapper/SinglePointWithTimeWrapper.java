package com.openmuc.j60870.wrapper;

import com.openmuc.j60870.ASduType;
import com.openmuc.j60870.ie.IeSinglePointWithQuality;
import com.openmuc.j60870.ie.IeTime56;
import com.openmuc.j60870.ie.InformationElement;
import com.openmuc.j60870.ie.InformationObject;

import java.util.Map;

/**
 * 带时标单点遥信 M_SP_TB_1 (Type ID 30) — SOE 事件点
 */
public class SinglePointWithTimeWrapper extends PointWrapper {
    private final IeSinglePointWithQuality ieValue;
    private IeTime56 ieTime;
    private Boolean srcValue;
    private long lastTimestampMs;

    public SinglePointWithTimeWrapper(Map<String, String> pointMap, int address, String name, ASduType aSduType, float deadLine) {
        super(pointMap, address, name, aSduType, deadLine);
        ieValue = new IeSinglePointWithQuality(false, false, false, false, true);
        lastTimestampMs = System.currentTimeMillis();
        ieTime = new IeTime56(lastTimestampMs);
        srcValue = false;
        informationObject = new InformationObject(address, new InformationElement[][]{{ieValue, ieTime}});
    }

    public void setWithTimestamp(Object data, long timestampMs, boolean invalid) {
        boolean newValue = Float.parseFloat(data.toString()) > 0;
        ieTime = new IeTime56(timestampMs);
        lastTimestampMs = timestampMs;
        if (newValue != srcValue) {
            ieValue.setValue(newValue);
            setChangeFlag();
        }
        srcValue = newValue;
        ieValue.setInvalid(invalid);
        syncBatchElements();
    }

    @Override
    public void set(Object data, boolean invalid) {
        setWithTimestamp(data, System.currentTimeMillis(), invalid);
    }

    @Override
    public void set(Object data) {
        set(data, false);
    }

    @Override
    public void setInvalid(boolean invalid) {
        boolean oldInvalid = ieValue.isInvalid();
        ieValue.setInvalid(invalid);
        if (oldInvalid != invalid) {
            setChangeFlag();
        }
    }

    @Override
    public Object get() {
        return srcValue;
    }

    @Override
    public Number getNumber() {
        return srcValue ? 1 : 0;
    }

    public long getLastTimestampMs() {
        return lastTimestampMs;
    }

    private void syncBatchElements() {
        if (batchInformationObject == null) {
            return;
        }
        InformationElement[][] elements = batchInformationObject.getInformationElements();
        int index = address - batchInformationObject.getInformationObjectAddress();
        if (index >= 0 && index < elements.length) {
            elements[index][0] = ieValue;
            elements[index][1] = ieTime;
        }
    }
}
