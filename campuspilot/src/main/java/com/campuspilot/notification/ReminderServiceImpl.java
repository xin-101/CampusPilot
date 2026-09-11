package com.campuspilot.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campuspilot.entity.Task;
import com.campuspilot.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒服务实现
 * 两种提醒：
 * 1. remind_at 到点提醒(尚未截止)
 * 2. due_at/deadline 到期未完成 -> 逾期提醒
 * 幂等：发送成功后设置 reminder_sent=1。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {

    private final TaskMapper taskMapper;
    private final NotificationService notificationService;

    @Override
    public int processDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        int sent = 0;

        // 1. 到点提醒: remind_at <= now 且未发送
        List<Task> remindDue = taskMapper.selectList(
            new LambdaQueryWrapper<Task>()
                .eq(Task::getReminderSent, 0)
                .eq(Task::getIsDeleted, 0)
                .ne(Task::getStatus, "COMPLETED")
                .ne(Task::getStatus, "CANCELLED")
                .isNotNull(Task::getRemindAt)
                .le(Task::getRemindAt, now)
        );
        for (Task task : remindDue) {
            try {
                notificationService.send(
                    task.getUserId(), null, "REMINDER",
                    "任务临近截止",
                    "任务「" + task.getTitle() + "」即将到期，请尽快处理。"
                        + (task.getDueAt() != null ? "截止时间：" + task.getDueAt() + "。" : ""),
                    task.getId()
                );
                task.setReminderSent(true);
                taskMapper.updateById(task);
                sent++;
            } catch (Exception e) {
                log.error("发送到期提醒失败: taskId={}", task.getId(), e);
            }
        }

        // 2. 逾期提醒: 任务未完成且已到截止时间
        List<Task> expired = taskMapper.selectList(
            new LambdaQueryWrapper<Task>()
                .eq(Task::getReminderSent, 0)
                .eq(Task::getIsDeleted, 0)
                .ne(Task::getStatus, "COMPLETED")
                .and(w -> w.isNotNull(Task::getDueAt).le(Task::getDueAt, now)
                    .or().isNotNull(Task::getDeadline).le(Task::getDeadline, now))
        );
        for (Task task : expired) {
            try {
                notificationService.send(
                    task.getUserId(), null, "REMINDER",
                    "任务已到期",
                    "任务「" + task.getTitle() + "」已到截止时间，请尽快完成或调整安排。",
                    task.getId()
                );
                task.setReminderSent(true);
                taskMapper.updateById(task);
                sent++;
            } catch (Exception e) {
                log.error("发送逾期提醒失败: taskId={}", task.getId(), e);
            }
        }

        if (sent > 0) {
            log.info("ReminderScheduler: sent {} reminders", sent);
        }
        return sent;
    }
}