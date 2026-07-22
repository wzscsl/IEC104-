package com.iec104tester;

import com.iec104tester.ui.MainFrame;
import com.iec104tester.ui.common.UITheme;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // 安装主题：FlatLaf + 统一字体/配色/间距
        // 必须在创建任何 Swing 组件之前调用
        UITheme.setup();

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
