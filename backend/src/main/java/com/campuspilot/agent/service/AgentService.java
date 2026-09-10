package com.campuspilot.agent.service;

import com.campuspilot.agent.model.AgentRequest;
import com.campuspilot.agent.model.AgentResponse;

public interface AgentService {
    
    AgentResponse chat(AgentRequest request);
}