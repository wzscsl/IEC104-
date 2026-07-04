package com.iec104tester.model;

import com.openmuc.j60870.ASduType;

/**
 * Represents a data point in the server's data model.
 */
public class DataPointInfo {

    private int address;
    private String name;
    private ASduType asduType;
    private double currentValue;
    private boolean qualityOk = true;
    private double deadLine;
    private int scaleIndex = 1;

    public DataPointInfo() {
    }

    public DataPointInfo(int address, String name, ASduType asduType, double currentValue) {
        this.address = address;
        this.name = name;
        this.asduType = asduType;
        this.currentValue = currentValue;
    }

    public int getAddress() { return address; }
    public void setAddress(int address) { this.address = address; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ASduType getAsduType() { return asduType; }
    public void setAsduType(ASduType asduType) { this.asduType = asduType; }

    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }

    public boolean isQualityOk() { return qualityOk; }
    public void setQualityOk(boolean qualityOk) { this.qualityOk = qualityOk; }

    public double getDeadLine() { return deadLine; }
    public void setDeadLine(double deadLine) { this.deadLine = deadLine; }

    public int getScaleIndex() { return scaleIndex; }
    public void setScaleIndex(int scaleIndex) { this.scaleIndex = scaleIndex; }

    public String getTypeStr() {
        return asduType != null ? asduType.toString() : "";
    }

    public String getValueStr() {
        if (asduType == ASduType.M_SP_NA_1 || asduType == ASduType.M_SP_TB_1) {
            return currentValue > 0 ? "ON" : "OFF";
        }
        if (asduType == ASduType.M_DP_NA_1) {
            int state = (int) currentValue;
            switch (state) {
                case 1: return "OFF";
                case 2: return "ON";
                default: return "INTERMEDIATE";
            }
        }
        return String.valueOf(currentValue);
    }

    public String getQualityStr() {
        return qualityOk ? "OK" : "INVALID";
    }
}
