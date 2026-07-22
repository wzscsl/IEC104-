package com.iec104tester.ui;

import com.iec104tester.ui.common.UITheme;
import com.openmuc.j60870.ie.IeDoubleCommand;

import javax.swing.*;
import java.awt.*;

/**
 * 命令发送对话框，支持多种 IEC 104 命令类型的参数输入。
 */
public class CommandDialog extends JDialog {

    public enum CommandType {
        INTERROGATION,
        COUNTER_INTERROGATION,
        CLOCK_SYNC,
        SINGLE_COMMAND,
        DOUBLE_COMMAND,
        SET_SHORT_FLOAT,
        SET_SCALED_VALUE,
        READ,
        TEST,
        RESET_PROCESS
    }

    private boolean approved = false;
    private final CommandType type;

    // 表单组件
    private JSpinner commonAddressSpinner;
    private JSpinner ioaSpinner;
    private JSpinner qualifierSpinner;
    private JSpinner requestSpinner;
    private JSpinner freezeSpinner;
    private JCheckBox onCheckBox;
    private JCheckBox selectCheckBox;
    private JComboBox<IeDoubleCommand.DoubleCommandState> stateComboBox;
    private JTextField valueFloatField;
    private JSpinner valueIntSpinner;
    private JSpinner qlSpinner;

    private CommandDialog(JFrame parent, CommandType type) {
        super(parent, getDialogTitle(type), true);
        this.type = type;
        initComponents();
        setSize(400, 300);
        setLocationRelativeTo(parent);
    }

    private static String getDialogTitle(CommandType type) {
        switch (type) {
            case INTERROGATION: return "总召唤";
            case COUNTER_INTERROGATION: return "电度召唤";
            case CLOCK_SYNC: return "时钟同步";
            case SINGLE_COMMAND: return "单点遥控";
            case DOUBLE_COMMAND: return "双点遥控";
            case SET_SHORT_FLOAT: return "设点(浮点数)";
            case SET_SCALED_VALUE: return "设点(标度值)";
            case READ: return "读命令";
            case TEST: return "测试命令";
            case RESET_PROCESS: return "复位进程";
            default: return "命令";
        }
    }

