package com.iec104tester.ui;

import com.iec104tester.capture.CaptureManager;
import com.iec104tester.capture.PacketRecord;
import com.iec104tester.capture.PacketStorage;
import com.iec104tester.core.ServerManager;
import com.iec104tester.core.ServerDataModel;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * IEC104 协议测试工具主窗口。
 */
public class MainFrame extends JFrame {

    private static final String MODE_CLIENT = "客户端模式";
    private static final String MODE_SERVER = "服务端模式";

    private final ServerManager serverManager;
    private final CaptureManager serverCaptureManager;

    private ClientPanel clientPanel;
    private JLabel statusLabel;
    private JLabel packetCountLabel;
    private JLabel modeLabel;
    private JTabbedPane tabbedPane;

    public MainFrame() {
        setTitle("IEC104 协议测试工具");
        setSize(1280, 820);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 服务端共享实例
        serverCaptureManager = new CaptureManager();
        serverManager = new ServerManager();
        ServerDataModel serverDataModel = new ServerDataModel();
        serverManager.setCaptureManager(serverCaptureManager);
        serverManager.setDataModel(serverDataModel);

        // 菜单栏
        setJMenuBar(createMenuBar());

        // 选项卡面板
        tabbedPane = new JTabbedPane();
        clientPanel = new ClientPanel();
        tabbedPane.addTab(MODE_CLIENT, clientPanel);
        tabbedPane.addTab(MODE_SERVER, new ServerPanel(serverManager, serverDataModel, serverCaptureManager));
        add(tabbedPane, BorderLayout.CENTER);

        // 状态栏
        add(createStatusBar(), BorderLayout.SOUTH);

        // 初始化状态栏显示
        updateModeAndStatus();

        // 选项卡切换监听
        tabbedPane.addChangeListener(e -> updateModeAndStatus());

        // 客户端状态变化回调
        clientPanel.setStateChangedCallback(() -> updateConnectionStatus());

        // 服务端错误回调
        serverManager.setErrorCallback(msg -> SwingUtilities.invokeLater(() -> {
            updateConnectionStatus();
            JOptionPane.showMessageDialog(this, msg, "服务端错误", JOptionPane.ERROR_MESSAGE);
        }));

        // 服务端报文计数回调
        serverCaptureManager.addCountCallback(count -> SwingUtilities.invokeLater(() -> {
            if (tabbedPane.getSelectedIndex() == 1) {
                packetCountLabel.setText("报文数: " + count);
            }
        }));

        // 定时刷新状态栏
        Timer statusTimer = new Timer(1000, e -> updateConnectionStatus());
        statusTimer.start();
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("文件");

        JMenuItem newItem = new JMenuItem("新建会话");
        newItem.addActionListener(e -> clearActivePackets());
        fileMenu.add(newItem);

        JMenuItem openItem = new JMenuItem("打开报文文件");
        openItem.addActionListener(e -> openPacketFile());
        fileMenu.add(openItem);

        JMenuItem saveItem = new JMenuItem("保存报文");
        saveItem.addActionListener(e -> savePackets());
        fileMenu.add(saveItem);

        JMenuItem exportItem = new JMenuItem("导出CSV");
        exportItem.addActionListener(e -> exportCsv());
        fileMenu.add(exportItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        JMenu helpMenu = new JMenu("帮助");
        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "IEC104 协议测试工具 v1.0\n基于 j60870 开源库\n支持 IEC 60870-5-104 协议测试\n支持多连接管理",
                "关于", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);

        return menuBar;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        statusLabel = new JLabel("状态: 未连接");
        packetCountLabel = new JLabel("报文数: 0");
        modeLabel = new JLabel("当前模式: " + MODE_CLIENT);

        statusBar.add(statusLabel);
        statusBar.add(new JLabel(" | "));
        statusBar.add(packetCountLabel);
        statusBar.add(new JLabel(" | "));
        statusBar.add(modeLabel);

        return statusBar;
    }

    private void updateModeAndStatus() {
        int index = tabbedPane.getSelectedIndex();
        if (index == 0) {
            modeLabel.setText("当前模式: " + MODE_CLIENT);
        } else {
            modeLabel.setText("当前模式: " + MODE_SERVER);
        }
        updateConnectionStatus();
    }

    private void updateConnectionStatus() {
        int index = tabbedPane.getSelectedIndex();
        if (index == 0) {
            // 客户端模式：显示当前选中连接的状态
            statusLabel.setText("状态: " + clientPanel.getActiveStatusText());
            packetCountLabel.setText("报文数: " + clientPanel.getActivePacketCount());
        } else {
            // 服务端模式
            statusLabel.setText("状态: " + stateToString(serverManager.getState()));
            packetCountLabel.setText("报文数: " + serverCaptureManager.getPacketCount());
        }
    }

    private String stateToString(ServerManager.ServerState state) {
        switch (state) {
            case STOPPED: return "已停止";
            case STARTING: return "启动中";
            case RUNNING: return "运行中";
            case ERROR: return "错误";
            default: return state.toString();
        }
    }

    private CaptureManager getActiveCaptureManager() {
        int index = tabbedPane.getSelectedIndex();
        if (index == 0) {
            return clientPanel.getActiveCaptureManager();
        } else {
            return serverCaptureManager;
        }
    }

    private void clearActivePackets() {
        CaptureManager mgr = getActiveCaptureManager();
        if (mgr != null) mgr.clearPackets();
    }

    private void openPacketFile() {
        CaptureManager mgr = getActiveCaptureManager();
        if (mgr == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("打开报文文件");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON Lines 文件 (*.jsonl)", "jsonl"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        try {
            List<PacketRecord> loaded = PacketStorage.loadFromJsonLines(file);
            mgr.loadPackets(loaded);
            JOptionPane.showMessageDialog(this, "成功加载 " + loaded.size() + " 条报文",
                    "加载成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "加载失败: " + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void savePackets() {
        CaptureManager mgr = getActiveCaptureManager();
        if (mgr == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("保存报文");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON Lines 文件 (*.jsonl)", "jsonl"));
        chooser.setSelectedFile(new File(PacketStorage.generateFileName()));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".jsonl")) {
            file = new File(file.getParentFile(), file.getName() + ".jsonl");
        }
        try {
            PacketStorage.saveToJsonLines(mgr.getPackets(), file);
            JOptionPane.showMessageDialog(this, "成功保存 " + mgr.getPacketCount() + " 条报文",
                    "保存成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCsv() {
        CaptureManager mgr = getActiveCaptureManager();
        if (mgr == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("导出CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV 文件 (*.csv)", "csv"));
        String csvName = PacketStorage.generateFileName().replace(".jsonl", ".csv");
        chooser.setSelectedFile(new File(csvName));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }
        try {
            PacketStorage.exportToCsv(mgr.getPackets(), file);
            JOptionPane.showMessageDialog(this, "成功导出 " + mgr.getPacketCount() + " 条报文",
                    "导出成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}
