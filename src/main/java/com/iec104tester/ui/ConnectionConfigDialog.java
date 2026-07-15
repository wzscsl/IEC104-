package com.iec104tester.ui;

import com.iec104tester.model.ConnectionConfig;
import com.iec104tester.model.ConnectionConfig.DataCategory;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 连接配置对话框，用于编辑 IEC 104 客户端连接参数。
 */
public class ConnectionConfigDialog extends JDialog {

    private boolean approved = false;
    private final ConnectionConfig config;

    // 基本设置
    private JTextField hostField;
    private JSpinner portSpinner;
    private JSpinner commonAddressSpinner;
    private JSpinner originatorAddressSpinner;

    // 超时参数
    private JSpinner connectionTimeoutSpinner;
    private JSpinner messageFragmentTimeoutSpinner;
    private JSpinner t1Spinner;
    private JSpinner t2Spinner;
    private JSpinner t3Spinner;

    // 协议参数
    private JSpinner kSpinner;
    private JSpinner wSpinner;

    // 字段长度
    private JSpinner cotFieldLengthSpinner;
    private JSpinner commonAddressFieldLengthSpinner;
    private JSpinner ioaFieldLengthSpinner;

    // IOA 范围配置
    private IoaRangeTableModel ioaRangeModel;
    private JTable ioaRangeTable;

    public ConnectionConfigDialog(JFrame parent, ConnectionConfig config) {
        super(parent, "连接配置", true);
        this.config = config;
        initComponents();
        loadFromConfig();
        setSize(500, 650);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(createBasicPanel());
        content.add(createIoaRangePanel());
        content.add(createTimeoutPanel());
        content.add(createProtocolPanel());
        content.add(createFieldLengthPanel());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okBtn = new JButton("确定");
        JButton cancelBtn = new JButton("取消");
        okBtn.addActionListener(e -> {
            commitTableEditing();
            saveToConfig();
            approved = true;
            dispose();
        });
        cancelBtn.addActionListener(e -> dispose());
        buttonPanel.add(okBtn);
        buttonPanel.add(cancelBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okBtn);
    }

