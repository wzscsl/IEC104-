package com.iec104tester.core;

import com.iec104tester.capture.PacketRecord;
import com.openmuc.j60870.ASdu;
import com.openmuc.j60870.ASduType;
import com.openmuc.j60870.CauseOfTransmission;
import com.openmuc.j60870.ie.InformationElement;
import com.openmuc.j60870.ie.InformationObject;

/**
 * Decodes ASDU to human-readable text with Chinese annotations.
 */
public class AsduDecoder {

    /**
     * Get Chinese name for ASDU type.
     */
    public static String getTypeNameCn(ASduType type) {
        if (type == null) return "";
        switch (type) {
            // 监视方向
            case M_SP_NA_1: return "单点信息";
            case M_SP_TA_1: return "单点信息(带时标)";
            case M_DP_NA_1: return "双点信息";
            case M_DP_TA_1: return "双点信息(带时标)";
            case M_ST_NA_1: return "步位置信息";
            case M_BO_NA_1: return "32位串";
            case M_ME_NA_1: return "测量值-标幺化值";
            case M_ME_NB_1: return "测量值-标度化值";
            case M_ME_NC_1: return "测量值-短浮点数";
            case M_IT_NA_1: return "累计量";
            // 控制方向
            case C_SC_NA_1: return "单点命令";
            case C_DC_NA_1: return "双点命令";
            case C_RC_NA_1: return "调节步命令";
            case C_SE_NA_1: return "设点-标幺化值";
            case C_SE_NB_1: return "设点-标度化值";
            case C_SE_NC_1: return "设点-短浮点数";
            // 系统命令
            case C_IC_NA_1: return "总召唤命令";
            case C_CI_NA_1: return "电度总召唤";
            case C_RD_NA_1: return "读命令";
            case C_CS_NA_1: return "时钟同步";
            case C_TS_NA_1: return "测试命令";
            case C_RP_NA_1: return "复位进程命令";
            case M_EI_NA_1: return "初始化结束";
            // 扩展时标类型
            case M_SP_TB_1: return "单点信息(带CP56时标)";
            case M_DP_TB_1: return "双点信息(带CP56时标)";
            case M_ME_TD_1: return "测量值-标幺化值(带时标)";
            case M_ME_TE_1: return "测量值-标度化值(带时标)";
            case M_ME_TF_1: return "测量值-短浮点数(带时标)";
            case C_SC_TA_1: return "单点命令(带时标)";
            case C_DC_TA_1: return "双点命令(带时标)";
            case C_SE_TA_1: return "设点-标幺化值(带时标)";
            case C_SE_TB_1: return "设点-标度化值(带时标)";
            case C_SE_TC_1: return "设点-短浮点数(带时标)";
            default:
                String desc = type.getDescription();
                return desc != null ? desc : type.toString();
        }
    }

    /**
     * Get Chinese name for cause of transmission.
     */
    public static String getCotNameCn(CauseOfTransmission cot) {
        if (cot == null) return "";
        switch (cot) {
            case PERIODIC: return "周期上送";
            case BACKGROUND_SCAN: return "背景扫描";
            case SPONTANEOUS: return "突发(自发)";
            case INITIALIZED: return "初始化";
            case REQUEST: return "请求";
            case ACTIVATION: return "激活";
            case ACTIVATION_CON: return "激活确认";
            case DEACTIVATION: return "停止激活";
            case DEACTIVATION_CON: return "停止激活确认";
            case ACTIVATION_TERMINATION: return "激活终止";
            case RETURN_INFO_REMOTE: return "返回信息-远端源";
            case RETURN_INFO_LOCAL: return "返回信息-本地源";
            case FILE_TRANSFER: return "文件传输";
            case INTERROGATED_BY_STATION: return "被站召唤";
            case REQUESTED_BY_GENERAL_COUNTER: return "被计数器召唤";
            case REQUESTED_BY_GROUP_1_COUNTER: return "被计数器组1召唤";
            case UNKNOWN_TYPE_ID: return "未知类型标识";
            case UNKNOWN_CAUSE_OF_TRANSMISSION: return "未知传送原因";
            case UNKNOWN_COMMON_ADDRESS_OF_ASDU: return "未知公共地址";
            case UNKNOWN_INFORMATION_OBJECT_ADDRESS: return "未知信息对象地址";
            default: return cot.toString();
        }
    }

