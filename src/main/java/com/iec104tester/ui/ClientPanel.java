package com.iec104tester.ui;

import com.iec104tester.core.ClientManager;
import com.iec104tester.core.ClientSession;
import com.iec104tester.model.ConnectionConfig;
import com.iec104tester.model.SceneConfig;
import com.iec104tester.ui.common.Icons;
import com.iec104tester.ui.common.UITheme;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户端模式面板 —— 支持多连接管理（类似 Navicat）。
 * 左侧为连接列表，右侧为选中连接的报文监控与数据视图。
 */
public class ClientPanel extends JPanel {

    private final List<ClientSession> sessions = new ArrayList<>();
    private int sessionCounter = 0;

    // 左侧连接列表
    private JList<ClientSession> sessionList;
    private DefaultListModel<ClientSession> listModel;
    private JButton newBtn;
    private JButton connectBtn;
    private JButton disconnectBtn;
    private JButton settingsBtn;
    private JButton deleteBtn;
    private JButton saveSceneBtn;
    private JButton loadSceneBtn;

    // 右侧内容区
    private JPanel contentPanel;
    private CardLayout cardLayout;

    // 状态回调（通知 MainFrame）
    private Runnable stateChangedCallback;

    public ClientPanel() {
        setLayout(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createSidebar(), createContentArea());
        splitPane.setDividerLocation(220);
        splitPane.setResizeWeight(0.0);

        add(splitPane, BorderLayout.CENTER);

        // 创建默认连接
        addNewSession();
    }

    // ===== 左侧边栏 =====

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBorder(new TitledBorder("连接列表"));
        sidebar.setMinimumSize(new Dimension(200, 0));
        sidebar.setPreferredSize(new Dimension(220, 0));