    /**
     * 显示命令对话框，返回输入值的 Object[] 数组，取消则返回 null。
     */
    public static Object[] show(JFrame parent, CommandType type) {
        CommandDialog dialog = new CommandDialog(parent, type);
        dialog.setVisible(true);
        if (dialog.approved) {
            return dialog.collectValues();
        }
        return null;
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 公共地址 - 所有命令都需要
        commonAddressSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 65535, 1));
        addRow(formPanel, gbc, 0, "公共地址:", commonAddressSpinner);

        int row = 1;
        switch (type) {
            case INTERROGATION:
                qualifierSpinner = new JSpinner(new SpinnerNumberModel(20, 0, 255, 1));
                addRow(formPanel, gbc, row++, "召唤限定词:", qualifierSpinner);
                break;
            case COUNTER_INTERROGATION:
                requestSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 255, 1));
                addRow(formPanel, gbc, row++, "请求类型:", requestSpinner);
                freezeSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 3, 1));
                addRow(formPanel, gbc, row++, "冻结类型:", freezeSpinner);
                break;
            case CLOCK_SYNC:
                // 只需公共地址
                break;
            case SINGLE_COMMAND:
                ioaSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 16777215, 1));
                addRow(formPanel, gbc, row++, "信息对象地址:", ioaSpinner);
                onCheckBox = new JCheckBox("合闸(ON)");
                addRow(formPanel, gbc, row++, "命令状态:", onCheckBox);
                qualifierSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 31, 1));
                addRow(formPanel, gbc, row++, "限定词:", qualifierSpinner);
                selectCheckBox = new JCheckBox("选择(先选择后执行)");
                addRow(formPanel, gbc, row++, "选择模式:", selectCheckBox);
                break;
            case DOUBLE_COMMAND:
                ioaSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 16777215, 1));
                addRow(formPanel, gbc, row++, "信息对象地址:", ioaSpinner);
                stateComboBox = new JComboBox<>(new IeDoubleCommand.DoubleCommandState[]{
                        IeDoubleCommand.DoubleCommandState.OFF,
                        IeDoubleCommand.DoubleCommandState.ON,
                        IeDoubleCommand.DoubleCommandState.NOT_PERMITTED_A,
                        IeDoubleCommand.DoubleCommandState.NOT_PERMITTED_B
                });
                addRow(formPanel, gbc, row++, "命令状态:", stateComboBox);
                qualifierSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 31, 1));
                addRow(formPanel, gbc, row++, "限定词:", qualifierSpinner);
                selectCheckBox = new JCheckBox("选择(先选择后执行)");
                addRow(formPanel, gbc, row++, "选择模式:", selectCheckBox);
                break;
            case SET_SHORT_FLOAT:
                ioaSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 16777215, 1));
                addRow(formPanel, gbc, row++, "信息对象地址:", ioaSpinner);
                valueFloatField = new JTextField("0.0", 12);
                addRow(formPanel, gbc, row++, "设点值(浮点):", valueFloatField);
                qlSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
                addRow(formPanel, gbc, row++, "限定词(QL):", qlSpinner);
                selectCheckBox = new JCheckBox("选择(先选择后执行)");
                addRow(formPanel, gbc, row++, "选择模式:", selectCheckBox);
                break;
            case SET_SCALED_VALUE:
                ioaSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 16777215, 1));
                addRow(formPanel, gbc, row++, "信息对象地址:", ioaSpinner);
                valueIntSpinner = new JSpinner(new SpinnerNumberModel(0, -32768, 32767, 1));
                addRow(formPanel, gbc, row++, "设点值(标度):", valueIntSpinner);
                qlSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
                addRow(formPanel, gbc, row++, "限定词(QL):", qlSpinner);
                selectCheckBox = new JCheckBox("选择(先选择后执行)");
                addRow(formPanel, gbc, row++, "选择模式:", selectCheckBox);
                break;
            case READ:
                ioaSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 16777215, 1));
                addRow(formPanel, gbc, row++, "信息对象地址:", ioaSpinner);
                break;
            case TEST:
                // 只需公共地址
                break;
            case RESET_PROCESS:
                qualifierSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
                addRow(formPanel, gbc, row++, "复位限定词:", qualifierSpinner);
                break;
        }

        add(formPanel, BorderLayout.CENTER);

        // 底部按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okBtn = new JButton("确定");
        JButton cancelBtn = new JButton("取消");
        UITheme.applyPrimaryButton(okBtn);
        okBtn.addActionListener(e -> {
            approved = true;
            dispose();
        });
        cancelBtn.addActionListener(e -> dispose());
        buttonPanel.add(okBtn);
        buttonPanel.add(cancelBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okBtn);
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        panel.add(comp, gbc);
    }

    private Object[] collectValues() {
        int ca = (Integer) commonAddressSpinner.getValue();
        switch (type) {
            case INTERROGATION:
                return new Object[]{ca, (Integer) qualifierSpinner.getValue()};
            case COUNTER_INTERROGATION:
                return new Object[]{ca, (Integer) requestSpinner.getValue(), (Integer) freezeSpinner.getValue()};
            case CLOCK_SYNC:
                return new Object[]{ca};
            case SINGLE_COMMAND:
                return new Object[]{ca, (Integer) ioaSpinner.getValue(), onCheckBox.isSelected(),
                        (Integer) qualifierSpinner.getValue(), selectCheckBox.isSelected()};
            case DOUBLE_COMMAND:
                return new Object[]{ca, (Integer) ioaSpinner.getValue(),
                        stateComboBox.getSelectedItem(),
                        (Integer) qualifierSpinner.getValue(), selectCheckBox.isSelected()};
            case SET_SHORT_FLOAT:
                float fv;
                try {
                    fv = Float.parseFloat(valueFloatField.getText().trim());
                } catch (NumberFormatException e) {
                    fv = 0f;
                }
                return new Object[]{ca, (Integer) ioaSpinner.getValue(), fv,
                        (Integer) qlSpinner.getValue(), selectCheckBox.isSelected()};
            case SET_SCALED_VALUE:
                return new Object[]{ca, (Integer) ioaSpinner.getValue(),
                        (Integer) valueIntSpinner.getValue(),
                        (Integer) qlSpinner.getValue(), selectCheckBox.isSelected()};
            case READ:
                return new Object[]{ca, (Integer) ioaSpinner.getValue()};
            case TEST:
                return new Object[]{ca};
            case RESET_PROCESS:
                return new Object[]{ca, (Integer) qualifierSpinner.getValue()};
            default:
                return null;
        }
    }
}