    /**
     * Format raw bytes as a hex dump with ASCII representation.
     * Each line: offset | hex bytes (16 per line) | ASCII
     */
    public static String toHexDump(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        int lines = (bytes.length + 15) / 16;
        for (int line = 0; line < lines; line++) {
            int start = line * 16;
            int end = Math.min(start + 16, bytes.length);

            // Offset
            sb.append(String.format("%04X  ", start));

            // Hex bytes
            for (int i = start; i < start + 16; i++) {
                if (i < end) {
                    sb.append(String.format("%02X ", bytes[i] & 0xFF));
                } else {
                    sb.append("   ");
                }
                if (i == start + 7) sb.append(" ");
            }

            sb.append(" |");

            // ASCII
            for (int i = start; i < end; i++) {
                int b = bytes[i] & 0xFF;
                sb.append(b >= 32 && b < 127 ? (char) b : '.');
            }

            sb.append("|\n");
        }
        return sb.toString();
    }

    /**
     * Build display text from a PacketRecord (used for loaded packets without live ASdu object).
     */
    public static String buildDisplayText(PacketRecord record) {
        StringBuilder sb = new StringBuilder();
        ASduType type = record.getAsduType();
        sb.append("ASDU类型: ").append(getTypeNameCn(type))
          .append(" (").append(type != null ? type : "").append(")")
          .append(" [TypeID=").append(record.getTypeId()).append("]\n");
        sb.append("传送原因: ").append(record.getCauseOfTransmission()).append("\n");
        sb.append("公共地址: ").append(record.getCommonAddress()).append("\n");
        sb.append("源发地址: ").append(record.getOriginatorAddress()).append("\n");
        sb.append("信息对象: ").append(record.getInfoObjectSummary()).append("\n");
        sb.append("\n--- 原始解码输出 ---\n");
        sb.append(record.getDecodedText());
        return sb.toString();
    }

    /**
     * Decode an ASDU to structured text with Chinese annotations.
     */
    public static String decodeAsdu(ASdu asdu) {
        if (asdu == null) return "(无ASDU内容 - U/S格式帧)";

        StringBuilder sb = new StringBuilder();
        ASduType type = asdu.getTypeIdentification();

        sb.append("ASDU类型: ").append(getTypeNameCn(type))
          .append(" (").append(type).append(")")
          .append(" [TypeID=").append(type != null ? type.getId() : 0).append("]\n");

        sb.append("传送原因: ").append(getCotNameCn(asdu.getCauseOfTransmission()))
          .append(" [").append(asdu.getCauseOfTransmission()).append("]\n");

        sb.append("公共地址: ").append(asdu.getCommonAddress()).append("\n");
        sb.append("源发地址: ").append(asdu.getOriginatorAddress()).append("\n");
        sb.append("测试标志: ").append(asdu.isTestFrame()).append("\n");
        sb.append("否定确认: ").append(asdu.isNegativeConfirm()).append("\n");

        InformationObject[] objects = asdu.getInformationObjects();
        if (objects != null) {
            sb.append("信息对象数: ").append(objects.length).append("\n");
            for (int i = 0; i < objects.length; i++) {
                InformationObject obj = objects[i];
                sb.append("  [").append(i + 1).append("] IOA=").append(obj.getInformationObjectAddress());
                InformationElement[][] elements = obj.getInformationElements();
                if (elements != null) {
                    for (int j = 0; j < elements.length; j++) {
                        for (int k = 0; k < elements[j].length; k++) {
                            if (elements[j][k] != null) {
                                sb.append("  ").append(elements[j][k].toString());
                            }
                        }
                    }
                }
                sb.append("\n");
            }
        }

        sb.append("\n--- 原始输出 ---\n");
        sb.append(asdu.toString());

        return sb.toString();
    }
}
