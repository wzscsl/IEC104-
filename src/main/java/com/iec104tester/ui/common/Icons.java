package com.iec104tester.ui.common;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;

/**
 * 图标统一加载工具。
 * SVG 文件位于 src/main/resources/icons/，通过 FlatSVGIcon 渲染。
 * 自动跟随主题色（currentColor），无需为亮/暗主题分别维护图标。
 */
public final class Icons {

    private Icons() {}

    /** 图标默认尺寸（与 16px 字体匹配） */
    private static final int DEFAULT_SIZE = 16;
    /** 菜单/小按钮用 */
    private static final int MENU_SIZE = 16;
    /** Tab 用 */
    private static final int TAB_SIZE = 16;
    /** 空状态大图标 */
    private static final int EMPTY_SIZE = 48;

    public static Icon newIcon() { return svg("new", MENU_SIZE); }
    public static Icon connect() { return svg("connect", DEFAULT_SIZE); }
    public static Icon disconnect() { return svg("disconnect", DEFAULT_SIZE); }
    public static Icon settings() { return svg("settings", DEFAULT_SIZE); }
    public static Icon delete() { return svg("delete", DEFAULT_SIZE); }
    public static Icon start() { return svg("start", DEFAULT_SIZE); }
    public static Icon stop() { return svg("stop", DEFAULT_SIZE); }
    public static Icon save() { return svg("save", MENU_SIZE); }
    public static Icon load() { return svg("load", MENU_SIZE); }

    public static Icon client() { return svg("client", TAB_SIZE); }
    public static Icon server() { return svg("server", TAB_SIZE); }
    public static Icon packet() { return svg("packet", TAB_SIZE); }
    public static Icon telesignaling() { return svg("telesignaling", TAB_SIZE); }
    public static Icon telemetry() { return svg("telemetry", TAB_SIZE); }
    public static Icon teleadjust() { return svg("teleadjust", TAB_SIZE); }
    public static Icon telecommand() { return svg("telecommand", TAB_SIZE); }
    public static Icon datapoint() { return svg("datapoint", TAB_SIZE); }

    public static Icon empty() { return svg("empty", EMPTY_SIZE); }

    /**
     * 从 resources/icons/{name}.svg 加载并返回着色后的图标。
     * FlatSVGIcon 默认使用当前组件 foreground 作为 stroke 色。
     */
    private static Icon svg(String name, int size) {
        try {
            return new FlatSVGIcon("icons/" + name + ".svg", size, size);
        } catch (Throwable e) {
            // 资源缺失时返回空图标，避免影响启动
            return UIManager.getIcon("Tree.leafIcon");
        }
    }

    /**
     * 给一个按钮设置图标，文字保留。
     */
    public static void apply(AbstractButton button, Icon icon) {
        button.setIcon(icon);
    }
}
