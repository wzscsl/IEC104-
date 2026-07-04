package com.iec104tester.ui;

import com.iec104tester.capture.CaptureManager;
import com.iec104tester.capture.PacketRecord;
import com.iec104tester.capture.PacketStorage;
import com.iec104tester.core.ClientManager;
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

    private final CaptureManager captureManager;
    private final ClientManager clientManager;
    private final ServerManager serverManager;

    private JLabel statusLabel;
    private JLabel packetCountLabel;
    private JLabel modeLabel;
    private JTabbedPane tabbedPane;

    public MainFrame() {
        // 1. 窗口基本设置
        setTitle("IEC104 协议测试工具");
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 5. 创建共享实例
        captureManager = new CaptureManager();
        clientManager = new ClientManager();
        serverManager = new ServerManager();
        ServerDataModel serverDataModel = new ServerDataModel();
        clientManager.setCaptureManager(captureManager);
        serverManager.setCaptureManager(captureManager);
        serverManager.setDataModel(serverDataModel);

        // 6. 菜单栏
        setJMenuBar(createMenuBar());

        // 7. 选项卡面板
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab(MODE_CLIENT, new ClientPanel(clientManager, captureManager));
        tabbedPane.addTab(MODE_SERVER, new ServerPanel(serverManager, serverDataModel, captureManager));
        add(tabbedPane, BorderLayout.CENTER);

        // 8. 状态栏
        add(createStatusBar(), BorderLayout.SOUTH);

        // 初始化状态栏显示
        updateModeAndStatus();

        // 选项卡切换监听：更新当前模式和连接状态
        tabbedPane.addChangeListener(e -> updateModeAndStatus());

        // 9. 错误回调
        clientManager.setErrorCallback(msg -> SwingUtilities.invokeLater(() -> {
            updateConnectionStatus();
            JOptionPane.showMessageDialog(this, msg, "客户端错误", JOptionPane.ERROR_MESSAGE);
        }));
        serverManager.setErrorCallback(msg -> SwingUtilities.invokeLater(() -> {
            updateConnectionStatus();
            JOptionPane.showMessageDialog(this, msg, "服务端错误", JOptionPane.ERROR_MESSAGE);
        }));

        // 报文计数回调
        captureManager.addCountCallback(count -> SwingUtilities.invokeLater(() ->
                packetCountLabel.setText("报文数: " + count)));

        // 定时刷新连接状态（避免与子面板的状态回调冲突）
        Timer statusTimer = new Timer(1000, e -> updateConnectionStatus());
        statusTimer.start();
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 文件菜单
        JMenu fileMenu = new JMenu("文件");

        JMenuItem newItem = new JMenuItem("新建会话");
        newItem.addActionListener(e -> captureManager.clearPackets());
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

        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助");
        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "IEC104 协议测试工具 v1.0\n基于 j60870 开源库\n支持 IEC 60870-5-104 协议测试",
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
        String status;
        if (index == 0) {
            status = "状态: " + stateToString(clientManager.getState());
        } else {
            status = "状态: " + stateToString(serverManager.getState());
        }
        statusLabel.setText(status);
    }

    private String stateToString(ClientManager.ConnectionState state) {
        switch (state) {
            case DISCONNECTED: return "未连接";
            case CONNECTING: return "连接中";
            case CONNECTED: return "已连接";
            case ERROR: return "错误";
            default: return state.toString();
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

    private void openPacketFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("打开报文文件");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON Lines 文件 (*.jsonl)", "jsonl"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try {
            List<PacketRecord> loaded = PacketStorage.loadFromJsonLines(file);
            captureManager.loadPackets(loaded);
            JOptionPane.showMessageDialog(this, "成功加载 " + loaded.size() + " 条报文",
                    "加载成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "加载失败: " + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void savePackets() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("保存报文");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON Lines 文件 (*.jsonl)", "jsonl"));
        chooser.setSelectedFile(new File(PacketStorage.generateFileName()));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".jsonl")) {
            file = new File(file.getParentFile(), file.getName() + ".jsonl");
        }
        try {
            PacketStorage.saveToJsonLines(captureManager.getPackets(), file);
            JOptionPane.showMessageDialog(this, "成功保存 " + captureManager.getPacketCount() + " 条报文",
                    "保存成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("导出CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV 文件 (*.csv)", "csv"));
        String csvName = PacketStorage.generateFileName().replace(".jsonl", ".csv");
        chooser.setSelectedFile(new File(csvName));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }
        try {
            PacketStorage.exportToCsv(captureManager.getPackets(), file);
            JOptionPane.showMessageDialog(this, "成功导出 " + captureManager.getPacketCount() + " 条报文",
                    "导出成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}
