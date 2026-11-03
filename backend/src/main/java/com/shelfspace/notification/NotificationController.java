package com.shelfspace.notification;

import com.shelfspace.auth.AuthUser;
import com.shelfspace.common.dto.PageResponse;
import com.shelfspace.notification.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Notifications", description = "Dashboard alerts for the logged-in user")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "My notifications, newest first")
    @GetMapping("/me")
    public PageResponse<NotificationResponse> mine(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                                   @PageableDefault(size = 20) Pageable pageable,
                                                   @AuthenticationPrincipal AuthUser user) {
        return notificationService.forUser(user.id(), unreadOnly, pageable);
    }

    @Operation(summary = "Number of unread notifications (for the bell icon)")
    @GetMapping("/me/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AuthUser user) {
        return Map.of("unread", notificationService.unreadCount(user.id()));
    }

    @Operation(summary = "Mark one notification as read")
    @PostMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return notificationService.markRead(id, user.id());
    }

    @Operation(summary = "Mark all my notifications as read")
    @PostMapping("/me/read-all")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal AuthUser user) {
        return Map.of("updated", notificationService.markAllRead(user.id()));
    }
}
