package com.iec104tester.ui;

import com.iec104tester.capture.CaptureManager;
import com.iec104tester.capture.PacketRecord;
import com.iec104tester.capture.PacketStorage;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel displaying captured packets in a table with filter and export capabilities.
 */
public class MessageTablePanel extends JPanel {

    private final CaptureManager captureManager;
    private final PacketTableModel tableModel;
    private final JTable table;
    private final List<PacketRecord> displayedPackets = new ArrayList<>();
    private Consumer<PacketRecord> selectionCallback;

    private JCheckBox autoScrollCheck;
    private JCheckBox captureCheck;
    private JButton clearBtn;
    private JButton saveBtn;
    private JButton exportBtn;
    private JButton loadBtn;
    private JLabel countLabel;

    private JComboBox<String> directionFilter;
    private JTextField searchField;

    public MessageTablePanel(CaptureManager captureManager) {
        this.captureManager = captureManager;
        this.tableModel = new PacketTableModel();

        setLayout(new BorderLayout());

        // Toolbar
        JPanel toolbar = createToolbar();
        add(toolbar, BorderLayout.NORTH);

        // Table
        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(90);  // Time
        table.getColumnModel().getColumn(1).setPreferredWidth(40);  // Dir
        table.getColumnModel().getColumn(2).setPreferredWidth(80);  // Frame
        table.getColumnModel().getColumn(3).setPreferredWidth(100); // ASDU Type
        table.getColumnModel().getColumn(4).setPreferredWidth(50);  // TypeID
        table.getColumnModel().getColumn(5).setPreferredWidth(80);  // COT
        table.getColumnModel().getColumn(6).setPreferredWidth(50);  // CA
        table.getColumnModel().getColumn(7).setPreferredWidth(80);  // IOA
        table.getColumnModel().getColumn(8).setPreferredWidth(200); // Summary

        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0 && row < displayedPackets.size() && selectionCallback != null) {
                    selectionCallback.accept(displayedPackets.get(row));
                }
            }
        });

        // Right-click context menu
        JPopupMenu popup = new JPopupMenu();
        JMenuItem copyHexItem = new JMenuItem("复制HEX");
        copyHexItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && row < displayedPackets.size()) {
                String hex = displayedPackets.get(row).getRawHex();
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(hex), null);
            }
        });
        popup.add(copyHexItem);
        table.setComponentPopupMenu(popup);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Filter bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterBar.add(new JLabel("方向:"));
        directionFilter = new JComboBox<>(new String[]{"全部", "收", "发"});
        directionFilter.addActionListener(e -> applyFilter());
        filterBar.add(directionFilter);
        filterBar.add(new JLabel("搜索:"));
        searchField = new JTextField(15);
        searchField.addActionListener(e -> applyFilter());
        filterBar.add(searchField);
        countLabel = new JLabel("0 条");
        filterBar.add(countLabel);
        add(filterBar, BorderLayout.SOUTH);

        // Register callbacks
        captureManager.addPacketCallback(record -> SwingUtilities.invokeLater(() -> {
            displayedPackets.add(record);
            if (matchesFilter(record)) {
                tableModel.fireTableDataChanged();
            }
            updateCount();
            if (autoScrollCheck.isSelected()) {
                scrollToBottom();
            }
        }));

        captureManager.addCountCallback(count -> SwingUtilities.invokeLater(this::updateCount));
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        captureCheck = new JCheckBox("捕获");
        captureCheck.addActionListener(e -> {
            if (captureCheck.isSelected()) {
                captureManager.startCapture();
            } else {
                captureManager.stopCapture();
            }
        });
        toolbar.add(captureCheck);

        autoScrollCheck = new JCheckBox("自动滚动", true);
        toolbar.add(autoScrollCheck);

        clearBtn = new JButton("清空");
        clearBtn.addActionListener(e -> {
            captureManager.clearPackets();
            displayedPackets.clear();
            tableModel.fireTableDataChanged();
            updateCount();
        });
        toolbar.add(clearBtn);

        saveBtn = new JButton("保存");
        saveBtn.addActionListener(e -> savePackets());
        toolbar.add(saveBtn);

        exportBtn = new JButton("导出CSV");
        exportBtn.addActionListener(e -> exportCsv());
        toolbar.add(exportBtn);

        loadBtn = new JButton("加载");
        loadBtn.addActionListener(e -> loadPackets());
        toolbar.add(loadBtn);

        return toolbar;
    }

    public void setSelectionCallback(Consumer<PacketRecord> callback) {
        this.selectionCallback = callback;
    }

    public void startCapture() {
        captureCheck.setSelected(true);
        captureManager.startCapture();
    }

    private boolean matchesFilter(PacketRecord record) {
        // Direction filter
        String dir = (String) directionFilter.getSelectedItem();
        if (dir != null && !dir.equals("全部")) {
            if (!record.getDirectionStr().equals(dir)) return false;
        }
        // Search filter
        String search = searchField.getText().trim().toLowerCase();
        if (!search.isEmpty()) {
            String text = (record.getAsduType() != null ? record.getAsduType().toString() : "") + " " +
                    record.getFrameType() + " " + record.getCauseOfTransmission() + " " +
                    record.getInfoObjectSummary() + " " + record.getRawHex();
            if (!text.toLowerCase().contains(search)) return false;
        }
        return true;
    }

    private void applyFilter() {
        tableModel.fireTableDataChanged();
        updateCount();
    }

    private void updateCount() {
        int total = displayedPackets.size();
        int shown = 0;
        for (PacketRecord r : displayedPackets) {
            if (matchesFilter(r)) shown++;
        }
        countLabel.setText(shown + "/" + total + " 条");
    }

    private void scrollToBottom() {
        int lastRow = table.getRowCount() - 1;
        if (lastRow >= 0) {
            table.scrollRectToVisible(table.getCellRect(lastRow, 0, true));
        }
    }

    private void savePackets() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(PacketStorage.generateFileName()));
        chooser.setFileFilter(new FileNameExtensionFilter("JSON Lines (*.jsonl)", "jsonl"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                PacketStorage.saveToJsonLines(captureManager.getPackets(), chooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "保存成功: " + chooser.getSelectedFile().getName());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "保存失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("iec104_capture.csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV (*.csv)", "csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                PacketStorage.exportToCsv(captureManager.getPackets(), chooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "导出成功: " + chooser.getSelectedFile().getName());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "导出失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadPackets() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("JSON Lines (*.jsonl)", "jsonl"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                List<PacketRecord> loaded = PacketStorage.loadFromJsonLines(chooser.getSelectedFile());
                captureManager.loadPackets(loaded);
                displayedPackets.clear();
                displayedPackets.addAll(loaded);
                tableModel.fireTableDataChanged();
                updateCount();
                JOptionPane.showMessageDialog(this, "加载成功: " + loaded.size() + " 条报文");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "加载失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class PacketTableModel extends AbstractTableModel {
        private final String[] columns = {"时间", "方向", "帧类型", "ASDU类型", "TypeID", "传送原因", "CA", "IOA", "摘要"};

        @Override
        public int getRowCount() {
            int count = 0;
            for (PacketRecord r : displayedPackets) {
                if (matchesFilter(r)) count++;
            }
            return count;
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
            int actualRow = 0;
            for (PacketRecord r : displayedPackets) {
                if (!matchesFilter(r)) continue;
                if (actualRow == row) {
                    switch (col) {
                        case 0: return r.getTimeStr();
                        case 1: return r.getDirectionStr();
                        case 2: return r.getFrameType();
                        case 3: return r.getAsduType() != null ? r.getAsduType().toString() : "";
                        case 4: return r.getTypeId();
                        case 5: return r.getCauseOfTransmission();
                        case 6: return r.getCommonAddress();
                        case 7: return r.getInfoObjectSummary();
                        case 8: return r.getRawHex();
                        default: return "";
                    }
                }
                actualRow++;
            }
            return "";
        }
    }
}
