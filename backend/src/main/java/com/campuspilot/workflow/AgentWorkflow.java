package com.campuspilot.workflow;

/**
 * Agent工作流抽象接口
 */
public interface AgentWorkflow {

    /** 工作流ID */
    String id();

    /** 工作流名称 */
    String name();

    /** 描述 */
    String description();

    /** 该工作流支持的意图(空表示通用) */
    String supportsIntent();

    /** 执行工作流 */
    WorkflowResult execute(WorkflowContext context);
}