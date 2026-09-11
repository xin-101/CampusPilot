package com.campuspilot.notification;

/**
 * 任务到期/提醒服务
 */
public interface ReminderService {

    /**
     * 扫描到期任务并发送提醒(幂等：reminder_sent=1 后不再发送)
     * @return 发送的提醒数量
     */
    int processDueReminders();
}