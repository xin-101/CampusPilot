package com.campuspilot.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 提醒调度器：每分钟扫描一次到期/逾期任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderService reminderService;

    @Scheduled(cron = "0 * * * * *")
    public void run() {
        try {
            reminderService.processDueReminders();
        } catch (Exception e) {
            log.error("ReminderScheduler异常", e);
        }
    }
}