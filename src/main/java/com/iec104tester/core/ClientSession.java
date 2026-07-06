package com.iec104tester.core;

import com.iec104tester.capture.CaptureManager;
import com.iec104tester.model.ConnectionConfig;
import com.openmuc.j60870.ASdu;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 封装一个客户端连接的完整状态：配置、连接管理器、报文捕获、数据模型。
 * 每个 ClientSession 实例对应一个独立的 IEC104 连接。
 * 支持多回调监听（ClientPanel 和 SessionPanel 可同时监听状态变化）。
 */
public class ClientSession {

    private String name;
    private ConnectionConfig config;
    private final ClientManager clientManager;
    private final CaptureManager captureManager;

    // 四类数据视图模型
    private final DataTableModel telesignalingModel = new DataTableModel();
    private final DataTableModel telemetryModel = new DataTableModel();
    private final DataTableModel teleadjustModel = new DataTableModel();
    private final DataTableModel telecommandModel = new DataTableModel();

    // 多回调列表（支持 ClientPanel 和 SessionPanel 同时监听）
    private final List<Consumer<ClientManager.ConnectionState>> stateCallbacks = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> errorCallbacks = new CopyOnWriteArrayList<>();
    private final List<Consumer<ASdu>> asduCallbacks = new CopyOnWriteArrayList<>();

    public ClientSession(String name, ConnectionConfig config) {
        this.name = name;
        this.config = config;
        this.clientManager = new ClientManager();
        this.captureManager = new CaptureManager();
        this.clientManager.setCaptureManager(captureManager);
        setupCallbacks();
    }

    private void setupCallbacks() {
        clientManager.setStateCallback(state -> {
            for (Consumer<ClientManager.ConnectionState> cb : stateCallbacks) {
                cb.accept(state);
            }
        });
        clientManager.setErrorCallback(msg -> {
            for (Consumer<String> cb : errorCallbacks) {
                cb.accept(msg);
            }
        });
        clientManager.setAsduCallback(asdu -> {
            for (Consumer<ASdu> cb : asduCallbacks) {
                cb.accept(asdu);
            }
        });
    }

    public void addStateCallback(Consumer<ClientManager.ConnectionState> callback) {
        stateCallbacks.add(callback);
    }

    public void addErrorCallback(Consumer<String> callback) {
        errorCallbacks.add(callback);
    }

    public void addAsduCallback(Consumer<ASdu> callback) {
        asduCallbacks.add(callback);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ConnectionConfig getConfig() { return config; }
    public void setConfig(ConnectionConfig config) { this.config = config; }

    public ClientManager getClientManager() { return clientManager; }
    public CaptureManager getCaptureManager() { return captureManager; }

    public DataTableModel getTelesignalingModel() { return telesignalingModel; }
    public DataTableModel getTelemetryModel() { return telemetryModel; }
    public DataTableModel getTeleadjustModel() { return teleadjustModel; }
    public DataTableModel getTelecommandModel() { return telecommandModel; }

    public DataTableModel getModelForCategory(ConnectionConfig.DataCategory category) {
        switch (category) {
            case TELESIGNALING: return telesignalingModel;
            case TELEMETRY: return telemetryModel;
            case TELEADJUST: return teleadjustModel;
            case TELECOMMAND: return telecommandModel;
            default: return null;
        }
    }

    public void clearData() {
        telesignalingModel.clear();
        telemetryModel.clear();
        teleadjustModel.clear();
        telecommandModel.clear();
        captureManager.clearPackets();
    }

    public String getStatusText() {
        switch (clientManager.getState()) {
            case DISCONNECTED: return "未连接";
            case CONNECTING: return "连接中";
            case CONNECTED: return "已连接";
            case ERROR: return "错误";
            default: return clientManager.getState().toString();
        }
    }

    /**
     * 通用数据表模型，用于遥信/遥测/遥调/遥控四个视图
     */
    public static class DataTableModel extends AbstractTableModel {
        private final String[] columns = {"IOA", "类型", "值", "品质", "传送原因", "更新时间"};
        private final Map<Integer, Object[]> rowData = new ConcurrentHashMap<>();
        private final List<Integer> ioaList = new ArrayList<>();

        @Override
        public int getRowCount() {
            synchronized (ioaList) {
                return ioaList.size();
            }
        }

        @Override
        public int getColumnCount() { return columns.length; }

        @Override
        public String getColumnName(int col) { return columns[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            synchronized (ioaList) {
                if (row >= ioaList.size()) return "";
                Integer ioa = ioaList.get(row);
                Object[] data = rowData.get(ioa);
                if (data == null) return "";
                return data[col];
            }
        }

        public void updateOrUpdate(int ioa, String type, String value, String quality, String cot, String time) {
            boolean isNew;
            synchronized (ioaList) {
                isNew = !rowData.containsKey(ioa);
                if (isNew) {
                    ioaList.add(ioa);
                    Collections.sort(ioaList);
                }
                rowData.put(ioa, new Object[]{ioa, type, value, quality, cot, time});
            }
            if (isNew) {
                int row;
                synchronized (ioaList) {
                    row = ioaList.indexOf(ioa);
                }
                if (row >= 0) {
                    fireTableRowsInserted(row, row);
                }
            } else {
                int row;
                synchronized (ioaList) {
                    row = ioaList.indexOf(ioa);
                }
                if (row >= 0) {
                    fireTableRowsUpdated(row, row);
                }
            }
        }

        public void clear() {
            rowData.clear();
            ioaList.clear();
            fireTableDataChanged();
        }
    }
}
