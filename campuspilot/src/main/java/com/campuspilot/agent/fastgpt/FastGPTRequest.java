package com.campuspilot.agent.fastgpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FastGPT Chat 请求参数
 * 注意：字段定义以 SF-FastGPT 官方 API 文档为准。
 * TODO: 根据实际 SF-FastGPT API 文档补充/调整字段（当前为占位定义，未验证）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FastGPTRequest {

    /** 对话模型/工作流配置（待文档确认） */
    private String appId;

    /** 对话ID（多轮会话，待文档确认） */
    private String chatId;

    /** 对话历史（待文档确认） */
    private Object[] messages;

    /** 响应模式：blocking / streaming（待文档确认） */
    private String responseMode;

    /** 用户输入 */
    private String query;

    /** 变量（待文档确认） */
    private Object variables;
}