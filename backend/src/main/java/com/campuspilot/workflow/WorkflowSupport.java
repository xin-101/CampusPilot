package com.campuspilot.workflow;

import com.campuspilot.rag.RetrievedDocument;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Workflow 公共辅助
 */
public final class WorkflowSupport {

    private WorkflowSupport() {
    }

    /**
     * 从用户消息推断政策分类(确定性关键词，非LLM)
     */
    public static String inferCategory(String message) {
        String msg = message == null ? "" : message.toLowerCase(Locale.ROOT);
        List<String[]> hints = Arrays.asList(
            new String[]{"SCHOLARSHIP", "奖"},
            new String[]{"AID", "助"},
            new String[]{"LEAVE", "请假"},
            new String[]{"EXAMINATION", "考试"},
            new String[]{"DORMITORY", "宿舍"},
            new String[]{"CERTIFICATE", "在读证明"}
        );
        for (String[] hint : hints) {
            if (msg.contains(hint[1])) {
                return hint[0];
            }
        }
        return null;
    }

    /**
     * 渲染政策文档为可读文本
     */
    public static String renderPolicyDocument(RetrievedDocument doc) {
        if (doc == null) {
            return "[无政策内容]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("《").append(doc.getPolicyName()).append("》");
        if (doc.getVersion() != null) {
            sb.append("（").append(doc.getVersion()).append("）");
        }
        sb.append("\n\n");
        String content = doc.getContent();
        if (content != null) {
            String[] lines = content.split("\\r?\\n");
            int shown = 0;
            for (String line : lines) {
                String t = line.trim();
                if (t.isEmpty()) {
                    continue;
                }
                sb.append(t).append("\n");
                if (++shown >= 12) {
                    sb.append("……\n");
                    break;
                }
            }
        }
        if (doc.getSource() != null) {
            sb.append("\n📎 来源：").append(doc.getSource());
        }
        return sb.toString().trim();
    }
}