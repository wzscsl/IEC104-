package com.iec104tester.ui.common;

import com.formdev.flatlaf.FlatIntelliJLaf;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;

/**
 * UI 主题统一配置。
 * 集中管理配色、字体、间距常量，并在应用启动时一次性注入 UIManager，
 * 各 UI 组件通过 UIManager 自动继承样式，无需逐处修改。
 */
public final class UITheme {

    private UITheme() {}

    // ============ 间距规范（spacing tokens）============
    public static final int SPACING_XS = 4;
    public static final int SPACING_SM = 8;
    public static final int SPACING_MD = 12;
    public static final int SPACING_LG = 16;
    public static final int SPACING_XL = 24;

    /** 统一表单组件 Insets，替代原先 2/3/4/5/6/8 等混用值 */
    public static final Insets FORM_INSETS = new Insets(SPACING_SM, SPACING_MD, SPACING_SM, SPACING_MD);
    public static final Insets FORM_INSETS_SM = new Insets(SPACING_XS, SPACING_SM, SPACING_XS, SPACING_SM);

    // ============ 字体规范 ============
    /** UI 默认字体（中文优先使用微软雅黑，回退 SansSerif） */
    public static final Font UI_FONT = createUIFont(13f);
    /** 等宽字体，用于报文 HEX 视图、原始数据展示 */
    public static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 13);
    /** 二级标题字体，用于 TitledBorder、分组标题 */
    public static final Font H2_FONT = UI_FONT.deriveFont(Font.BOLD, 12f);

    // ============ 品牌色与语义色 ============
    /** 主色 / 强调色（按钮、链接、选中态） */
    public static final Color PRIMARY = new Color(0x2563EB);
    /** 主色 hover 态 */
    public static final Color PRIMARY_HOVER = new Color(0x1D4ED8);

    /** 语义色 - 成功（已连接、已启动、运行中） */
    public static final Color SUCCESS = new Color(0x16A34A);
    /** 语义色 - 警告（连接中、未响应、启动中） */
    public static final Color WARNING = new Color(0xEA580C);
    /** 语义色 - 危险（断开、错误、异常） */
    public static final Color DANGER = new Color(0xDC2626);
    /** 语义色 - 信息（普通提示、收方向） */
    public static final Color INFO = new Color(0x2563EB);

    /** 文本主色 */
    public static final Color TEXT_PRIMARY = new Color(0x1F2937);
    /** 文本辅助色（副标题、占位符） */
    public static final Color TEXT_SECONDARY = new Color(0x6B7280);

    /** 背景色 - 主区域 */
    public static final Color BG_MAIN = new Color(0xFFFFFF);
    /** 背景色 - 次级（卡片、面板背景） */
    public static final Color BG_SUBTLE = new Color(0xF8FAFC);
    /** 斑马条纹 - 偶数行 */
    public static final Color TABLE_STRIPE = new Color(0xF8FAFC);
    /** 分隔线颜色 */
    public static final Color BORDER = new Color(0xE5E7EB);

    // ============ 启动初始化 ============
    private static volatile boolean initialized = false;

    /**
     * 安装 FlatLaf 主题并注入全局 UIDefaults。
     * 应在 SwingUtilities.invokeLater 之前、main 方法入口调用一次。
     */
    public static synchronized void setup() {
        if (initialized) return;
        initialized = true;

        try {
            FlatIntelliJLaf.setup();
        } catch (Throwable e) {
            // FlatLaf 加载失败时回退到系统 L&F，保证可用
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        applyUIDefaults();
    }

    /**
     * 通过 UIManager 注入全局样式，对所有已存在的 UI 组件立即生效。
     */
    private static void applyUIDefaults() {
        UIManager.put("defaultFont", UI_FONT);

        // 全局字体覆盖（FlatLaf 也建议通过这些 key 设置）
        UIManager.put("Button.font", UI_FONT);
        UIManager.put("Label.font", UI_FONT);
        UIManager.put("TextField.font", UI_FONT);
        UIManager.put("PasswordField.font", UI_FONT);
        UIManager.put("TextArea.font", UI_FONT);
        UIManager.put("ComboBox.font", UI_FONT);
        UIManager.put("CheckBox.font", UI_FONT);
        UIManager.put("RadioButton.font", UI_FONT);
        UIManager.put("Spinner.font", UI_FONT);
        UIManager.put("Table.font", UI_FONT);
        UIManager.put("TableHeader.font", UI_FONT);
        UIManager.put("List.font", UI_FONT);
        UIManager.put("Tree.font", UI_FONT);
        UIManager.put("TabbedPane.font", UI_FONT);
        UIManager.put("MenuBar.font", UI_FONT);
        UIManager.put("Menu.font", UI_FONT);
        UIManager.put("MenuItem.font", UI_FONT);
        UIManager.put("PopupMenu.font", UI_FONT);
        UIManager.put("OptionPane.messageFont", UI_FONT);
        UIManager.put("OptionPane.buttonFont", UI_FONT);
        UIManager.put("TitledBorder.font", H2_FONT);
        UIManager.put("ToolTip.font", UI_FONT);

        // 表格优化
        UIManager.put("Table.rowHeight", 26);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 0));
        UIManager.put("Table.selectionBackground", PRIMARY);
        UIManager.put("Table.selectionForeground", new ColorUIResource(Color.WHITE));
        UIManager.put("Table.gridColor", BORDER);

        // 树
        UIManager.put("Tree.rowHeight", 26);

        // 按钮主次区分：主按钮风格（蓝色背景白字）通过客户端属性 buttonType=toolTipText 触发，
        // 具体使用时调用 UITheme.applyPrimaryButton(jButton)
        UIManager.put("Button.arc", 6);
        UIManager.put("Component.arc", 6);
        UIManager.put("Component.focusColor", PRIMARY);
        UIManager.put("Component.focusedBorderColor", PRIMARY);
        UIManager.put("Button.default.boldText", Boolean.TRUE);

        // 文本组件
        UIManager.put("TextField.background", BG_MAIN);
        UIManager.put("TextArea.background", BG_MAIN);
        UIManager.put("ComboBox.background", BG_MAIN);
    }

    /**
     * 将一个 JButton 标记为主按钮（确定/连接/启动等关键操作）。
     * 调用后按钮显示为蓝色背景白字，视觉突出。
     */
    public static void applyPrimaryButton(AbstractButton button) {
        button.putClientProperty("Button.buttonType", "roundRect");
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusable(true);
        button.putClientProperty("Button.focusedBackground", PRIMARY_HOVER);
    }

    /**
     * 根据状态文本推断语义颜色。
     * 用于状态栏、连接列表等展示状态文字的位置。
     */
    public static Color colorForStatus(String text) {
        if (text == null) return TEXT_SECONDARY;
        if (text.contains("已连接") || text.contains("运行中")) return SUCCESS;
        if (text.contains("连接中") || text.contains("启动中")) return WARNING;
        if (text.contains("错误") || text.contains("异常")) return DANGER;
        return TEXT_SECONDARY;
    }

    /**
     * 创建带圆角的主色按钮。
     */
    private static Font createUIFont(float size) {
        // 中文环境优先微软雅黑，否则 SansSerif
        String[] candidates = {"Microsoft YaHei UI", "Microsoft YaHei", "PingFang SC", "Segoe UI", "SansSerif"};
        for (String name : candidates) {
            Font f = new Font(name, Font.PLAIN, (int) size);
            if (!f.getFamily().equalsIgnoreCase("Dialog") && fontCanRender(f)) {
                return f;
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, (int) size);
    }

    /**
     * 简单检测字体是否可用（避免系统无对应字体时回退成默认 Dialog）。
     */
    private static boolean fontCanRender(Font f) {
        return f.canDisplayUpTo("测试中文 IEC104") == -1;
    }
}
