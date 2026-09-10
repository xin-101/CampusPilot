package com.campuspilot.task;

import com.campuspilot.vo.TaskVO;

import java.util.List;

public interface TaskService {
    
    List<TaskVO> getCurrentUserTasks();
    
    TaskVO getTaskById(Long id);
    
    TaskVO createTask(String title, String description, String deadline, Long policyId);
    
    TaskVO updateTaskStatus(Long id, String status);
}