    private JPanel createBasicPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("基本设置"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        hostField = new JTextField(15);
        portSpinner = new JSpinner(new SpinnerNumberModel(2404, 1, 65535, 1));
        commonAddressSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 65535, 1));
        originatorAddressSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));

        addFormRow(panel, gbc, 0, "主机地址:", hostField);
        addFormRow(panel, gbc, 1, "端口:", portSpinner);
        addFormRow(panel, gbc, 2, "公共地址:", commonAddressSpinner);
        addFormRow(panel, gbc, 3, "源发地址:", originatorAddressSpinner);
        return panel;
    }

    private JPanel createIoaRangePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("IOA 范围配置（起始地址 / 数量）"));

        ioaRangeModel = new IoaRangeTableModel();
        ioaRangeTable = new JTable(ioaRangeModel);
        ioaRangeTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        ioaRangeTable.setRowHeight(26);
        ioaRangeTable.getColumnModel().getColumn(1).setCellEditor(new SpinnerCellEditor(0, 0, 16777215, 1));
        ioaRangeTable.getColumnModel().getColumn(2).setCellEditor(new SpinnerCellEditor(0, 0, 100000, 1));

        JScrollPane tableScroll = new JScrollPane(ioaRangeTable);
        tableScroll.setPreferredSize(new Dimension(440, 120));
        panel.add(tableScroll, BorderLayout.CENTER);

        JLabel hint = new JLabel("数量设为 0 表示不限制范围，显示所有接收到的数据");
        hint.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        panel.add(hint, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createTimeoutPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("超时参数(ms)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        connectionTimeoutSpinner = new JSpinner(new SpinnerNumberModel(30000, 100, 600000, 100));
        messageFragmentTimeoutSpinner = new JSpinner(new SpinnerNumberModel(5000, 100, 600000, 100));
        t1Spinner = new JSpinner(new SpinnerNumberModel(15000, 100, 600000, 100));
        t2Spinner = new JSpinner(new SpinnerNumberModel(10000, 100, 600000, 100));
        t3Spinner = new JSpinner(new SpinnerNumberModel(20000, 100, 600000, 100));

        addFormRow(panel, gbc, 0, "连接超时(t0):", connectionTimeoutSpinner);
        addFormRow(panel, gbc, 1, "报文分段超时:", messageFragmentTimeoutSpinner);
        addFormRow(panel, gbc, 2, "t1(未确认超时):", t1Spinner);
        addFormRow(panel, gbc, 3, "t2(确认超时):", t2Spinner);
        addFormRow(panel, gbc, 4, "t3(空闲超时):", t3Spinner);
        return panel;
    }

    private JPanel createProtocolPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("协议参数"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        kSpinner = new JSpinner(new SpinnerNumberModel(12, 1, 32767, 1));
        wSpinner = new JSpinner(new SpinnerNumberModel(8, 1, 32767, 1));

        addFormRow(panel, gbc, 0, "k(最大未确认I帧):", kSpinner);
        addFormRow(panel, gbc, 1, "w(最大未确认接收):", wSpinner);
        return panel;
    }

    private JPanel createFieldLengthPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("字段长度"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        cotFieldLengthSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 2, 1));
        commonAddressFieldLengthSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 2, 1));
        ioaFieldLengthSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 3, 1));

        addFormRow(panel, gbc, 0, "传送原因字段长度(1/2):", cotFieldLengthSpinner);
        addFormRow(panel, gbc, 1, "公共地址字段长度(1/2):", commonAddressFieldLengthSpinner);
        addFormRow(panel, gbc, 2, "IOA字段长度(1/2/3):", ioaFieldLengthSpinner);
        return panel;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        panel.add(comp, gbc);
    }

    private void loadFromConfig() {
        hostField.setText(config.getHost());
        portSpinner.setValue(config.getPort());
        commonAddressSpinner.setValue(config.getCommonAddress());
        originatorAddressSpinner.setValue(config.getOriginatorAddress());
        connectionTimeoutSpinner.setValue(config.getConnectionTimeout());
        messageFragmentTimeoutSpinner.setValue(config.getMessageFragmentTimeout());
        t1Spinner.setValue(config.getT1());
        t2Spinner.setValue(config.getT2());
        t3Spinner.setValue(config.getT3());
        kSpinner.setValue(config.getK());
        wSpinner.setValue(config.getW());
        cotFieldLengthSpinner.setValue(config.getCotFieldLength());
        commonAddressFieldLengthSpinner.setValue(config.getCommonAddressFieldLength());
        ioaFieldLengthSpinner.setValue(config.getIoaFieldLength());
        // 加载 IOA 范围
        for (int i = 0; i < DataCategory.values().length; i++) {
            DataCategory cat = DataCategory.values()[i];
            ioaRangeModel.rows.get(i).startAddress = config.getIoaStart(cat);
            ioaRangeModel.rows.get(i).count = config.getIoaCount(cat);
        }
        ioaRangeModel.fireTableDataChanged();
    }

    private void commitTableEditing() {
        if (ioaRangeTable != null && ioaRangeTable.isEditing()) {
            ioaRangeTable.getCellEditor().stopCellEditing();
        }
    }
    private void saveToConfig() {
        config.setHost(hostField.getText().trim());
        config.setPort((Integer) portSpinner.getValue());
        config.setCommonAddress((Integer) commonAddressSpinner.getValue());
        config.setOriginatorAddress((Integer) originatorAddressSpinner.getValue());
        config.setConnectionTimeout((Integer) connectionTimeoutSpinner.getValue());
        config.setMessageFragmentTimeout((Integer) messageFragmentTimeoutSpinner.getValue());
        config.setT1((Integer) t1Spinner.getValue());
        config.setT2((Integer) t2Spinner.getValue());
        config.setT3((Integer) t3Spinner.getValue());
        config.setK((Integer) kSpinner.getValue());
        config.setW((Integer) wSpinner.getValue());
        config.setCotFieldLength((Integer) cotFieldLengthSpinner.getValue());
        config.setCommonAddressFieldLength((Integer) commonAddressFieldLengthSpinner.getValue());
        config.setIoaFieldLength((Integer) ioaFieldLengthSpinner.getValue());
        // 保存 IOA 范围
        for (int i = 0; i < DataCategory.values().length; i++) {
            IoaRangeRow row = ioaRangeModel.rows.get(i);
            config.setIoaRange(DataCategory.values()[i], row.startAddress, row.count);
        }
    }

    public boolean isApproved() {
        return approved;
    }

    public ConnectionConfig getConfig() {
        return config;
    }

    // ===== IOA 范围配置表模型 =====

    private static class IoaRangeRow {
        final String categoryName;
        int startAddress;
        int count;

        IoaRangeRow(String categoryName, int startAddress, int count) {
            this.categoryName = categoryName;
            this.startAddress = startAddress;
            this.count = count;
        }
    }

    private static class IoaRangeTableModel extends AbstractTableModel {
        private final String[] columns = {"数据类型", "起始地址", "数量"};
        private final List<IoaRangeRow> rows = new ArrayList<>();

        IoaRangeTableModel() {
            for (DataCategory cat : DataCategory.values()) {
                rows.add(new IoaRangeRow(cat.getLabel(), 0, 0));
            }
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
            IoaRangeRow r = rows.get(row);
            switch (col) {
                case 0: return r.categoryName;
                case 1: return r.startAddress;
                case 2: return r.count;
                default: return "";
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            IoaRangeRow r = rows.get(row);
            if (col == 1) {
                if (value instanceof Integer) r.startAddress = (Integer) value;
            } else if (col == 2) {
                if (value instanceof Integer) r.count = (Integer) value;
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
