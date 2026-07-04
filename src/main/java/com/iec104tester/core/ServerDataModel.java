package com.iec104tester.core;

import com.iec104tester.model.DataPointInfo;
import com.openmuc.j60870.ASduType;
import com.openmuc.j60870.CauseOfTransmission;
import com.openmuc.j60870.ie.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages server data points. Supports runtime add/remove/update.
 * Builds InformationObjects for interrogation responses and spontaneous transmission.
 */
public class ServerDataModel {

    private final Map<Integer, DataPointInfo> pointMap = new ConcurrentHashMap<>();
    private final Set<Integer> changedAddresses = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void addDataPoint(DataPointInfo point) {
        pointMap.put(point.getAddress(), point);
        changedAddresses.add(point.getAddress());
    }

    public void removeDataPoint(int address) {
        pointMap.remove(address);
        changedAddresses.remove(address);
    }

    public DataPointInfo getDataPoint(int address) {
        return pointMap.get(address);
    }

    public void updateValue(int address, double value) {
        DataPointInfo point = pointMap.get(address);
        if (point != null) {
            point.setCurrentValue(value);
            changedAddresses.add(address);
        }
    }

    public void updateValue(int address, double value, boolean qualityOk) {
        DataPointInfo point = pointMap.get(address);
        if (point != null) {
            point.setCurrentValue(value);
            point.setQualityOk(qualityOk);
            changedAddresses.add(address);
        }
    }

    public List<DataPointInfo> getAllPoints() {
        List<DataPointInfo> list = new ArrayList<>(pointMap.values());
        list.sort(Comparator.comparingInt(DataPointInfo::getAddress));
        return list;
    }

    public List<DataPointInfo> getPointsByType(ASduType type) {
        List<DataPointInfo> result = new ArrayList<>();
        for (DataPointInfo point : pointMap.values()) {
            if (point.getAsduType() == type) {
                result.add(point);
            }
        }
        result.sort(Comparator.comparingInt(DataPointInfo::getAddress));
        return result;
    }

    /**
     * Build InformationObjects for interrogation response for a given type.
     * Groups points into InformationObjects (each can hold multiple IOs if addresses are consecutive).
     */
    public List<InformationObject> buildInterrogationObjects(ASduType type) {
        List<DataPointInfo> points = getPointsByType(type);
        List<InformationObject> objects = new ArrayList<>();

        for (DataPointInfo point : points) {
            InformationObject obj = buildInformationObject(point);
            if (obj != null) {
                objects.add(obj);
            }
        }
        return objects;
    }

    /**
     * Build a single InformationObject for a data point.
     */
    public InformationObject buildInformationObject(DataPointInfo point) {
        if (point == null) return null;
        int ioa = point.getAddress();
        boolean invalid = !point.isQualityOk();

        switch (point.getAsduType()) {
            case M_SP_NA_1:
                return new InformationObject(ioa,
                        new IeSinglePointWithQuality(point.getCurrentValue() > 0, false, false, false, invalid));
            case M_DP_NA_1: {
                int state = (int) point.getCurrentValue();
                IeDoublePointWithQuality.DoublePointInformation dpi =
                        (state == 1) ? IeDoublePointWithQuality.DoublePointInformation.OFF :
                        (state == 2) ? IeDoublePointWithQuality.DoublePointInformation.ON :
                        IeDoublePointWithQuality.DoublePointInformation.INDETERMINATE_OR_INTERMEDIATE;
                return new InformationObject(ioa,
                        new IeDoublePointWithQuality(dpi, false, false, false, invalid));
            }
            case M_ME_NA_1:
                return new InformationObject(ioa,
                        new IeNormalizedValue((int) point.getCurrentValue()),
                        new IeQuality(false, false, false, false, invalid));
            case M_ME_NB_1:
                return new InformationObject(ioa,
                        new IeScaledValue((int) point.getCurrentValue()),
                        new IeQuality(false, false, false, false, invalid));
            case M_ME_NC_1:
                return new InformationObject(ioa,
                        new IeShortFloat((float) point.getCurrentValue()),
                        new IeQuality(false, false, false, false, invalid));
            case M_IT_NA_1: {
                IeBinaryCounterReading bcr;
                if (invalid) {
                    bcr = new IeBinaryCounterReading(
                            (int) point.getCurrentValue(), 0,
                            IeBinaryCounterReading.Flag.INVALID);
                } else {
                    bcr = new IeBinaryCounterReading(
                            (int) point.getCurrentValue(), 0);
                }
                return new InformationObject(ioa, bcr);
            }
            case M_SP_TB_1:
                return new InformationObject(ioa,
                        new IeSinglePointWithQuality(point.getCurrentValue() > 0, false, false, false, invalid),
                        new IeTime56(System.currentTimeMillis()));
            default:
                return null;
        }
    }

    /**
     * Get all changed data points and clear the change flags.
     */
    public List<DataPointInfo> getAndClearChangedPoints() {
        if (changedAddresses.isEmpty()) {
            return Collections.emptyList();
        }
        List<DataPointInfo> changed = new ArrayList<>();
        for (Integer addr : changedAddresses) {
            DataPointInfo point = pointMap.get(addr);
            if (point != null) {
                changed.add(point);
            }
        }
        changedAddresses.clear();
        return changed;
    }

    public boolean hasChanges() {
        return !changedAddresses.isEmpty();
    }

    public int getPointCount() {
        return pointMap.size();
    }

    /**
     * Get all ASDU types that have data points.
     */
    public Set<ASduType> getUsedTypes() {
        Set<ASduType> types = new TreeSet<>();
        for (DataPointInfo point : pointMap.values()) {
            if (point.getAsduType() != null) {
                types.add(point.getAsduType());
            }
        }
        return types;
    }
}
