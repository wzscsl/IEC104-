package com.iec104tester.ui;

import com.iec104tester.model.DataPointInfo;
import com.openmuc.j60870.ASduType;

import javax.swing.*;
import java.awt.*;

/**
 * 数据点添加/编辑对话框
 */
public class DataPointDialog extends JDialog {

    private DataPointInfo dataPoint;
    private boolean approved = false;

    private JSpinner addressSpinner;
    private JTextField nameField;
    private JComboBox<String> typeCombo;
    private JTextField valueField;
    private JCheckBox qualityCheck;
    private JTextField deadLineField;
    private JSpinner scaleIndexSpinner;

    private static final String[] TYPE_NAMES = {
            "M_SP_NA_1 - 单点信息",
            "M_DP_NA_1 - 双点信息",
            "M_ME_NA_1 - 归一化测量值",
            "M_ME_NB_1 - 标度化测量值",
            "M_ME_NC_1 - 短浮点测量值",
            "M_IT_NA_1 - 电度",
            "M_SP_TB_1 - 带时标单点信息"
    };
    private static final ASduType[] TYPE_VALUES = {
            ASduType.M_SP_NA_1,
            ASduType.M_DP_NA_1,
            ASduType.M_ME_NA_1,
            ASduType.M_ME_NB_1,
            ASduType.M_ME_NC_1,
            ASduType.M_IT_NA_1,
            ASduType.M_SP_TB_1
    };

    public DataPointDialog(JFrame parent, DataPointInfo point) {
        super(parent, point == null ? "添加数据点" : "编辑数据点", true);
        this.dataPoint = point == null ? new DataPointInfo() : point;
        initUI();
        loadFromPoint();
        setSize(400, 300);
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Address
        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("地址:"), gbc);
        gbc.gridx = 1;
        addressSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 16777215, 1));
        add(addressSpinner, gbc);

        // Name
        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("名称:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(15);
        add(nameField, gbc);

        // Type
        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("类型:"), gbc);
        gbc.gridx = 1;
        typeCombo = new JComboBox<>(TYPE_NAMES);
        add(typeCombo, gbc);

        // Value
        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("当前值:"), gbc);
        gbc.gridx = 1;
        valueField = new JTextField("0", 15);
        add(valueField, gbc);

        // Quality
        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("品质:"), gbc);
        gbc.gridx = 1;
        qualityCheck = new JCheckBox("正常", true);
        add(qualityCheck, gbc);

        // DeadLine
        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("死区:"), gbc);
        gbc.gridx = 1;
        deadLineField = new JTextField("0", 15);
        add(deadLineField, gbc);

        // Scale
        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("标度系数:"), gbc);
        gbc.gridx = 1;
        scaleIndexSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        add(scaleIndexSpinner, gbc);

        // Buttons
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okBtn = new JButton("确定");
        okBtn.addActionListener(e -> {
            if (saveToPoint()) {
                approved = true;
                dispose();
            }
        });
        btnPanel.add(okBtn);
        JButton cancelBtn = new JButton("取消");
        cancelBtn.addActionListener(e -> dispose());
        btnPanel.add(cancelBtn);
        add(btnPanel, gbc);
    }

    private void loadFromPoint() {
        if (dataPoint.getAddress() > 0) {
            addressSpinner.setValue(dataPoint.getAddress());
        }
        if (dataPoint.getName() != null) {
            nameField.setText(dataPoint.getName());
        }
        if (dataPoint.getAsduType() != null) {
            for (int i = 0; i < TYPE_VALUES.length; i++) {
                if (TYPE_VALUES[i] == dataPoint.getAsduType()) {
                    typeCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        valueField.setText(dataPoint.getValueStr());
        qualityCheck.setSelected(dataPoint.isQualityOk());
        deadLineField.setText(String.valueOf(dataPoint.getDeadLine()));
        scaleIndexSpinner.setValue(dataPoint.getScaleIndex());
    }

    private boolean saveToPoint() {
        try {
            dataPoint.setAddress((Integer) addressSpinner.getValue());
            dataPoint.setName(nameField.getText().trim());
            dataPoint.setAsduType(TYPE_VALUES[typeCombo.getSelectedIndex()]);
            dataPoint.setCurrentValue(Double.parseDouble(valueField.getText().trim()));
            dataPoint.setQualityOk(qualityCheck.isSelected());
            dataPoint.setDeadLine(Double.parseDouble(deadLineField.getText().trim()));
            dataPoint.setScaleIndex((Integer) scaleIndexSpinner.getValue());
            return true;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "数值格式错误: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public boolean isApproved() {
        return approved;
    }

    public DataPointInfo getDataPoint() {
        return dataPoint;
    }
}
