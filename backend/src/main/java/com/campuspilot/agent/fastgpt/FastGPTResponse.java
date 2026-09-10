package com.campuspilot.agent.fastgpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * FastGPT Chat/检索 响应参数
 * 注意：字段定义以 SF-FastGPT 官方 API 文档为准。
 * TODO: 根据实际 SF-FastGPT API 文档补充/调整字段（当前为占位定义，未验证）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FastGPTResponse {

    /** HTTP状态码（待文档确认） */
    private int code;

    /** 状态文案（待文档确认） */
    private String statusText;

    /** 业务数据（待文档确认） */
    private Object data;

    /** 文本回答（待文档确认） */
    private String response;

    /** 引用来源列表（待文档确认） */
    private List<Map<String, Object>> citations;

    public boolean isSuccess() {
        return code == 200 || (data != null || response != null);
    }
}