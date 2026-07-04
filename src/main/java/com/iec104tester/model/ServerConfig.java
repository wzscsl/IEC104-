package com.iec104tester.model;

import com.openmuc.j60870.ASduType;

import java.util.HashMap;
import java.util.Map;

/**
 * Server configuration for IEC 104.
 */
public class ServerConfig {

    private String bindAddress = "0.0.0.0";
    private int port = 2404;
    private int maxConnections = 100;
    private int commonAddress = 1;
    private int cotFieldLength = 2;
    private int commonAddressFieldLength = 2;
    private int ioaFieldLength = 3;
    private int t1 = 15000;
    private int t2 = 10000;
    private int t3 = 20000;
    private int k = 12;
    private int w = 8;
    private boolean spontaneousEnabled = true;
    private int spontaneousInterval = 1;

    /** 数据类型配置：ASduType -> [startAddress, count] */
    private Map<ASduType, int[]> dataTypeConfig = new HashMap<>();

    public ServerConfig() {
        // 默认配置（参考电力行业常用 IOA 分配）
        // 遥信: 0x0001(1), 遥测: 0x4001(16385), 遥控: 0x6001(24577), 遥调: 0x6201(25089)
        dataTypeConfig.put(ASduType.M_SP_NA_1, new int[]{0x0001, 20});       // 单点遥信: 1 起
        dataTypeConfig.put(ASduType.M_DP_NA_1, new int[]{0x2001, 10});       // 双点遥信: 8193 起
        dataTypeConfig.put(ASduType.M_SP_TB_1, new int[]{0x3001, 0});        // 带时标单点: 12289 起
        dataTypeConfig.put(ASduType.M_ME_NA_1, new int[]{0x4001, 0});        // 归一化遥测: 16385 起
        dataTypeConfig.put(ASduType.M_ME_NB_1, new int[]{0x4801, 0});        // 标度化遥测: 18433 起
        dataTypeConfig.put(ASduType.M_ME_NC_1, new int[]{0x5001, 50});       // 短浮点遥测: 20481 起
        dataTypeConfig.put(ASduType.M_IT_NA_1, new int[]{0x5801, 10});       // 电度: 22529 起
    }

    public String getBindAddress() { return bindAddress; }
    public void setBindAddress(String bindAddress) { this.bindAddress = bindAddress; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public int getMaxConnections() { return maxConnections; }
    public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }

    public int getCommonAddress() { return commonAddress; }
    public void setCommonAddress(int commonAddress) { this.commonAddress = commonAddress; }

    public int getCotFieldLength() { return cotFieldLength; }
    public void setCotFieldLength(int cotFieldLength) { this.cotFieldLength = cotFieldLength; }

    public int getCommonAddressFieldLength() { return commonAddressFieldLength; }
    public void setCommonAddressFieldLength(int commonAddressFieldLength) { this.commonAddressFieldLength = commonAddressFieldLength; }

    public int getIoaFieldLength() { return ioaFieldLength; }
    public void setIoaFieldLength(int ioaFieldLength) { this.ioaFieldLength = ioaFieldLength; }

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

    public boolean isSpontaneousEnabled() { return spontaneousEnabled; }
    public void setSpontaneousEnabled(boolean spontaneousEnabled) { this.spontaneousEnabled = spontaneousEnabled; }

    public int getSpontaneousInterval() { return spontaneousInterval; }
    public void setSpontaneousInterval(int spontaneousInterval) { this.spontaneousInterval = spontaneousInterval; }

    public Map<ASduType, int[]> getDataTypeConfig() { return dataTypeConfig; }
    public void setDataTypeConfig(Map<ASduType, int[]> dataTypeConfig) { this.dataTypeConfig = dataTypeConfig; }

    public int getStartAddress(ASduType type) {
        int[] cfg = dataTypeConfig.get(type);
        return cfg != null ? cfg[0] : 0;
    }

    public int getCount(ASduType type) {
        int[] cfg = dataTypeConfig.get(type);
        return cfg != null ? cfg[1] : 0;
    }

    public void setTypeConfig(ASduType type, int startAddress, int count) {
        dataTypeConfig.put(type, new int[]{startAddress, count});
    }

    public ServerConfig copy() {
        ServerConfig c = new ServerConfig();
        c.bindAddress = bindAddress;
        c.port = port;
        c.maxConnections = maxConnections;
        c.commonAddress = commonAddress;
        c.cotFieldLength = cotFieldLength;
        c.commonAddressFieldLength = commonAddressFieldLength;
        c.ioaFieldLength = ioaFieldLength;
        c.t1 = t1;
        c.t2 = t2;
        c.t3 = t3;
        c.k = k;
        c.w = w;
        c.spontaneousEnabled = spontaneousEnabled;
        c.spontaneousInterval = spontaneousInterval;
        // 深拷贝数据类型配置
        for (Map.Entry<ASduType, int[]> entry : dataTypeConfig.entrySet()) {
            c.dataTypeConfig.put(entry.getKey(), entry.getValue().clone());
        }
        return c;
    }
}