        // 连接列表
        listModel = new DefaultListModel<>();
        sessionList = new JList<>(listModel);
        sessionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sessionList.setCellRenderer(new SessionListCellRenderer());
        sessionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                switchSession(sessionList.getSelectedIndex());
            }
        });
        sidebar.add(new JScrollPane(sessionList), BorderLayout.CENTER);

        // 右键菜单
        JPopupMenu popup = new JPopupMenu();
        JMenuItem connectItem = new JMenuItem("连接");
        connectItem.addActionListener(e -> doConnect());
        JMenuItem disconnectItem = new JMenuItem("断开");
        disconnectItem.addActionListener(e -> doDisconnect());
        JMenuItem settingsItem = new JMenuItem("设置");
        settingsItem.addActionListener(e -> openSettings());
        JMenuItem deleteItem = new JMenuItem("删除");
        deleteItem.addActionListener(e -> deleteSession());
        popup.add(connectItem);
        popup.add(disconnectItem);
        popup.addSeparator();
        popup.add(settingsItem);
        popup.add(deleteItem);
        sessionList.setComponentPopupMenu(popup);

        // 按钮区
        JPanel buttonPanel = new JPanel(new GridLayout(4, 2, UITheme.SPACING_SM, UITheme.SPACING_SM));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(
                UITheme.SPACING_SM, UITheme.SPACING_SM, UITheme.SPACING_SM, UITheme.SPACING_SM));
        newBtn = new JButton("新建");
        newBtn.setIcon(Icons.newIcon());
        newBtn.addActionListener(e -> addNewSession());
        connectBtn = new JButton("连接");
        connectBtn.setIcon(Icons.connect());
        connectBtn.addActionListener(e -> doConnect());
        UITheme.applyPrimaryButton(connectBtn);
        disconnectBtn = new JButton("断开");
        disconnectBtn.setIcon(Icons.disconnect());
        disconnectBtn.addActionListener(e -> doDisconnect());
        settingsBtn = new JButton("设置");
        settingsBtn.setIcon(Icons.settings());
        settingsBtn.addActionListener(e -> openSettings());
        deleteBtn = new JButton("删除");
        deleteBtn.setIcon(Icons.delete());
        deleteBtn.addActionListener(e -> deleteSession());
        saveSceneBtn = new JButton("保存场景");
        saveSceneBtn.setIcon(Icons.save());
        saveSceneBtn.addActionListener(e -> saveScene());
        loadSceneBtn = new JButton("加载场景");
        loadSceneBtn.setIcon(Icons.load());
        loadSceneBtn.addActionListener(e -> loadScene());

        buttonPanel.add(newBtn);
        buttonPanel.add(connectBtn);
        buttonPanel.add(disconnectBtn);
        buttonPanel.add(settingsBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(saveSceneBtn);
        buttonPanel.add(loadSceneBtn);
        buttonPanel.add(new JLabel()); // 占位

        sidebar.add(buttonPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    // ===== 右侧内容区 =====

    private JPanel createContentArea() {
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.add(createEmptyStatePanel(), "empty");
        return contentPanel;
    }

    /**
     * 空状态面板：图标 + 提示文字 + 引导按钮，避免空白页面的"未完成"感。
     */
    private JPanel createEmptyStatePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.BG_SUBTLE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, UITheme.SPACING_MD, 0);

        JLabel iconLabel = new JLabel(Icons.empty());
        iconLabel.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(iconLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(UITheme.SPACING_SM, UITheme.SPACING_LG, UITheme.SPACING_SM, UITheme.SPACING_LG);
        JLabel hintLabel = new JLabel("请选择或新建连接");
        hintLabel.setFont(UITheme.UI_FONT.deriveFont(Font.BOLD, 15f));
        hintLabel.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(hintLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(UITheme.SPACING_SM, 0, 0, 0);
        JButton createBtn = new JButton(" + 新建连接 ");
        createBtn.setIcon(Icons.newIcon());
        UITheme.applyPrimaryButton(createBtn);
        createBtn.addActionListener(e -> addNewSession());
        panel.add(createBtn, gbc);

        return panel;
    }

    // ===== 会话管理 =====

    private void addNewSession() {
        sessionCounter++;
        String name = "连接 " + sessionCounter;
        ConnectionConfig config = new ConnectionConfig();
        addNewSession(name, config);
    }

    /**
     * 以指定名称和配置创建新会话。
     */
    private void addNewSession(String name, ConnectionConfig config) {
        ClientSession session = new ClientSession(name, config);

        // 状态变化时刷新列表
        session.addStateCallback(state -> SwingUtilities.invokeLater(() -> {
            refreshList();
            notifyStateChanged();
        }));
        session.addErrorCallback(msg -> SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, msg, name + " 错误", JOptionPane.ERROR_MESSAGE)));

        sessions.add(session);
        SessionPanel panel = new SessionPanel(session);
        contentPanel.add(panel, name);

        refreshList();
        sessionList.setSelectedIndex(sessions.size() - 1);
    }

    private void deleteSession() {
        int index = sessionList.getSelectedIndex();
        if (index < 0 || index >= sessions.size()) return;

        ClientSession session = sessions.get(index);
        if (session.getClientManager().isConnected()) {
            int ret = JOptionPane.showConfirmDialog(this,
                    "连接 \"" + session.getName() + "\" 正在使用中，确定要断开并删除吗？",
                    "确认删除", JOptionPane.OK_CANCEL_OPTION);
            if (ret != JOptionPane.OK_OPTION) return;
            session.getClientManager().disconnect();
        }

        sessions.remove(index);
        contentPanel.remove(index);

        refreshList();
        if (!sessions.isEmpty()) {
            sessionList.setSelectedIndex(Math.min(index, sessions.size() - 1));
        } else {
            cardLayout.show(contentPanel, "empty");
        }
        notifyStateChanged();
    }

    private void switchSession(int index) {
        if (index < 0 || index >= sessions.size()) {
            cardLayout.show(contentPanel, "empty");
            return;
        }
        ClientSession session = sessions.get(index);
        cardLayout.show(contentPanel, session.getName());
        notifyStateChanged();
    }

    private void doConnect() {
        ClientSession session = getSelectedSession();
        if (session == null) return;
        if (session.getClientManager().isConnected()) return;

        session.getCaptureManager().startCapture();
        session.clearData();
        session.getClientManager().connect(session.getConfig());
    }

    private void doDisconnect() {
        ClientSession session = getSelectedSession();
        if (session == null) return;
        session.getClientManager().disconnect();
    }

    private void openSettings() {
        ClientSession session = getSelectedSession();
        if (session == null) return;
        if (session.getClientManager().isConnected()) {
            JOptionPane.showMessageDialog(this, "请先断开连接再修改设置", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ConnectionConfig copy = session.getConfig().copy();
        ConnectionConfigDialog dialog = new ConnectionConfigDialog(getParentFrame(), copy);
        dialog.setVisible(true);
        if (dialog.isApproved()) {
            session.setConfig(dialog.getConfig());
            refreshList();
        }
    }

    // ===== 场景保存/加载 =====

    /**
     * 保存当前所有连接配置到 JSON 文件（.iec104）。
     */
    private void saveScene() {
        if (sessions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有可保存的连接", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("保存场景");
        chooser.setFileFilter(new FileNameExtensionFilter("IEC104 场景文件 (*.iec104)", "iec104"));
        chooser.setSelectedFile(new File("scene.iec104"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".iec104")) {
            file = new File(file.getParentFile(), file.getName() + ".iec104");
        }

        SceneConfig scene = new SceneConfig(file.getName().replace(".iec104", ""));
        for (ClientSession s : sessions) {
            scene.addClient(s.getName(), s.getConfig().copy());
        }
        scene.stampSaveTime();

        try {
            scene.saveToFile(file);
            JOptionPane.showMessageDialog(this,
                    "成功保存 " + sessions.size() + " 个连接到\n" + file.getAbsolutePath(),
                    "保存成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 从 JSON 文件（.iec104）加载场景，清除现有连接并重建。
     */
    private void loadScene() {
        // 若存在已连接的会话，提示用户确认
        for (ClientSession s : sessions) {
            if (s.getClientManager().isConnected()) {
                int ret = JOptionPane.showConfirmDialog(this,
                        "当前存在已连接的会话，加载场景将断开并清除所有现有连接，是否继续？",
                        "确认加载", JOptionPane.OK_CANCEL_OPTION);
                if (ret != JOptionPane.OK_OPTION) return;
                break;
            }
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("加载场景");
        chooser.setFileFilter(new FileNameExtensionFilter("IEC104 场景文件 (*.iec104)", "iec104"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();

        SceneConfig scene;
        try {
            scene = SceneConfig.loadFromFile(file);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "加载失败: " + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (scene.getClients() == null || scene.getClients().isEmpty()) {
            JOptionPane.showMessageDialog(this, "文件中未包含任何连接配置", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // 清除现有连接
        clearAllSessions();

        // 根据文件内容重建连接
        for (SceneConfig.ClientEntry entry : scene.getClients()) {
            String name = entry.getName() != null ? entry.getName() : ("连接 " + (sessionCounter + 1));
            ConnectionConfig cfg = entry.getConfig() != null ? entry.getConfig().copy() : new ConnectionConfig();
            addNewSession(name, cfg);
        }

        JOptionPane.showMessageDialog(this,
                "成功加载 " + scene.getClients().size() + " 个连接",
                "加载成功", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 断开并清除所有会话。
     */
    private void clearAllSessions() {
        for (ClientSession s : sessions) {
            if (s.getClientManager().isConnected()) {
                s.getClientManager().disconnect();
            }
        }
        sessions.clear();
        contentPanel.removeAll();
        contentPanel.add(createEmptyStatePanel(), "empty");
        cardLayout.show(contentPanel, "empty");
        sessionCounter = 0;
        refreshList();
        notifyStateChanged();
    }

    private ClientSession getSelectedSession() {
        int index = sessionList.getSelectedIndex();
        if (index < 0 || index >= sessions.size()) return null;
        return sessions.get(index);
    }

    private void refreshList() {
        int selected = sessionList.getSelectedIndex();
        listModel.clear();
        for (ClientSession s : sessions) {
            listModel.addElement(s);
        }
        if (selected >= 0 && selected < sessions.size()) {
            sessionList.setSelectedIndex(selected);
        }
    }

    // ===== 对外接口 =====

    public void setStateChangedCallback(Runnable callback) {
        this.stateChangedCallback = callback;
    }

    private void notifyStateChanged() {
        if (stateChangedCallback != null) stateChangedCallback.run();
    }

    /**
     * 获取当前选中连接的状态文本（供 MainFrame 状态栏使用）
     */
    public String getActiveStatusText() {
        ClientSession session = getSelectedSession();
        if (session == null) return "无连接";
        return session.getName() + ": " + session.getStatusText();
    }

    /**
     * 获取当前选中连接的报文数（供 MainFrame 状态栏使用）
     */
    public int getActivePacketCount() {
        ClientSession session = getSelectedSession();
        if (session == null) return 0;
        return session.getCaptureManager().getPacketCount();
    }

    /**
     * 获取当前选中连接的 CaptureManager（供 MainFrame 保存/导出使用）
     */
    public com.iec104tester.capture.CaptureManager getActiveCaptureManager() {
        ClientSession session = getSelectedSession();
        if (session == null) return null;
        return session.getCaptureManager();
    }

    private JFrame getParentFrame() {
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof JFrame) return (JFrame) w;
        return null;
    }

    /**
     * 连接列表渲染器：双行布局
     * 第一行：连接名称（主色）+ 状态色块
     * 第二行：host:port（辅助色）
     */
    private static class SessionListCellRenderer extends JPanel implements ListCellRenderer<ClientSession> {
        private final JLabel nameLabel = new JLabel();
        private final JLabel addrLabel = new JLabel();
        private final JLabel statusDot = new JLabel("●");

        SessionListCellRenderer() {
            setLayout(new BorderLayout(UITheme.SPACING_SM, 0));
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(
                    UITheme.SPACING_SM, UITheme.SPACING_MD,
                    UITheme.SPACING_SM, UITheme.SPACING_MD));

            JPanel textPanel = new JPanel(new BorderLayout());
            textPanel.setOpaque(false);
            nameLabel.setFont(UITheme.UI_FONT.deriveFont(Font.PLAIN, 13f));
            addrLabel.setFont(UITheme.UI_FONT.deriveFont(Font.PLAIN, 11f));
            addrLabel.setForeground(UITheme.TEXT_SECONDARY);
            textPanel.add(nameLabel, BorderLayout.NORTH);
            textPanel.add(addrLabel, BorderLayout.CENTER);

            statusDot.setFont(UITheme.UI_FONT.deriveFont(Font.PLAIN, 12f));
            statusDot.setHorizontalAlignment(SwingConstants.RIGHT);

            add(textPanel, BorderLayout.CENTER);
            add(statusDot, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ClientSession> list,
                                                      ClientSession session, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            if (session == null) return this;

            ConnectionConfig cfg = session.getConfig();
            nameLabel.setText(session.getName());
            addrLabel.setText(cfg.getHost() + ":" + cfg.getPort());

            // 状态色块
            String status = session.getStatusText();
            statusDot.setForeground(UITheme.colorForStatus(status));
            statusDot.setToolTipText(status);

            // 选中态
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                nameLabel.setForeground(list.getSelectionForeground());
                addrLabel.setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                nameLabel.setForeground(UITheme.TEXT_PRIMARY);
                addrLabel.setForeground(UITheme.TEXT_SECONDARY);
            }
            return this;
        }
    }
}
