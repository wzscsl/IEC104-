package com.iec104tester.ui;

import com.iec104tester.capture.PacketRecord;
import com.iec104tester.core.AsduDecoder;

import javax.swing.*;
import java.awt.*;

/**
 * Panel displaying detailed information about a selected packet.
 */
public class MessageDetailPanel extends JPanel {

    private final JTextArea detailArea;
    private final JTextArea hexArea;

    public MessageDetailPanel() {
        setLayout(new BorderLayout());

        JTabbedPane tabPane = new JTabbedPane();

        // Detail tab
        detailArea = new JTextArea();
        detailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        JScrollPane detailScroll = new JScrollPane(detailArea);
        tabPane.addTab("详细信息", detailScroll);

        // Hex dump tab
        hexArea = new JTextArea();
        hexArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        hexArea.setEditable(false);
        JScrollPane hexScroll = new JScrollPane(hexArea);
        tabPane.addTab("原始报文(HEX)", hexScroll);

        add(tabPane, BorderLayout.CENTER);
    }

    public void setPacket(PacketRecord record) {
        if (record == null) {
            detailArea.setText("");
            hexArea.setText("");
            return;
        }

        // Build detail text
        StringBuilder sb = new StringBuilder();
        sb.append("时间: ").append(record.getTimeStr()).append("\n");
        sb.append("方向: ").append(record.getDirectionStr()).append("\n");
        sb.append("帧类型: ").append(record.getFrameType()).append("\n");
        if (record.getSendSeqNum() != 0 || record.getReceiveSeqNum() != 0) {
            sb.append("发送序号: ").append(record.getSendSeqNum());
            sb.append("  接收序号: ").append(record.getReceiveSeqNum()).append("\n");
        }
        sb.append("\n");

        if (record.getDecodedText() != null && !record.getDecodedText().isEmpty()) {
            sb.append(AsduDecoder.buildDisplayText(record));
        } else {
            sb.append("(U/S格式帧，无ASDU内容)\n");
            switch (record.getFrameType()) {
                case "STARTDT_ACT": sb.append("启动数据传输激活\n"); break;
                case "STARTDT_CON": sb.append("启动数据传输确认\n"); break;
                case "STOPDT_ACT": sb.append("停止数据传输激活\n"); break;
                case "STOPDT_CON": sb.append("停止数据传输确认\n"); break;
                case "TESTFR_ACT": sb.append("测试帧激活\n"); break;
                case "TESTFR_CON": sb.append("测试帧确认\n"); break;
                case "S_FORMAT": sb.append("S格式确认帧\n"); break;
            }
        }

        detailArea.setText(sb.toString());
        detailArea.setCaretPosition(0);

        // Hex dump
        hexArea.setText(AsduDecoder.toHexDump(record.getRawBytes()));
        hexArea.setCaretPosition(0);
    }
}
