package com.campuspilot.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campuspilot.entity.Task;
import com.campuspilot.entity.User;
import com.campuspilot.mapper.TaskMapper;
import com.campuspilot.mapper.UserMapper;
import com.campuspilot.security.SecurityUtils;
import com.campuspilot.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    
    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    
    @Override
    public List<TaskVO> getCurrentUserTasks() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return List.of();
        }
        
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getIsDeleted, 0)
        );
        
        if (user == null) {
            return List.of();
        }
        
        List<Task> tasks = taskMapper.selectList(
            new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, user.getId())
                .eq(Task::getIsDeleted, 0)
                .orderByDesc(Task::getCreatedAt)
        );
        
        return tasks.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    @Override
    public TaskVO getTaskById(Long id) {
        Task task = fetchTaskWithOwnershipCheck(id);
        if (task == null) {
            return null;
        }
        return convertToVO(task);
    }
    
    @Override
    public TaskVO createTask(String title, String description, String deadline, Long policyId) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new RuntimeException("用户未登录");
        }
        
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getIsDeleted, 0)
        );
        
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        LocalDateTime deadlineTime = null;
        if (deadline != null && !deadline.isEmpty()) {
            deadlineTime = LocalDateTime.parse(deadline, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        
        Task task = Task.builder()
            .userId(user.getId())
            .policyId(policyId)
            .title(title)
            .description(description)
            .status("PENDING")
            .priority("NORMAL")
            .deadline(deadlineTime)
            .createdBy(user.getId())
            .build();
        
        taskMapper.insert(task);
        return convertToVO(task);
    }
    
    @Override
    public TaskVO updateTaskStatus(Long id, String status) {
        Task task = fetchTaskWithOwnershipCheck(id);
        if (task == null) {
            return null;
        }
        
        task.setStatus(status);
        if ("COMPLETED".equals(status)) {
            task.setCompletedAt(LocalDateTime.now());
        }
        
        taskMapper.updateById(task);
        return convertToVO(task);
    }
    
    /**
     * 查询任务并做数据访问校验：学生只能访问自己的任务，教师/管理员角色拥有完整权限
     */
    private Task fetchTaskWithOwnershipCheck(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null || task.getIsDeleted() == 1) {
            return null;
        }
        
        String role = SecurityUtils.getCurrentRole();
        if (role == null || role.contains("STUDENT")) {
            User currentUser = getCurrentUser();
            if (currentUser == null || !task.getUserId().equals(currentUser.getId())) {
                return null;
            }
        }
        return task;
    }
    
    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return null;
        }
        return userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getIsDeleted, 0)
        );
    }
    
    private TaskVO convertToVO(Task task) {
        return TaskVO.builder()
            .id(task.getId())
            .userId(task.getUserId())
            .policyId(task.getPolicyId())
            .title(task.getTitle())
            .description(task.getDescription())
            .status(task.getStatus())
            .priority(task.getPriority())
            .deadline(task.getDeadline())
            .completedAt(task.getCompletedAt())
            .createdAt(task.getCreatedAt())
            .updatedAt(task.getUpdatedAt())
            .source(task.getCreatedBy() != null ? "USER" : "AGENT")
            .build();
    }
}