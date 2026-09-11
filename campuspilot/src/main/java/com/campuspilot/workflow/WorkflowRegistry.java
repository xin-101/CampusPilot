package com.campuspilot.workflow;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流注册表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowRegistry {

    private final List<AgentWorkflow> workflowList;

    private final Map<String, AgentWorkflow> byId = new HashMap<>();
    private final Map<String, AgentWorkflow> byIntent = new HashMap<>();

    @PostConstruct
    public void init() {
        for (AgentWorkflow workflow : workflowList) {
            byId.put(workflow.id(), workflow);
            if (workflow.supportsIntent() != null && !workflow.supportsIntent().isBlank()) {
                byIntent.put(workflow.supportsIntent(), workflow);
                log.info("Registered workflow: {} (intent={})", workflow.id(), workflow.supportsIntent());
            }
        }
    }

    public AgentWorkflow getById(String id) {
        return byId.get(id);
    }

    public AgentWorkflow getByIntent(String intent) {
        return byIntent.get(intent);
    }
}