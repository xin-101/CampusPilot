package com.campuspilot.notification;

import com.campuspilot.vo.NotificationVO;

import java.util.List;

public interface NotificationService {

    /**
     * 发送通知(后台任务/工作流使用，直接指定接收用户)
     */
    NotificationVO send(Long userId, String studentId, String type, String title, String content, Long relatedId);

    /**
     * 获取当前用户通知列表(学生只能看自己的)
     */
    List<NotificationVO> getCurrentUserNotifications();

    /**
     * 标记已读(带归属校验)
     */
    boolean markRead(Long id);

    /**
     * 全部标记已读
     */
    int markAllRead();

    /**
     * 未读数
     */
    long unreadCount();
}