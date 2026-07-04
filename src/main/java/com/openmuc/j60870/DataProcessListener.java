package com.openmuc.j60870;

/**
 * @ Author     ：dengzhihai
 * @ Date       ：Created in 10:00 2023/5/24
 * @ Description：iec104写数据监听
 * @ Modified By：
 * @Version:
 */
public interface DataProcessListener {
    void write(int adress, Number oldValue, Number newValue);
}
