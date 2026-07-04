package com.iec104tester.ui;

import com.iec104tester.capture.CaptureManager;
import com.iec104tester.core.ClientManager;
import com.iec104tester.model.ConnectionConfig;
import com.iec104tester.model.ConnectionConfig.DataCategory;
import com.openmuc.j60870.ASdu;
import com.openmuc.j60870.ASduType;
import com.openmuc.j60870.ie.InformationElement;
import com.openmuc.j60870.ie.InformationObject;
import com.openmuc.j60870.ie.IeDoubleCommand;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IEC 104 测试工具的客户端模式面板。
 * 顶部为连接控制，中部为报文监控与数据视图（遥信/遥测/遥调/遥控），底部为命令按钮。
 */
public class ClientPanel extends JPanel {

    private final ClientManager clientManager;
    private final CaptureManager captureManager;
    private ConnectionConfig config;

    // 顶部连接控件
    private JTextField hostField;
    private JSpinner portSpinner;
    private JButton settingsBtn;
    private JButton connectBtn;
    private JButton disconnectBtn;
    private JLabel statusLabel;

    // 底部命令按钮
    private JButton interrogationBtn;
    private JButton counterBtn;
    private JButton clockSyncBtn;
    private JButton singleCmdBtn;
    private JButton doubleCmdBtn;
    private JButton setFloatBtn;
    private JButton setScaledBtn;
    private JButton readBtn;
    private JButton testBtn;
    private JButton resetProcessBtn;

    private MessageTablePanel messageTablePanel;
    private MessageDetailPanel messageDetailPanel;

    // 四类数据视图模型
    private DataTableModel telesignalingModel;   // 遥信
    private DataTableModel telemetryModel;        // 遥测
    private DataTableModel teleadjustModel;       // 遥调（设点）
    private DataTableModel telecommandModel;      // 遥控（命令确认）

    public ClientPanel(ClientManager clientManager, CaptureManager captureManager) {
        this.clientManager = clientManager;
        this.captureManager = captureManager;
        this.config = new ConnectionConfig();

        setLayout(new BorderLayout());

        add(createTopPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);

        setupCallbacks();
        updateButtonStates(ClientManager.ConnectionState.DISCONNECTED);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        panel.add(new JLabel("主机:"));
        hostField = new JTextField(config.getHost(), 12);
        panel.add(hostField);

        panel.add(new JLabel("端口:"));
        portSpinner = new JSpinner(new SpinnerNumberModel(config.getPort(), 1, 65535, 1));
        panel.add(portSpinner);

        settingsBtn = new JButton("高级设置");
        settingsBtn.addActionListener(e -> openSettings());
        panel.add(settingsBtn);

        connectBtn = new JButton("连接");
        connectBtn.addActionListener(e -> doConnect());
        panel.add(connectBtn);

        disconnectBtn = new JButton("断开");
        disconnectBtn.addActionListener(e -> clientManager.disconnect());
        panel.add(disconnectBtn);

        statusLabel = new JLabel("未连接");
        panel.add(statusLabel);

        return panel;
    }

    private Component createCenterPanel() {
        JTabbedPane tabPane = new JTabbedPane();

        // 报文监控 tab
        messageTablePanel = new MessageTablePanel(captureManager);
        messageDetailPanel = new MessageDetailPanel();
        messageTablePanel.setSelectionCallback(record -> messageDetailPanel.setPacket(record));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, messageTablePanel, messageDetailPanel);
        splitPane.setDividerLocation(600);
        splitPane.setResizeWeight(0.6);
        tabPane.addTab("报文监控", splitPane);

        // 遥信数据 tab (单点/双点/带时标)
        telesignalingModel = new DataTableModel();
        tabPane.addTab("遥信", createDataTab(telesignalingModel));

        // 遥测数据 tab (归一化/标度化/短浮点)
        telemetryModel = new DataTableModel();
        tabPane.addTab("遥测", createDataTab(telemetryModel));

        // 遥调数据 tab (设点命令)
        teleadjustModel = new DataTableModel();
        tabPane.addTab("遥调", createDataTab(teleadjustModel));

        // 遥控数据 tab (单点/双点命令)
        telecommandModel = new DataTableModel();
        tabPane.addTab("遥控", createDataTab(telecommandModel));

