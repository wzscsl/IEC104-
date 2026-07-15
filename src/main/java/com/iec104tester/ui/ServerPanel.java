package com.iec104tester.ui;

import com.iec104tester.capture.CaptureManager;
import com.iec104tester.core.ServerDataModel;
import com.iec104tester.core.ServerManager;
import com.iec104tester.model.DataPointInfo;
import com.iec104tester.model.SceneConfig;
import com.iec104tester.model.ServerConfig;
import com.openmuc.j60870.ASduType;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * 服务端模式面板
 */
public class ServerPanel extends JPanel {

    private final ServerManager serverManager;
    private final ServerDataModel dataModel;
    private final CaptureManager captureManager;

    private JTextField bindField;
    private JSpinner portSpinner;
    private JButton startBtn;
    private JButton stopBtn;
    private JButton settingsBtn;
    private JButton saveConfigBtn;
    private JButton loadConfigBtn;

    private ServerConfig config = new ServerConfig();
    private DataPointTableModel tableModel;
    private JTable dataPointTable;

    private JFrame parentFrame;

    public ServerPanel(ServerManager serverManager, ServerDataModel dataModel, CaptureManager captureManager) {
        this.serverManager = serverManager;
        this.dataModel = dataModel;
        this.captureManager = captureManager;
        this.config = new ServerConfig();

        // Find parent frame
        try {
            parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        } catch (Exception e) {
            // Will be set later
        }

        setLayout(new BorderLayout());

        add(createControlPanel(), BorderLayout.NORTH);
        add(createTabbedPane(), BorderLayout.CENTER);

        // Setup callbacks
        serverManager.setStateCallback(state -> SwingUtilities.invokeLater(() -> {
            switch (state) {
                case RUNNING:
                    startBtn.setEnabled(false);
                    stopBtn.setEnabled(true);
                    settingsBtn.setEnabled(false);
                    bindField.setEnabled(false);
                    portSpinner.setEnabled(false);
                    loadConfigBtn.setEnabled(false);
                    break;
                case STOPPED:
                case ERROR:
                    startBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    settingsBtn.setEnabled(true);
                    bindField.setEnabled(true);
                    portSpinner.setEnabled(true);
                    loadConfigBtn.setEnabled(true);
                    break;
                default:
                    break;
            }
        }));

        serverManager.setErrorCallback(msg -> SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, msg, "服务端错误", JOptionPane.ERROR_MESSAGE)));

        updateButtonStates();
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("绑定地址:"));
        bindField = new JTextField(config.getBindAddress(), 10);
        panel.add(bindField);
        panel.add(new JLabel("端口:"));
        portSpinner = new JSpinner(new SpinnerNumberModel(config.getPort(), 1, 65535, 1));
        panel.add(portSpinner);
        settingsBtn = new JButton("高级设置");
        settingsBtn.addActionListener(e -> openSettings());
        panel.add(settingsBtn);
        startBtn = new JButton("启动");
        startBtn.addActionListener(e -> startServer());
        panel.add(startBtn);
        stopBtn = new JButton("停止");
        stopBtn.addActionListener(e -> serverManager.stop());
        panel.add(stopBtn);
        saveConfigBtn = new JButton("保存配置");
        saveConfigBtn.addActionListener(e -> saveServerConfig());
        panel.add(saveConfigBtn);
        loadConfigBtn = new JButton("加载配置");
        loadConfigBtn.addActionListener(e -> loadServerConfig());
        panel.add(loadConfigBtn);

        return panel;
    }

    private JTabbedPane createTabbedPane() {
        JTabbedPane tabPane = new JTabbedPane();

        // Tab 1: Data point management
        tabPane.addTab("数据点管理", createDataPointPanel());

        // Tab 2: Packet monitoring
        MessageTablePanel messageTablePanel = new MessageTablePanel(captureManager);
        MessageDetailPanel messageDetailPanel = new MessageDetailPanel();
        messageTablePanel.setSelectionCallback(messageDetailPanel::setPacket);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, messageTablePanel, messageDetailPanel);
        splitPane.setDividerLocation(600);
        splitPane.setResizeWeight(0.6);
        tabPane.addTab("报文监控", splitPane);

        // Tab 3: Connection list
        tabPane.addTab("连接列表", createConnectionPanel());

        return tabPane;
    }

    private JPanel createDataPointPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        tableModel = new DataPointTableModel();
        dataPointTable = new JTable(tableModel);
        dataPointTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        dataPointTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        dataPointTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        dataPointTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        dataPointTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        dataPointTable.getColumnModel().getColumn(5).setPreferredWidth(60);

        JScrollPane scrollPane = new JScrollPane(dataPointTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton addBtn = new JButton("添加");
        addBtn.addActionListener(e -> addDataPoint());
        btnPanel.add(addBtn);

        JButton batchBtn = new JButton("批量生成");
        batchBtn.addActionListener(e -> batchGenerate());
        btnPanel.add(batchBtn);

        JButton editBtn = new JButton("编辑");
        editBtn.addActionListener(e -> editDataPoint());
        btnPanel.add(editBtn);

        JButton delBtn = new JButton("删除");
        delBtn.addActionListener(e -> deleteDataPoint());
        btnPanel.add(delBtn);

        JButton setValueBtn = new JButton("修改值");
        setValueBtn.addActionListener(e -> modifyValue());
        btnPanel.add(setValueBtn);

        JButton clearAllBtn = new JButton("全部清空");
        clearAllBtn.addActionListener(e -> clearAllDataPoints());
        btnPanel.add(clearAllBtn);

        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] cols = {"ID", "客户端地址"};
        Object[][] data = new Object[0][];
        JTable connTable = new JTable(data, cols);
        JScrollPane scrollPane = new JScrollPane(connTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton disconnectBtn = new JButton("断开选中连接");
        disconnectBtn.addActionListener(e -> {
            int row = connTable.getSelectedRow();
            if (row >= 0) {
                int connId = (int) connTable.getValueAt(row, 0);
                serverManager.disconnectClient(connId);
            }
        });
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.add(disconnectBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        // Update connection table via callback
        serverManager.setConnectionListCallback(connIds -> SwingUtilities.invokeLater(() -> {
            java.util.Map<Integer, String> connInfo = serverManager.getConnectionInfoMap();
            Object[][] newData = new Object[connInfo.size()][2];
            int i = 0;
            for (java.util.Map.Entry<Integer, String> entry : connInfo.entrySet()) {
                newData[i][0] = entry.getKey();
                newData[i][1] = entry.getValue();
                i++;
            }
            connTable.setModel(new javax.swing.table.DefaultTableModel(newData, cols) {
                @Override
                public boolean isCellEditable(int row, int col) { return false; }
            });
        }));

        return panel;
    }

    private void openSettings() {
        // Sync basic fields to config
        config.setBindAddress(bindField.getText());
        config.setPort((Integer) portSpinner.getValue());

        ServerConfigDialog dialog = new ServerConfigDialog(getParentFrame(), config.copy());
        dialog.setVisible(true);
        if (dialog.isApproved()) {
            config = dialog.getConfig();
            bindField.setText(config.getBindAddress());
            portSpinner.setValue(config.getPort());
        }
    }

    /**
     * 保存服务端配置到 JSON 文件（.iec104）。
     */
    private void saveServerConfig() {
        // 同步基础字段到配置
        config.setBindAddress(bindField.getText());
        config.setPort((Integer) portSpinner.getValue());

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("保存服务端配置");
        chooser.setFileFilter(new FileNameExtensionFilter("IEC104 场景文件 (*.iec104)", "iec104"));
        chooser.setSelectedFile(new File("server.iec104"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".iec104")) {
            file = new File(file.getParentFile(), file.getName() + ".iec104");
        }

        SceneConfig scene = new SceneConfig(file.getName().replace(".iec104", ""));
        scene.setServerConfig(config.copy());
        scene.stampSaveTime();

        try {
            scene.saveToFile(file);
            JOptionPane.showMessageDialog(this,
                    "服务端配置已保存到\n" + file.getAbsolutePath(),
                    "保存成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 从 JSON 文件（.iec104）加载服务端配置。
     */
    private void loadServerConfig() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("加载服务端配置");
        chooser.setFileFilter(new FileNameExtensionFilter("IEC104 场景文件 (*.iec104)", "iec104"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();

        SceneConfig scene;
        try {
            scene = SceneConfig.loadFromFile(file);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "加载失败: " + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (scene.getServerConfig() == null) {
            JOptionPane.showMessageDialog(this, "文件中未包含服务端配置", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        config = scene.getServerConfig().copy();
        bindField.setText(config.getBindAddress());
        portSpinner.setValue(config.getPort());

        JOptionPane.showMessageDialog(this, "服务端配置加载成功",
                "加载成功", JOptionPane.INFORMATION_MESSAGE);
    }

    private void startServer() {
        config.setBindAddress(bindField.getText());
        config.setPort((Integer) portSpinner.getValue());
        // 根据配置自动生成数据点
        autoGenerateDataPoints();
        serverManager.start(config);
    }

    /**
     * 根据 ServerConfig 中的数据类型配置自动生成数据点。
     * 如果数据模型中已有数据点则不重复生成。
     */
    private void autoGenerateDataPoints() {
        if (dataModel.getPointCount() > 0) return; // 已有数据则不自动生成

        com.openmuc.j60870.ASduType[] types = {
                com.openmuc.j60870.ASduType.M_SP_NA_1,
                com.openmuc.j60870.ASduType.M_DP_NA_1,
                com.openmuc.j60870.ASduType.M_SP_TB_1,
                com.openmuc.j60870.ASduType.M_ME_NA_1,
                com.openmuc.j60870.ASduType.M_ME_NB_1,
                com.openmuc.j60870.ASduType.M_ME_NC_1,
                com.openmuc.j60870.ASduType.M_IT_NA_1,
        };
        String[] prefixes = {"YX", "YX2", "YX_T", "YC", "YC2", "YC3", "DD"};

        for (int i = 0; i < types.length; i++) {
            int start = config.getStartAddress(types[i]);
            int count = config.getCount(types[i]);
            for (int j = 0; j < count; j++) {
                int addr = start + j;
                String name = prefixes[i] + "_" + (j + 1);
                DataPointInfo point = new DataPointInfo(addr, name, types[i], 0);
                point.setQualityOk(true);
                dataModel.addDataPoint(point);
            }
        }
        tableModel.fireTableDataChanged();
    }

    private void addDataPoint() {
        DataPointDialog dialog = new DataPointDialog(getParentFrame(), null);
        dialog.setVisible(true);
        if (dialog.isApproved()) {
            DataPointInfo point = dialog.getDataPoint();
            dataModel.addDataPoint(point);
            tableModel.fireTableDataChanged();
        }
    }

    /**
     * 批量生成数据点
     */
    private void batchGenerate() {
        BatchDataPointDialog dialog = new BatchDataPointDialog(getParentFrame(), dataModel);
        dialog.setVisible(true);
        if (dialog.isApproved()) {
            tableModel.fireTableDataChanged();
        }
    }

    /**
     * 清空所有数据点
     */
    private void clearAllDataPoints() {
        int result = JOptionPane.showConfirmDialog(this,
                "确认清空所有数据点？", "确认", JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) return;
        for (DataPointInfo point : dataModel.getAllPoints()) {
            dataModel.removeDataPoint(point.getAddress());
        }
        tableModel.fireTableDataChanged();
    }

    private void editDataPoint() {
        int row = dataPointTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一个数据点", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<DataPointInfo> points = dataModel.getAllPoints();
        if (row < points.size()) {
            DataPointInfo point = points.get(row);
            DataPointDialog dialog = new DataPointDialog(getParentFrame(), point);
            dialog.setVisible(true);
            if (dialog.isApproved()) {
                DataPointInfo edited = dialog.getDataPoint();
                dataModel.removeDataPoint(point.getAddress());
                dataModel.addDataPoint(edited);
                tableModel.fireTableDataChanged();
            }
        }
    }

    private void deleteDataPoint() {
        int row = dataPointTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一个数据点", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<DataPointInfo> points = dataModel.getAllPoints();
        if (row < points.size()) {
            int addr = points.get(row).getAddress();
            dataModel.removeDataPoint(addr);
            tableModel.fireTableDataChanged();
        }
    }

    private void modifyValue() {
        int row = dataPointTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一个数据点", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<DataPointInfo> points = dataModel.getAllPoints();
        if (row < points.size()) {
            DataPointInfo point = points.get(row);
            String input = JOptionPane.showInputDialog(this, "输入新值:", point.getValueStr());
            if (input != null) {
                try {
                    double val = Double.parseDouble(input.trim());
                    dataModel.updateValue(point.getAddress(), val);
                    tableModel.fireTableDataChanged();
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "无效的数值", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void updateButtonStates() {
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
    }

    private JFrame getParentFrame() {
        if (parentFrame == null) {
            parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        }
        if (parentFrame == null) {
            // Create a dummy frame as fallback
            parentFrame = new JFrame();
        }
        return parentFrame;
    }

    private class DataPointTableModel extends AbstractTableModel {
        private final String[] columns = {"地址", "名称", "类型", "当前值", "品质", "死区"};

        @Override
        public int getRowCount() {
            return dataModel.getPointCount();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int col) {
            return columns[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            List<DataPointInfo> points = dataModel.getAllPoints();
            if (row >= points.size()) return "";
            DataPointInfo point = points.get(row);
            switch (col) {
                case 0: return point.getAddress();
                case 1: return point.getName();
                case 2: return point.getTypeStr();
                case 3: return point.getValueStr();
                case 4: return point.getQualityStr();
                case 5: return point.getDeadLine();
                default: return "";
            }
        }
    }
}
