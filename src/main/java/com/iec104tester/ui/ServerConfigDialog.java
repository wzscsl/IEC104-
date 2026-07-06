package com.iec104tester.ui;

import com.iec104tester.model.ServerConfig;
import com.openmuc.j60870.ASduType;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务端配置对话框。
 * 包含基本设置、超时参数、协议参数、字段长度、突发上送、数据类型配置。
 */
public class ServerConfigDialog extends JDialog {

    private ServerConfig config;
    private boolean approved = false;

    private JTextField bindAddressField;
    private JSpinner portSpinner;
    private JSpinner maxConnSpinner;
    private JSpinner caSpinner;
    private JSpinner t1Spinner, t2Spinner, t3Spinner;
    private JSpinner kSpinner, wSpinner;
    private JSpinner cotLenSpinner, caLenSpinner, ioaLenSpinner;
    private JCheckBox spontaneousCheck;
    private JSpinner spontaneousIntervalSpinner;
    private DataTypeTableModel dataTypeModel;

    private static final ASduType[] CONFIG_TYPES = {
            ASduType.M_SP_NA_1,
            ASduType.M_DP_NA_1,
            ASduType.M_SP_TB_1,
            ASduType.M_ME_NA_1,
            ASduType.M_ME_NB_1,
            ASduType.M_ME_NC_1,
            ASduType.M_IT_NA_1,
    };
    private static final String[] CONFIG_TYPE_NAMES = {
            "M_SP_NA_1 - 单点遥信",
            "M_DP_NA_1 - 双点遥信",
            "M_SP_TB_1 - 带时标单点",
            "M_ME_NA_1 - 归一化遥测",
            "M_ME_NB_1 - 标度化遥测",
            "M_ME_NC_1 - 短浮点遥测",
            "M_IT_NA_1 - 电度",
    };

    public ServerConfigDialog(JFrame parent, ServerConfig config) {
        super(parent, "服务端配置", true);
        this.config = config;
        initUI();
        loadConfig();
        setSize(520, 620);
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 所有内容放在一个面板中，外层包 JScrollPane
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // === 基本设置 ===
        JPanel basicPanel = new JPanel(new GridBagLayout());
        basicPanel.setBorder(BorderFactory.createTitledBorder("基本设置"));
        GridBagConstraints gbc = createGbc();

        basicPanel.add(new JLabel("绑定地址:"), gbc);
        gbc.gridx++;
        bindAddressField = new JTextField(15);
        basicPanel.add(bindAddressField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        basicPanel.add(new JLabel("端口:"), gbc);
        gbc.gridx++;
        portSpinner = new JSpinner(new SpinnerNumberModel(2404, 1, 65535, 1));
        basicPanel.add(portSpinner, gbc);

        gbc.gridx = 0; gbc.gridy++;
        basicPanel.add(new JLabel("最大连接数:"), gbc);
        gbc.gridx++;
        maxConnSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 10000, 1));
        basicPanel.add(maxConnSpinner, gbc);

