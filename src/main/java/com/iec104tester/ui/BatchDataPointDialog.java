package com.iec104tester.ui;

import com.iec104tester.core.ServerDataModel;
import com.iec104tester.model.DataPointInfo;
import com.openmuc.j60870.ASduType;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量生成数据点对话框。
 * 按数据类型配置起始地址和数量，一键生成全部数据点。
 */
public class BatchDataPointDialog extends JDialog {

    private boolean approved = false;
    private final BatchTableModel tableModel;
    private final ServerDataModel dataModel;

    public BatchDataPointDialog(JFrame parent, ServerDataModel dataModel) {
        super(parent, "批量生成数据点", true);
        this.dataModel = dataModel;
        this.tableModel = new BatchTableModel();
        initUI();
        setSize(650, 400);
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setLayout(new BorderLayout(5, 5));

        // 说明
        JLabel hint = new JLabel("为每种数据类型配置起始地址和数量，点击\"生成\"将自动创建数据点。");
        hint.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        add(hint, BorderLayout.NORTH);

        // 表格
        JTable table = new JTable(tableModel);
        table.setRowHeight(28);
        // 起始地址列使用 Spinner
        table.getColumnModel().getColumn(1).setCellEditor(new SpinnerCellEditor(0, 0, 16777215, 1));
        // 数量列使用 Spinner
        table.getColumnModel().getColumn(2).setCellEditor(new SpinnerCellEditor(0, 0, 100000, 1));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("数据类型配置"));
        add(scrollPane, BorderLayout.CENTER);

        // 按钮面板
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton clearBtn = new JButton("全部清零");
        clearBtn.addActionListener(e -> tableModel.clearAll());
        btnPanel.add(clearBtn);

        JButton presetBtn = new JButton("默认预设");
        presetBtn.addActionListener(e -> tableModel.applyDefaultPreset());
        btnPanel.add(presetBtn);

        JButton genBtn = new JButton("生成");
        genBtn.addActionListener(e -> {
            generateDataPoints();
            approved = true;
            dispose();
        });
        btnPanel.add(genBtn);

        JButton cancelBtn = new JButton("取消");
        cancelBtn.addActionListener(e -> dispose());
        btnPanel.add(cancelBtn);

        add(btnPanel, BorderLayout.SOUTH);
    }

    /**
     * 根据配置批量生成数据点
     */
    private void generateDataPoints() {
        int totalGenerated = 0;
        for (BatchRow row : tableModel.rows) {
            if (row.count <= 0) continue;
            for (int i = 0; i < row.count; i++) {
                int addr = row.startAddress + i;
                String name = row.namePrefix + "_" + (i + 1);
                double defaultValue = getDefaultForType(row.type);
                DataPointInfo point = new DataPointInfo(addr, name, row.type, defaultValue);
                point.setQualityOk(true);
                dataModel.addDataPoint(point);
                totalGenerated++;
            }
        }
        JOptionPane.showMessageDialog(this,
                "已生成 " + totalGenerated + " 个数据点",
                "完成", JOptionPane.INFORMATION_MESSAGE);
    }

    private double getDefaultForType(ASduType type) {
        switch (type) {
            case M_SP_NA_1:
            case M_SP_TB_1:
            case M_DP_NA_1:
                return 0; // 遥信默认 OFF
            case M_ME_NA_1:
            case M_ME_NB_1:
            case M_ME_NC_1:
                return 0.0; // 遥测默认 0
            case M_IT_NA_1:
                return 0; // 电度默认 0
            default:
                return 0;
        }
    }

    public boolean isApproved() {
        return approved;
    }

    // ===== 表格模型 =====

    private static class BatchRow {
        final String typeName;
        final ASduType type;
        int startAddress;
        int count;
        String namePrefix;

        BatchRow(String typeName, ASduType type, int startAddress, int count, String namePrefix) {
            this.typeName = typeName;
            this.type = type;
            this.startAddress = startAddress;
            this.count = count;
            this.namePrefix = namePrefix;
        }
    }

    private static class BatchTableModel extends AbstractTableModel {
        private final String[] columns = {"数据类型", "起始地址", "数量", "名称前缀"};
        private final List<BatchRow> rows = new ArrayList<>();

        BatchTableModel() {
            // 按数据类型分区排列：遥信 -> 遥测
            rows.add(new BatchRow("M_SP_NA_1 - 单点遥信", ASduType.M_SP_NA_1, 0x0001, 0, "YX"));
            rows.add(new BatchRow("M_DP_NA_1 - 双点遥信", ASduType.M_DP_NA_1, 0x2001, 0, "YX2"));
            rows.add(new BatchRow("M_SP_TB_1 - 带时标单点", ASduType.M_SP_TB_1, 0x3001, 0, "YX_T"));
            rows.add(new BatchRow("M_ME_NA_1 - 归一化遥测", ASduType.M_ME_NA_1, 0x4001, 0, "YC"));
            rows.add(new BatchRow("M_ME_NB_1 - 标度化遥测", ASduType.M_ME_NB_1, 0x4001, 0, "YC2"));
            rows.add(new BatchRow("M_ME_NC_1 - 短浮点遥测", ASduType.M_ME_NC_1, 0x4001, 0, "YC3"));
            rows.add(new BatchRow("M_IT_NA_1 - 电度", ASduType.M_IT_NA_1, 0x4033, 0, "DD"));
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
            for (BatchRow row : rows) {
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
            switch (col) {
                case 1: return Integer.class;
                case 2: return Integer.class;
                default: return String.class;
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col >= 1;
        }

        @Override
        public Object getValueAt(int row, int col) {
            BatchRow r = rows.get(row);
            switch (col) {
                case 0: return r.typeName;
                case 1: return r.startAddress;
                case 2: return r.count;
                case 3: return r.namePrefix;
                default: return "";
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            BatchRow r = rows.get(row);
            switch (col) {
                case 1:
                    if (value instanceof Integer) {
                        r.startAddress = (Integer) value;
                    } else if (value instanceof String) {
                        try { r.startAddress = Integer.parseInt((String) value); } catch (Exception ignored) {}
                    }
                    break;
                case 2:
                    if (value instanceof Integer) {
                        r.count = (Integer) value;
                    } else if (value instanceof String) {
                        try { r.count = Integer.parseInt((String) value); } catch (Exception ignored) {}
                    }
                    break;
                case 3:
                    r.namePrefix = String.valueOf(value);
                    break;
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
