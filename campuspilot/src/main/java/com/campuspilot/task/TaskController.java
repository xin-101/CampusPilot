package com.campuspilot.task;

import com.campuspilot.common.result.ApiResponse;
import com.campuspilot.dto.TaskCreateRequest;
import com.campuspilot.vo.TaskVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    
    private final TaskService taskService;
    
    @GetMapping
    public ApiResponse<List<TaskVO>> getCurrentUserTasks() {
        List<TaskVO> tasks = taskService.getCurrentUserTasks();
        return ApiResponse.success(tasks);
    }
    
    @GetMapping("/{id}")
    public ApiResponse<TaskVO> getTaskById(@PathVariable Long id) {
        TaskVO task = taskService.getTaskById(id);
        if (task == null) {
            return ApiResponse.error(404, "任务不存在");
        }
        return ApiResponse.success(task);
    }
    
    @PostMapping
    public ApiResponse<TaskVO> createTask(@Valid @RequestBody TaskCreateRequest request) {
        TaskVO task = taskService.createTask(
            request.getTitle(), 
            request.getDescription(), 
            request.getDeadline(), 
            request.getRelatedPolicyId()
        );
        return ApiResponse.success("任务创建成功", task);
    }
    
    @PutMapping("/{id}/complete")
    public ApiResponse<TaskVO> completeTask(@PathVariable Long id) {
        TaskVO task = taskService.updateTaskStatus(id, "COMPLETED");
        if (task == null) {
            return ApiResponse.error(404, "任务不存在");
        }
        return ApiResponse.success("任务已完成", task);
    }
    
    @PutMapping("/{id}/cancel")
    public ApiResponse<TaskVO> cancelTask(@PathVariable Long id) {
        TaskVO task = taskService.updateTaskStatus(id, "CANCELLED");
        if (task == null) {
            return ApiResponse.error(404, "任务不存在");
        }
        return ApiResponse.success("任务已取消", task);
    }
}