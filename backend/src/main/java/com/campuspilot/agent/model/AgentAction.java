package com.campuspilot.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 建议下一步行动。
 * 由后端根据 Workflow 结果确定性生成，前端据此渲染按钮/卡片，避免从 answer 文本猜测。
 * type: CREATE_TASK / VIEW_POLICY / VIEW_TASKS
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentAction {

    /** 行动类型: CREATE_TASK / VIEW_POLICY / VIEW_TASKS */
    private String type;

    /** 按钮文案 */
    private String label;

    /** 附加参数(如 policyId / policyName / taskId) */
    private Map<String, Object> params;
}