        return tabPane;
    }

    private JScrollPane createDataTab(DataTableModel model) {
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(22);
        return new JScrollPane(table);
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 5, 5, 5));

        interrogationBtn = new JButton("总召唤");
        interrogationBtn.addActionListener(e -> sendInterrogation());
        counterBtn = new JButton("电度召唤");
        counterBtn.addActionListener(e -> sendCommand(CommandDialog.CommandType.COUNTER_INTERROGATION));
        clockSyncBtn = new JButton("时钟同步");
        clockSyncBtn.addActionListener(e -> sendClockSync());
        singleCmdBtn = new JButton("单点遥控");
        singleCmdBtn.addActionListener(e -> sendCommand(CommandDialog.CommandType.SINGLE_COMMAND));
        doubleCmdBtn = new JButton("双点遥控");
        doubleCmdBtn.addActionListener(e -> sendCommand(CommandDialog.CommandType.DOUBLE_COMMAND));
        setFloatBtn = new JButton("设点(浮点)");
        setFloatBtn.addActionListener(e -> sendCommand(CommandDialog.CommandType.SET_SHORT_FLOAT));
        setScaledBtn = new JButton("设点(标度)");
        setScaledBtn.addActionListener(e -> sendCommand(CommandDialog.CommandType.SET_SCALED_VALUE));
        readBtn = new JButton("读命令");
        readBtn.addActionListener(e -> sendCommand(CommandDialog.CommandType.READ));
        testBtn = new JButton("测试命令");
        testBtn.addActionListener(e -> sendCommand(CommandDialog.CommandType.TEST));
        resetProcessBtn = new JButton("复位进程");
        resetProcessBtn.addActionListener(e -> sendCommand(CommandDialog.CommandType.RESET_PROCESS));

        panel.add(interrogationBtn);
        panel.add(counterBtn);
        panel.add(clockSyncBtn);
        panel.add(singleCmdBtn);
        panel.add(doubleCmdBtn);
        panel.add(setFloatBtn);
        panel.add(setScaledBtn);
        panel.add(readBtn);
        panel.add(testBtn);
        panel.add(resetProcessBtn);

        return panel;
    }

    private void setupCallbacks() {
        clientManager.setStateCallback(state -> SwingUtilities.invokeLater(() -> updateButtonStates(state)));
        clientManager.setErrorCallback(msg -> SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, msg, "错误", JOptionPane.ERROR_MESSAGE)));
        clientManager.setAsduCallback(asdu -> SwingUtilities.invokeLater(() -> processReceivedAsdu(asdu)));
    }

    private void updateButtonStates(ClientManager.ConnectionState state) {
        boolean connected = (state == ClientManager.ConnectionState.CONNECTED);
        boolean canConnect = (state == ClientManager.ConnectionState.DISCONNECTED
                || state == ClientManager.ConnectionState.ERROR);
        connectBtn.setEnabled(canConnect);
        disconnectBtn.setEnabled(connected);
        settingsBtn.setEnabled(canConnect);
        hostField.setEnabled(canConnect);
        portSpinner.setEnabled(canConnect);

        interrogationBtn.setEnabled(connected);
        counterBtn.setEnabled(connected);
        clockSyncBtn.setEnabled(connected);
        singleCmdBtn.setEnabled(connected);
        doubleCmdBtn.setEnabled(connected);
        setFloatBtn.setEnabled(connected);
        setScaledBtn.setEnabled(connected);
        readBtn.setEnabled(connected);
        testBtn.setEnabled(connected);
        resetProcessBtn.setEnabled(connected);

        switch (state) {
            case DISCONNECTED:
                statusLabel.setText("未连接");
                break;
            case CONNECTING:
                statusLabel.setText("连接中...");
                break;
            case CONNECTED:
                statusLabel.setText("已连接");
                autoInterrogation();
                break;
            case ERROR:
                statusLabel.setText("连接错误");
                break;
        }
    }

    private void autoInterrogation() {
        try {
            clientManager.sendInterrogation(config.getCommonAddress(), 20);
        } catch (IOException e) {
            // 自动总召唤失败不影响使用
        }
    }

    private void syncConfigFromFields() {
        config.setHost(hostField.getText().trim());
        config.setPort((Integer) portSpinner.getValue());
    }

    private void doConnect() {
        syncConfigFromFields();
        captureManager.startCapture();
        // 清空旧数据
        telesignalingModel.clear();
        telemetryModel.clear();
        teleadjustModel.clear();
        telecommandModel.clear();
        clientManager.connect(config);
    }

    private void openSettings() {
        syncConfigFromFields();
        ConnectionConfig copy = config.copy();
        ConnectionConfigDialog dialog = new ConnectionConfigDialog(getParentFrame(), copy);
        dialog.setVisible(true);
        if (dialog.isApproved()) {
            this.config = dialog.getConfig();
            hostField.setText(config.getHost());
            portSpinner.setValue(config.getPort());
        }
    }

    private JFrame getParentFrame() {
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof JFrame) {
            return (JFrame) w;
        }
        return null;
    }

    // ===== 命令发送 =====

    private void sendInterrogation() {
        if (!clientManager.isConnected()) return;
        JPanel form = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField caField = new JTextField(String.valueOf(config.getCommonAddress()));
        JTextField qualField = new JTextField("20");
        form.add(new JLabel("公共地址:"));
        form.add(caField);
        form.add(new JLabel("召唤限定词:"));
        form.add(qualField);
        int result = JOptionPane.showConfirmDialog(this, form, "总召唤", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;
        int ca = parseIntOrDefault(caField.getText(), config.getCommonAddress());
        int qualifier = parseIntOrDefault(qualField.getText(), 20);
        try {
            clientManager.sendInterrogation(ca, qualifier);
        } catch (IOException ex) {
            showError(ex.getMessage());
        }
    }

    private void sendClockSync() {
        if (!clientManager.isConnected()) return;
        String caStr = JOptionPane.showInputDialog(this, "公共地址:",
                String.valueOf(config.getCommonAddress()));
        if (caStr == null) return;
        int ca = parseIntOrDefault(caStr, config.getCommonAddress());
        try {
            clientManager.sendClockSync(ca);
        } catch (IOException ex) {
            showError(ex.getMessage());
        }
    }

    private void sendCommand(CommandDialog.CommandType type) {
        if (!clientManager.isConnected()) return;
        Object[] values = CommandDialog.show(getParentFrame(), type);
        if (values == null) return;
        try {
            switch (type) {
                case COUNTER_INTERROGATION:
                    clientManager.sendCounterInterrogation((Integer) values[0], (Integer) values[1], (Integer) values[2]);
                    break;
                case SINGLE_COMMAND:
                    clientManager.sendSingleCommand((Integer) values[0], (Integer) values[1],
                            (Boolean) values[2], (Integer) values[3], (Boolean) values[4]);
                    break;
                case DOUBLE_COMMAND:
                    clientManager.sendDoubleCommand((Integer) values[0], (Integer) values[1],
                            (IeDoubleCommand.DoubleCommandState) values[2],
                            (Integer) values[3], (Boolean) values[4]);
                    break;
                case SET_SHORT_FLOAT:
                    clientManager.sendShortFloatCommand((Integer) values[0], (Integer) values[1],
                            (Float) values[2], (Integer) values[3], (Boolean) values[4]);
                    break;
                case SET_SCALED_VALUE:
                    clientManager.sendScaledValueCommand((Integer) values[0], (Integer) values[1],
                            (Integer) values[2], (Integer) values[3], (Boolean) values[4]);
                    break;
                case READ:
                    clientManager.sendReadCommand((Integer) values[0], (Integer) values[1]);
                    break;
                case TEST:
                    clientManager.sendTestCommand((Integer) values[0]);
                    break;
                case RESET_PROCESS:
                    clientManager.sendResetProcessCommand((Integer) values[0], (Integer) values[1]);
                    break;
                default:
                    break;
            }
        } catch (IOException ex) {
            showError(ex.getMessage());
        }
    }

    // ===== 接收数据处理 =====

    /**
     * 处理接收到的 ASDU，按数据类型分发到对应的数据视图
     */
    private void processReceivedAsdu(ASdu asdu) {
        if (asdu == null || asdu.getInformationObjects() == null) return;

        ASduType type = asdu.getTypeIdentification();
        if (type == null) return;

        DataCategory category = getCategoryForType(type);
        if (category == null) return;

        DataTableModel model = getModelForCategory(category);
        if (model == null) return;

        String cot = com.iec104tester.core.AsduDecoder.getCotNameCn(asdu.getCauseOfTransmission());
        String time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());

        for (InformationObject obj : asdu.getInformationObjects()) {
            int ioa = obj.getInformationObjectAddress();
            // 按 IOA 范围过滤
            if (!config.isIoaInRange(ioa, category)) continue;

            String typeName = com.iec104tester.core.AsduDecoder.getTypeNameCn(type);
            String value = extractValue(type, obj);
            String quality = extractQuality(type, obj);
            model.updateOrUpdate(ioa, typeName, value, quality, cot, time);
        }
    }

    /**
     * 根据 ASDU 类型获取数据类别
     */
    private DataCategory getCategoryForType(ASduType type) {
        switch (type) {
            case M_SP_NA_1: case M_DP_NA_1: case M_ST_NA_1: case M_BO_NA_1:
            case M_SP_TB_1: case M_DP_TB_1: case M_ST_TB_1: case M_BO_TB_1:
                return DataCategory.TELESIGNALING;

            case M_ME_NA_1: case M_ME_NB_1: case M_ME_NC_1:
            case M_ME_TD_1: case M_ME_TE_1: case M_ME_TF_1:
            case M_IT_NA_1: case M_IT_TB_1:
                return DataCategory.TELEMETRY;

            case C_SE_NA_1: case C_SE_NB_1: case C_SE_NC_1:
                return DataCategory.TELEADJUST;

            case C_SC_NA_1: case C_DC_NA_1: case C_RC_NA_1:
                return DataCategory.TELECOMMAND;

            default:
                return null;
        }
    }

    /**
     * 根据数据类别获取对应的数据模型
     */
    private DataTableModel getModelForCategory(DataCategory category) {
        switch (category) {
            case TELESIGNALING: return telesignalingModel;
            case TELEMETRY: return telemetryModel;
            case TELEADJUST: return teleadjustModel;
            case TELECOMMAND: return telecommandModel;
            default: return null;
        }
    }

    private String extractValue(ASduType type, InformationObject obj) {
        InformationElement[][] elements = obj.getInformationElements();
        if (elements == null || elements.length == 0 || elements[0].length == 0) {
            return "?";
        }
        try {
            InformationElement elem = elements[0][0];
            if (elem == null) return "?";
            String str = elem.toString();
            if (str.contains(":")) {
                return str.substring(str.indexOf(":") + 1).trim();
            }
            return str;
        } catch (Exception e) {
            return "?";
        }
    }

    private String extractQuality(ASduType type, InformationObject obj) {
        InformationElement[][] elements = obj.getInformationElements();
        if (elements == null || elements.length == 0) return "?";
        if (elements[0].length > 1) {
            try {
                return elements[0][1].toString();
            } catch (Exception e) {
                return "?";
            }
        }
        return "-";
    }

    private int parseIntOrDefault(String s, int defaultValue) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, "发送失败: " + msg, "错误", JOptionPane.ERROR_MESSAGE);
    }

    // ===== 通用数据表模型 =====

    private static class DataTableModel extends AbstractTableModel {
        private final String[] columns = {"IOA", "类型", "值", "品质", "传送原因", "更新时间"};
        private final Map<Integer, Object[]> rowData = new ConcurrentHashMap<>();
        private final List<Integer> ioaList = new ArrayList<>();

        @Override
        public int getRowCount() {
            return ioaList.size();
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
            if (row >= ioaList.size()) return "";
            Integer ioa = ioaList.get(row);
            Object[] data = rowData.get(ioa);
            if (data == null) return "";
            return data[col];
        }

        void updateOrUpdate(int ioa, String type, String value, String quality, String cot, String time) {
            if (!rowData.containsKey(ioa)) {
                ioaList.add(ioa);
                Collections.sort(ioaList);
            }
            rowData.put(ioa, new Object[]{ioa, type, value, quality, cot, time});
            fireTableDataChanged();
        }

        void clear() {
            rowData.clear();
            ioaList.clear();
            fireTableDataChanged();
        }
    }
}
