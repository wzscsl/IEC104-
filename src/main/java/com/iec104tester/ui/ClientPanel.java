package com.iec104tester.ui;

import com.iec104tester.core.ClientManager;
import com.iec104tester.core.ClientSession;
import com.iec104tester.model.ConnectionConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
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
    private JList<String> sessionList;
    private DefaultListModel<String> listModel;
    private JButton newBtn;
    private JButton connectBtn;
    private JButton disconnectBtn;
    private JButton settingsBtn;
    private JButton deleteBtn;

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
        JPanel buttonPanel = new JPanel(new GridLayout(3, 2, 3, 3));
        newBtn = new JButton("新建");
        newBtn.addActionListener(e -> addNewSession());
        connectBtn = new JButton("连接");
        connectBtn.addActionListener(e -> doConnect());
        disconnectBtn = new JButton("断开");
        disconnectBtn.addActionListener(e -> doDisconnect());
        settingsBtn = new JButton("设置");
        settingsBtn.addActionListener(e -> openSettings());
        deleteBtn = new JButton("删除");
        deleteBtn.addActionListener(e -> deleteSession());

        buttonPanel.add(newBtn);
        buttonPanel.add(connectBtn);
        buttonPanel.add(disconnectBtn);
        buttonPanel.add(settingsBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(new JLabel()); // 占位

        sidebar.add(buttonPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    // ===== 右侧内容区 =====

    private JPanel createContentArea() {
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.add(new JLabel("请选择或新建连接", SwingConstants.CENTER), "empty");
        return contentPanel;
    }

    // ===== 会话管理 =====

    private void addNewSession() {
        sessionCounter++;
        String name = "连接 " + sessionCounter;
        ConnectionConfig config = new ConnectionConfig();
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

    private ClientSession getSelectedSession() {
        int index = sessionList.getSelectedIndex();
        if (index < 0 || index >= sessions.size()) return null;
        return sessions.get(index);
    }

    private void refreshList() {
        int selected = sessionList.getSelectedIndex();
        listModel.clear();
        for (ClientSession s : sessions) {
            ConnectionConfig cfg = s.getConfig();
            String item = String.format("%s  [%s:%d]  %s",
                    s.getName(), cfg.getHost(), cfg.getPort(), s.getStatusText());
            listModel.addElement(item);
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
}
