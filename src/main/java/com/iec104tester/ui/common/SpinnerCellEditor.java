package com.iec104tester.ui.common;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.EventObject;

/**
 * 表格中的 Spinner 编辑器，支持 Integer 数字输入。
 * 原先在 ConnectionConfigDialog / ServerConfigDialog / BatchDataPointDialog 各有一份重复实现。
 */
public class SpinnerCellEditor extends AbstractCellEditor implements TableCellEditor {

    private final JSpinner spinner;

    public SpinnerCellEditor(int value, int min, int max, int step) {
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

    @Override
    public boolean isCellEditable(EventObject e) {
        return true;
    }
}
