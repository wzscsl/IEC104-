package com.iec104tester.ui;

import com.iec104tester.capture.CaptureManager;
import com.iec104tester.capture.PacketRecord;
import com.iec104tester.capture.PacketStorage;
import com.iec104tester.core.ServerManager;
import com.iec104tester.core.ServerDataModel;
import com.iec104tester.ui.common.Icons;
import com.iec104tester.ui.common.UITheme;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
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
        setTitle("IEC104 协议测试工具 v1.2");
        setSize(1280, 820);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 设置窗口图标（FlatSVGIcon 自动跟随主题）
        try {
            com.formdev.flatlaf.extras.FlatSVGIcon icon =
                    new com.formdev.flatlaf.extras.FlatSVGIcon("icons/app-icon.svg", 256, 256);
            setIconImage(icon.getImage());
        } catch (Exception e) {
            // 图标加载失败不影响启动
        }

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
        tabbedPane.addTab(MODE_CLIENT, Icons.client(), clientPanel);
        tabbedPane.addTab(MODE_SERVER, Icons.server(), new ServerPanel(serverManager, serverDataModel, serverCaptureManager));
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

        // 首次启动引导
        showWelcomeIfFirstLaunch();
    }

    /**
     * 首次启动检测：基于用户目录下的标记文件判断是否首次运行。
     * 首次运行时弹出欢迎对话框并创建标记文件。
     */
    private void showWelcomeIfFirstLaunch() {
        java.io.File marker = new java.io.File(System.getProperty("user.home"),
                ".iec104-tester/.welcome-shown");
        if (marker.exists()) return;

        // 确保目录存在
        java.io.File dir = marker.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }

        String welcome = "<html><div style='width:420px'>"
                + "<h2 style='color:#2563EB;margin:0 0 12px 0'>欢迎使用 IEC104 协议测试工具 v1.2</h2>"
                + "<p style='margin:0 0 8px 0'>本工具基于开源 j60870 库，支持 IEC 60870-5-104 协议的客户端与服务端测试。</p>"
                + "<p style='margin:0 0 8px 0'><b>快速入门：</b></p>"
                + "<ul style='margin:0 0 8px 0;padding-left:20px'>"
                + "<li><b>客户端模式：</b>左侧点击「新建」创建连接，设置 IP/端口后点「连接」</li>"
                + "<li><b>服务端模式：</b>切换到服务端 Tab，配置参数后点「启动」监听端口</li>"
                + "<li><b>报文监控：</b>连接后「报文监控」Tab 实时显示收发的 I/S/U 帧</li>"
                + "<li><b>报文保存：</b>Ctrl+S 保存报文为 JSONL，支持 Ctrl+E 导出 CSV</li>"
                + "</ul>"
                + "<p style='margin:0;color:#6B7280;font-size:11px'>"
                + "快捷键：Ctrl+N 新建会话 | Ctrl+S 保存报文 | Ctrl+O 打开报文</p>"
                + "</div></html>";

        JOptionPane.showMessageDialog(this, welcome,
                "欢迎使用", JOptionPane.PLAIN_MESSAGE);

        // 创建标记文件，下次不再弹出
        try {
            marker.createNewFile();
        } catch (Exception e) {
            // 创建失败不影响使用
        }
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("文件");

        JMenuItem newItem = new JMenuItem("新建会话", Icons.newIcon());
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        newItem.addActionListener(e -> clearActivePackets());
        fileMenu.add(newItem);

        JMenuItem openItem = new JMenuItem("打开报文文件", Icons.load());
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openItem.addActionListener(e -> openPacketFile());
        fileMenu.add(openItem);

        JMenuItem saveItem = new JMenuItem("保存报文", Icons.save());
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> savePackets());
        fileMenu.add(saveItem);

        JMenuItem exportItem = new JMenuItem("导出CSV");
        exportItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
        exportItem.addActionListener(e -> exportCsv());
        fileMenu.add(exportItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
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
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, UITheme.SPACING_MD, UITheme.SPACING_XS));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));

        statusLabel = new JLabel("状态: 未连接");
        packetCountLabel = new JLabel("报文数: 0");
        modeLabel = new JLabel("当前模式: " + MODE_CLIENT);

        // 次级文字使用辅助色，提升视觉层次
        packetCountLabel.setForeground(UITheme.TEXT_SECONDARY);
        modeLabel.setForeground(UITheme.TEXT_SECONDARY);

        statusBar.add(statusLabel);
        statusBar.add(new JSeparator(JSeparator.VERTICAL));
        statusBar.add(packetCountLabel);
        statusBar.add(new JSeparator(JSeparator.VERTICAL));
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
        String statusText;
        if (index == 0) {
            // 客户端模式：显示当前选中连接的状态
            statusText = "状态: " + clientPanel.getActiveStatusText();
            packetCountLabel.setText("报文数: " + clientPanel.getActivePacketCount());
        } else {
            // 服务端模式
            statusText = "状态: " + stateToString(serverManager.getState());
            packetCountLabel.setText("报文数: " + serverCaptureManager.getPacketCount());
        }
        statusLabel.setText(statusText);
        // 根据状态文本应用语义颜色（已连接/运行中=绿；连接中/启动中=橙；错误=红；其余=灰）
        statusLabel.setForeground(UITheme.colorForStatus(statusText));
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