        gbc.gridx = 0; gbc.gridy++;
        basicPanel.add(new JLabel("公共地址:"), gbc);
        gbc.gridx++;
        caSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 65535, 1));
        basicPanel.add(caSpinner, gbc);
        contentPanel.add(basicPanel);

        // === 数据类型配置 ===
        JPanel dataTypePanel = new JPanel(new BorderLayout());
        dataTypePanel.setBorder(BorderFactory.createTitledBorder("数据点配置（起始地址 / 数量）"));

        dataTypeModel = new DataTypeTableModel();
        JTable dataTypeTable = new JTable(dataTypeModel);
        dataTypeTable.setRowHeight(26);
        dataTypeTable.getColumnModel().getColumn(1).setCellEditor(new SpinnerCellEditor(0, 0, 16777215, 1));
        dataTypeTable.getColumnModel().getColumn(2).setCellEditor(new SpinnerCellEditor(0, 0, 100000, 1));

        JScrollPane tableScroll = new JScrollPane(dataTypeTable);
        tableScroll.setPreferredSize(new Dimension(480, 180));
        dataTypePanel.add(tableScroll, BorderLayout.CENTER);

        JPanel dataTypeBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton presetBtn = new JButton("默认预设");
        presetBtn.addActionListener(e -> dataTypeModel.applyDefaultPreset());
        dataTypeBtnPanel.add(presetBtn);
        JButton clearBtn = new JButton("全部清零");
        clearBtn.addActionListener(e -> dataTypeModel.clearAll());
        dataTypeBtnPanel.add(clearBtn);
        dataTypePanel.add(dataTypeBtnPanel, BorderLayout.SOUTH);

        contentPanel.add(dataTypePanel);

        // === 超时参数 ===
        JPanel timeoutPanel = new JPanel(new GridBagLayout());
        timeoutPanel.setBorder(BorderFactory.createTitledBorder("超时参数(ms)"));
        gbc = createGbc();

        timeoutPanel.add(new JLabel("t1:"), gbc);
        gbc.gridx++;
        t1Spinner = new JSpinner(new SpinnerNumberModel(15000, 1000, 255000, 1000));
        timeoutPanel.add(t1Spinner, gbc);

        gbc.gridx = 0; gbc.gridy++;
        timeoutPanel.add(new JLabel("t2:"), gbc);
        gbc.gridx++;
        t2Spinner = new JSpinner(new SpinnerNumberModel(10000, 1000, 255000, 1000));
        timeoutPanel.add(t2Spinner, gbc);

        gbc.gridx = 0; gbc.gridy++;
        timeoutPanel.add(new JLabel("t3:"), gbc);
        gbc.gridx++;
        t3Spinner = new JSpinner(new SpinnerNumberModel(20000, 1000, 255000, 1000));
        timeoutPanel.add(t3Spinner, gbc);
        contentPanel.add(timeoutPanel);

        // === 协议参数 ===
        JPanel protoPanel = new JPanel(new GridBagLayout());
        protoPanel.setBorder(BorderFactory.createTitledBorder("协议参数"));
        gbc = createGbc();

        protoPanel.add(new JLabel("k (最大未确认I帧):"), gbc);
        gbc.gridx++;
        kSpinner = new JSpinner(new SpinnerNumberModel(12, 1, 32767, 1));
        protoPanel.add(kSpinner, gbc);

        gbc.gridx = 0; gbc.gridy++;
        protoPanel.add(new JLabel("w (最大未确认接收):"), gbc);
        gbc.gridx++;
        wSpinner = new JSpinner(new SpinnerNumberModel(8, 1, 32767, 1));
        protoPanel.add(wSpinner, gbc);
        contentPanel.add(protoPanel);

        // === 字段长度 ===
        JPanel fieldPanel = new JPanel(new GridBagLayout());
        fieldPanel.setBorder(BorderFactory.createTitledBorder("字段长度"));
        gbc = createGbc();

        fieldPanel.add(new JLabel("COT长度(1-2):"), gbc);
        gbc.gridx++;
        cotLenSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 2, 1));
        fieldPanel.add(cotLenSpinner, gbc);

        gbc.gridx = 0; gbc.gridy++;
        fieldPanel.add(new JLabel("CA长度(1-2):"), gbc);
        gbc.gridx++;
        caLenSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 2, 1));
        fieldPanel.add(caLenSpinner, gbc);

        gbc.gridx = 0; gbc.gridy++;
        fieldPanel.add(new JLabel("IOA长度(1-3):"), gbc);
        gbc.gridx++;
        ioaLenSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 3, 1));
        fieldPanel.add(ioaLenSpinner, gbc);
        contentPanel.add(fieldPanel);

        // === 突发上送 ===
        JPanel spontPanel = new JPanel(new GridBagLayout());
        spontPanel.setBorder(BorderFactory.createTitledBorder("突发上送"));
        gbc = createGbc();

        spontaneousCheck = new JCheckBox("启用突发上送", true);
        spontPanel.add(spontaneousCheck, gbc);

        gbc.gridx = 0; gbc.gridy++;
        spontPanel.add(new JLabel("上送周期(秒):"), gbc);
        gbc.gridx++;
        spontaneousIntervalSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 3600, 1));
        spontPanel.add(spontaneousIntervalSpinner, gbc);
        contentPanel.add(spontPanel);

        // 用 JScrollPane 包裹所有内容
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // === 底部按钮 ===
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okBtn = new JButton("确定");
        okBtn.addActionListener(e -> {
            saveConfig();
            approved = true;
            dispose();
        });
        btnPanel.add(okBtn);
        JButton cancelBtn = new JButton("取消");
        cancelBtn.addActionListener(e -> dispose());
        btnPanel.add(cancelBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void loadConfig() {
        bindAddressField.setText(config.getBindAddress());
        portSpinner.setValue(config.getPort());
        maxConnSpinner.setValue(config.getMaxConnections());
        caSpinner.setValue(config.getCommonAddress());
        t1Spinner.setValue(config.getT1());
        t2Spinner.setValue(config.getT2());
        t3Spinner.setValue(config.getT3());
        kSpinner.setValue(config.getK());
        wSpinner.setValue(config.getW());
        cotLenSpinner.setValue(config.getCotFieldLength());
        caLenSpinner.setValue(config.getCommonAddressFieldLength());
        ioaLenSpinner.setValue(config.getIoaFieldLength());
        spontaneousCheck.setSelected(config.isSpontaneousEnabled());
        spontaneousIntervalSpinner.setValue(config.getSpontaneousInterval());
        // 加载数据类型配置
        for (int i = 0; i < CONFIG_TYPES.length; i++) {
            dataTypeModel.rows.get(i).startAddress = config.getStartAddress(CONFIG_TYPES[i]);
            dataTypeModel.rows.get(i).count = config.getCount(CONFIG_TYPES[i]);
        }
        dataTypeModel.fireTableDataChanged();
    }

    private void saveConfig() {
        config.setBindAddress(bindAddressField.getText());
        config.setPort((Integer) portSpinner.getValue());
        config.setMaxConnections((Integer) maxConnSpinner.getValue());
        config.setCommonAddress((Integer) caSpinner.getValue());
        config.setT1((Integer) t1Spinner.getValue());
        config.setT2((Integer) t2Spinner.getValue());
        config.setT3((Integer) t3Spinner.getValue());
        config.setK((Integer) kSpinner.getValue());
        config.setW((Integer) wSpinner.getValue());
        config.setCotFieldLength((Integer) cotLenSpinner.getValue());
        config.setCommonAddressFieldLength((Integer) caLenSpinner.getValue());
        config.setIoaFieldLength((Integer) ioaLenSpinner.getValue());
        config.setSpontaneousEnabled(spontaneousCheck.isSelected());
        config.setSpontaneousInterval((Integer) spontaneousIntervalSpinner.getValue());
        // 保存数据类型配置
        for (int i = 0; i < CONFIG_TYPES.length; i++) {
            DataTypeRow row = dataTypeModel.rows.get(i);
            config.setTypeConfig(CONFIG_TYPES[i], row.startAddress, row.count);
        }
    }

    private GridBagConstraints createGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 5, 3, 5);
        return gbc;
    }

    public boolean isApproved() {
        return approved;
    }

    public ServerConfig getConfig() {
        return config;
    }

    // ===== 数据类型配置表模型 =====

    private static class DataTypeRow {
        final String typeName;
        int startAddress;
        int count;

        DataTypeRow(String typeName, int startAddress, int count) {
            this.typeName = typeName;
            this.startAddress = startAddress;
            this.count = count;
        }
    }

    private static class DataTypeTableModel extends AbstractTableModel {
        private final String[] columns = {"数据类型", "起始地址", "数量"};
        private final List<DataTypeRow> rows = new ArrayList<>();

        DataTypeTableModel() {
            for (int i = 0; i < CONFIG_TYPE_NAMES.length; i++) {
                rows.add(new DataTypeRow(CONFIG_TYPE_NAMES[i], 0, 0));
            }
        }

        void applyDefaultPreset() {
            int[][] defaults = {
                    {0x0001, 20},   // M_SP_NA_1 单点遥信
                    {0x2001, 10},   // M_DP_NA_1 双点遥信
                    {0x3001, 0},    // M_SP_TB_1 带时标单点
                    {0x4001, 0},    // M_ME_NA_1 归一化遥测
                    {0x4001, 0},    // M_ME_NB_1 标度化遥测
                    {0x4001, 50},   // M_ME_NC_1 短浮点遥测
                    {0x4033, 10},   // M_IT_NA_1 电度
            };
            for (int i = 0; i < rows.size() && i < defaults.length; i++) {
                rows.get(i).startAddress = defaults[i][0];
                rows.get(i).count = defaults[i][1];
            }
            fireTableDataChanged();
        }

        void clearAll() {
            for (DataTypeRow row : rows) {
                row.count = 0;
            }
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
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
        public Class<?> getColumnClass(int col) {
            return (col == 0) ? String.class : Integer.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col >= 1;
        }

        @Override
        public Object getValueAt(int row, int col) {
            DataTypeRow r = rows.get(row);
            switch (col) {
                case 0: return r.typeName;
                case 1: return r.startAddress;
                case 2: return r.count;
                default: return "";
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            DataTypeRow r = rows.get(row);
            if (col == 1) {
                if (value instanceof Integer) r.startAddress = (Integer) value;
                else if (value instanceof String) {
                    try { r.startAddress = Integer.parseInt((String) value); } catch (Exception ignored) {}
                }
            } else if (col == 2) {
                if (value instanceof Integer) r.count = (Integer) value;
                else if (value instanceof String) {
                    try { r.count = Integer.parseInt((String) value); } catch (Exception ignored) {}
                }
            }
            fireTableDataChanged();
        }
    }

    /**
     * 表格中使用 Spinner 的单元格编辑器
     */
    private static class SpinnerCellEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JSpinner spinner;

        SpinnerCellEditor(int value, int min, int max, int step) {
            spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            if (value instanceof Integer) {
                spinner.setValue(value);
            }
            return spinner;
        }
    }
}
