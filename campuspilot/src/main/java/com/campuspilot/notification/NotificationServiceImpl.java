package com.campuspilot.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campuspilot.entity.Notification;
import com.campuspilot.entity.User;
import com.campuspilot.mapper.NotificationMapper;
import com.campuspilot.mapper.UserMapper;
import com.campuspilot.security.SecurityUtils;
import com.campuspilot.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    @Override
    public NotificationVO send(Long userId, String studentId, String type, String title, String content, Long relatedId) {
        Notification notification = Notification.builder()
            .userId(userId)
            .studentId(studentId)
            .type(type)
            .title(title)
            .content(content)
            .relatedId(relatedId)
            .isRead(false)
            .build();
        notificationMapper.insert(notification);
        log.info("Notification sent: userId={}, type={}, title={}", userId, type, title);
        return convertToVO(notification);
    }

    @Override
    public List<NotificationVO> getCurrentUserNotifications() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return List.of();
        }
        List<Notification> list = notificationMapper.selectList(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt)
        );
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public boolean markRead(Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            return false;
        }
        Long userId = getCurrentUserId();
        if (userId != null && !userId.equals(notification.getUserId())) {
            return false;
        }
        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationMapper.updateById(notification);
        }
        return true;
    }

    @Override
    public int markAllRead() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return 0;
        }
        List<Notification> unread = notificationMapper.selectList(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false)
        );
        int count = 0;
        for (Notification n : unread) {
            n.setIsRead(true);
            n.setReadAt(LocalDateTime.now());
            notificationMapper.updateById(n);
            count++;
        }
        return count;
    }

    @Override
    public long unreadCount() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return 0;
        }
        return notificationMapper.selectCount(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false)
        );
    }

    private Long getCurrentUserId() {
        String role = SecurityUtils.getCurrentRole();
        if (role != null && !role.contains("STUDENT")) {
            // 教职工/管理员：不做归属过滤(仅按需)
            String username = SecurityUtils.getCurrentUsername();
            return username != null ? lookupUserId(username) : null;
        }
        String username = SecurityUtils.getCurrentUsername();
        return username != null ? lookupUserId(username) : null;
    }

    private Long lookupUserId(String username) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getIsDeleted, 0)
        );
        return user != null ? user.getId() : null;
    }

    private NotificationVO convertToVO(Notification n) {
        return NotificationVO.builder()
            .id(n.getId())
            .userId(n.getUserId())
            .studentId(n.getStudentId())
            .type(n.getType())
            .title(n.getTitle())
            .content(n.getContent())
            .relatedId(n.getRelatedId())
            .isRead(n.getIsRead())
            .readAt(n.getReadAt())
            .createdAt(n.getCreatedAt())
            .build();
    }
}