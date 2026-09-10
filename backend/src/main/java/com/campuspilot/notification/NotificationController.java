package com.campuspilot.notification;

import com.campuspilot.common.result.ApiResponse;
import com.campuspilot.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationVO>> list() {
        return ApiResponse.success(notificationService.getCurrentUserNotifications());
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount() {
        return ApiResponse.success(Map.of("count", notificationService.unreadCount()));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Map<String, Boolean>> markRead(@PathVariable Long id) {
        return ApiResponse.success(Map.of("ok", notificationService.markRead(id)));
    }

    @PutMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllRead() {
        return ApiResponse.success(Map.of("count", notificationService.markAllRead()));
    }
}