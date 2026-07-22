package com.iec104tester.ui;

import com.iec104tester.core.ClientManager;
import com.iec104tester.core.ClientSession;
import com.iec104tester.model.ConnectionConfig;
import com.iec104tester.ui.common.Icons;
import com.openmuc.j60870.ASdu;
import com.openmuc.j60870.ASduType;
import com.openmuc.j60870.ie.InformationElement;
import com.openmuc.j60870.ie.InformationObject;
import com.openmuc.j60870.ie.IeDoubleCommand;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 单个连接会话的 UI 面板：报文监控 + 遥信/遥测/遥调/遥控数据视图 + 命令按钮。
 */
public class SessionPanel extends JPanel {

    private final ClientSession session;

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

    public SessionPanel(ClientSession session) {
        this.session = session;
        setLayout(new BorderLayout());
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);
        setupCallbacks();
        updateButtonStates();
    }

    private Component createCenterPanel() {
        JTabbedPane tabPane = new JTabbedPane();

        // 报文监控 tab
        messageTablePanel = new MessageTablePanel(session.getCaptureManager());
        messageDetailPanel = new MessageDetailPanel();
        messageTablePanel.setSelectionCallback(record -> messageDetailPanel.setPacket(record));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, messageTablePanel, messageDetailPanel);
        splitPane.setDividerLocation(600);
        splitPane.setResizeWeight(0.6);
        tabPane.addTab("报文监控", Icons.packet(), splitPane);

        // 遥信
        tabPane.addTab("遥信", Icons.telesignaling(), createDataTab(session.getTelesignalingModel()));

        // 遥测
        tabPane.addTab("遥测", Icons.telemetry(), createDataTab(session.getTelemetryModel()));

        // 遥调
        tabPane.addTab("遥调", Icons.teleadjust(), createDataTab(session.getTeleadjustModel()));

        // 遥控
        tabPane.addTab("遥控", Icons.telecommand(), createDataTab(session.getTelecommandModel()));

        return tabPane;
    }

    private JScrollPane createDataTab(ClientSession.DataTableModel model) {
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
        session.addStateCallback(state -> SwingUtilities.invokeLater(() -> {
            updateButtonStates();
            if (state == ClientManager.ConnectionState.CONNECTED) {
                autoInterrogation();
            }
        }));
        session.addErrorCallback(msg -> SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, msg, "错误", JOptionPane.ERROR_MESSAGE)));
        session.addAsduCallback(asdu -> SwingUtilities.invokeLater(() -> processReceivedAsdu(asdu)));
    }

    private void updateButtonStates() {
        boolean connected = session.getClientManager().isConnected();
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
    }

    private void autoInterrogation() {
        try {
            session.getClientManager().sendInterrogation(session.getConfig().getCommonAddress(), 20);
        } catch (IOException e) {
            // 自动总召唤失败不影响使用
        }
    }

    // ===== 命令发送 =====

    private void sendInterrogation() {
        if (!session.getClientManager().isConnected()) return;
        ConnectionConfig config = session.getConfig();
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
            session.getClientManager().sendInterrogation(ca, qualifier);
        } catch (IOException ex) {
            showError(ex.getMessage());
        }
    }

    private void sendClockSync() {
        if (!session.getClientManager().isConnected()) return;
        ConnectionConfig config = session.getConfig();
        String caStr = JOptionPane.showInputDialog(this, "公共地址:",
                String.valueOf(config.getCommonAddress()));
        if (caStr == null) return;
        int ca = parseIntOrDefault(caStr, config.getCommonAddress());
        try {
            session.getClientManager().sendClockSync(ca);
        } catch (IOException ex) {
            showError(ex.getMessage());
        }
    }

    private void sendCommand(CommandDialog.CommandType type) {
        if (!session.getClientManager().isConnected()) return;
        Object[] values = CommandDialog.show(getParentFrame(), type);
        if (values == null) return;
        try {
            switch (type) {
                case COUNTER_INTERROGATION:
                    session.getClientManager().sendCounterInterrogation((Integer) values[0], (Integer) values[1], (Integer) values[2]);
                    break;
                case SINGLE_COMMAND:
                    session.getClientManager().sendSingleCommand((Integer) values[0], (Integer) values[1],
                            (Boolean) values[2], (Integer) values[3], (Boolean) values[4]);
                    break;
                case DOUBLE_COMMAND:
                    session.getClientManager().sendDoubleCommand((Integer) values[0], (Integer) values[1],
                            (IeDoubleCommand.DoubleCommandState) values[2],
                            (Integer) values[3], (Boolean) values[4]);
                    break;
                case SET_SHORT_FLOAT:
                    session.getClientManager().sendShortFloatCommand((Integer) values[0], (Integer) values[1],
                            (Float) values[2], (Integer) values[3], (Boolean) values[4]);
                    break;
                case SET_SCALED_VALUE:
                    session.getClientManager().sendScaledValueCommand((Integer) values[0], (Integer) values[1],
                            (Integer) values[2], (Integer) values[3], (Boolean) values[4]);
                    break;
                case READ:
                    session.getClientManager().sendReadCommand((Integer) values[0], (Integer) values[1]);
                    break;
                case TEST:
                    session.getClientManager().sendTestCommand((Integer) values[0]);
                    break;
                case RESET_PROCESS:
                    session.getClientManager().sendResetProcessCommand((Integer) values[0], (Integer) values[1]);
                    break;
                default:
                    break;
            }
        } catch (IOException ex) {
            showError(ex.getMessage());
        }
    }

    // ===== 接收数据处理 =====

    private void processReceivedAsdu(ASdu asdu) {
        if (asdu == null || asdu.getInformationObjects() == null) return;

        ASduType type = asdu.getTypeIdentification();
        if (type == null) return;

        ConnectionConfig.DataCategory category = getCategoryForType(type);
        if (category == null) return;

        ClientSession.DataTableModel model = session.getModelForCategory(category);
        if (model == null) return;

        String cot = com.iec104tester.core.AsduDecoder.getCotNameCn(asdu.getCauseOfTransmission());
        String time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());

        for (InformationObject obj : asdu.getInformationObjects()) {
            InformationElement[][] elementSets = obj.getInformationElements();
            if (elementSets == null || elementSets.length == 0) continue;

            for (int i = 0; i < elementSets.length; i++) {
                int ioa = asdu.isSequenceOfElements()
                        ? obj.getInformationObjectAddress() + i
                        : obj.getInformationObjectAddress();
                if (!session.getConfig().isIoaInRange(ioa, category)) continue;

                String typeName = com.iec104tester.core.AsduDecoder.getTypeNameCn(type);
                String value = extractValue(type, elementSets[i]);
                String quality = extractQuality(type, elementSets[i]);
                model.updateOrUpdate(ioa, typeName, value, quality, cot, time);
            }
        }
    }

    private ConnectionConfig.DataCategory getCategoryForType(ASduType type) {
        switch (type) {
            case M_SP_NA_1: case M_DP_NA_1: case M_ST_NA_1: case M_BO_NA_1:
            case M_SP_TB_1: case M_DP_TB_1: case M_ST_TB_1: case M_BO_TB_1:
                return ConnectionConfig.DataCategory.TELESIGNALING;

            case M_ME_NA_1: case M_ME_NB_1: case M_ME_NC_1: case M_ME_ND_1:
            case M_ME_TA_1: case M_ME_TB_1: case M_ME_TC_1:
            case M_ME_TD_1: case M_ME_TE_1: case M_ME_TF_1:
            case M_IT_NA_1: case M_IT_TA_1: case M_IT_TB_1:
                return ConnectionConfig.DataCategory.TELEMETRY;

            case C_SE_NA_1: case C_SE_NB_1: case C_SE_NC_1:
                return ConnectionConfig.DataCategory.TELEADJUST;

            case C_SC_NA_1: case C_DC_NA_1: case C_RC_NA_1:
                return ConnectionConfig.DataCategory.TELECOMMAND;

            default:
                return null;
        }
    }

    private String extractValue(ASduType type, InformationElement[] elements) {
        if (elements == null || elements.length == 0) return "?";
        try {
            InformationElement elem = elements[0];
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

    private String extractQuality(ASduType type, InformationElement[] elements) {
        if (elements == null || elements.length == 0) return "?";
        if (elements.length > 1) {
            try {
                return elements[1].toString();
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

    private JFrame getParentFrame() {
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof JFrame) return (JFrame) w;
        return null;
    }
}
