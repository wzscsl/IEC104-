package com.iec104tester.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Client connection configuration for IEC 104.
 */
public class ConnectionConfig {

    private String host = "127.0.0.1";
    private int port = 2404;
    private int originatorAddress = 0;
    private int commonAddress = 1;
    private int connectionTimeout = 30000;
    private int messageFragmentTimeout = 5000;
    private int t1 = 15000;
    private int t2 = 10000;
    private int t3 = 20000;
    private int k = 12;
    private int w = 8;
    private int cotFieldLength = 2;
    private int commonAddressFieldLength = 2;
    private int ioaFieldLength = 3;

    /** IOA 范围配置：category -> [startAddress, count] */
    public enum DataCategory {
        TELESIGNALING("遥信"),
        TELEMETRY("遥测"),
        TELEADJUST("遥调"),
        TELECOMMAND("遥控");

        private final String label;
        DataCategory(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private Map<DataCategory, int[]> ioaRanges = new HashMap<>();

    public ConnectionConfig() {
        // 默认 IOA 范围（参考电力行业常用分配）
        // 遥信: 0x0001, 遥测: 0x4001(16385), 遥控: 0x6001(24577), 遥调: 0x6201(25089)
        ioaRanges.put(DataCategory.TELESIGNALING, new int[]{0x0001, 0});     // 遥信: 1 起
        ioaRanges.put(DataCategory.TELEMETRY, new int[]{0x4001, 0});         // 遥测: 16385 起
        ioaRanges.put(DataCategory.TELECOMMAND, new int[]{0x6001, 0});       // 遥控: 24577 起
        ioaRanges.put(DataCategory.TELEADJUST, new int[]{0x6201, 0});        // 遥调: 25089 起
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public int getOriginatorAddress() { return originatorAddress; }
    public void setOriginatorAddress(int originatorAddress) { this.originatorAddress = originatorAddress; }

    public int getCommonAddress() { return commonAddress; }
    public void setCommonAddress(int commonAddress) { this.commonAddress = commonAddress; }

    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

    public int getMessageFragmentTimeout() { return messageFragmentTimeout; }
    public void setMessageFragmentTimeout(int messageFragmentTimeout) { this.messageFragmentTimeout = messageFragmentTimeout; }

    public int getT1() { return t1; }
    public void setT1(int t1) { this.t1 = t1; }

    public int getT2() { return t2; }
    public void setT2(int t2) { this.t2 = t2; }

    public int getT3() { return t3; }
    public void setT3(int t3) { this.t3 = t3; }

    public int getK() { return k; }
    public void setK(int k) { this.k = k; }

    public int getW() { return w; }
    public void setW(int w) { this.w = w; }

    public int getCotFieldLength() { return cotFieldLength; }
    public void setCotFieldLength(int cotFieldLength) { this.cotFieldLength = cotFieldLength; }

    public int getCommonAddressFieldLength() { return commonAddressFieldLength; }
    public void setCommonAddressFieldLength(int commonAddressFieldLength) { this.commonAddressFieldLength = commonAddressFieldLength; }

    public int getIoaFieldLength() { return ioaFieldLength; }
    public void setIoaFieldLength(int ioaFieldLength) { this.ioaFieldLength = ioaFieldLength; }

    public int getIoaStart(DataCategory cat) {
        int[] r = ioaRanges.get(cat);
        return r != null ? r[0] : 0;
    }

    public int getIoaCount(DataCategory cat) {
        int[] r = ioaRanges.get(cat);
        return r != null ? r[1] : 0;
    }

    public void setIoaRange(DataCategory cat, int start, int count) {
        ioaRanges.put(cat, new int[]{start, count});
    }

    public boolean isIoaInRange(int ioa, DataCategory cat) {
        int[] r = ioaRanges.get(cat);
        if (r == null || r[1] <= 0) return true; // 未配置范围则全部显示
        return ioa >= r[0] && ioa < r[0] + r[1];
    }

    public ConnectionConfig copy() {
        ConnectionConfig c = new ConnectionConfig();
        c.host = host;
        c.port = port;
        c.originatorAddress = originatorAddress;
        c.commonAddress = commonAddress;
        c.connectionTimeout = connectionTimeout;
        c.messageFragmentTimeout = messageFragmentTimeout;
        c.t1 = t1;
        c.t2 = t2;
        c.t3 = t3;
        c.k = k;
        c.w = w;
        c.cotFieldLength = cotFieldLength;
        c.commonAddressFieldLength = commonAddressFieldLength;
        c.ioaFieldLength = ioaFieldLength;
        for (Map.Entry<DataCategory, int[]> entry : ioaRanges.entrySet()) {
            c.ioaRanges.put(entry.getKey(), entry.getValue().clone());
        }
        return c;
    }
}
