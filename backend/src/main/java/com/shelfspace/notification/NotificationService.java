package com.shelfspace.notification;

import com.shelfspace.common.dto.PageResponse;
import com.shelfspace.common.dto.SortGuard;
import com.shelfspace.common.exception.ResourceNotFoundException;
import com.shelfspace.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Stores in-app alerts. This is the single place notifications are created, so adding email or push later
 * means adding a sender here, not touching every caller.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final Clock clock;

    @Transactional
    public void notify(Long userId, NotificationType type, String title, String message,
                       String referenceType, Long referenceId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setReferenceType(referenceType);
        n.setReferenceId(referenceId);
        repository.save(n);
        log.debug("Notification {} for user {}: {}", type, userId, title);
    }

    /** Same as notify, but skipped if this alert was already sent today (safe for jobs that run more than once). */
    @Transactional
    public boolean notifyOncePerDay(Long userId, NotificationType type, String title, String message,
                                    String referenceType, Long referenceId) {
        LocalDateTime startOfToday = LocalDate.now(clock).atStartOfDay();
        if (repository.existsByUserIdAndTypeAndReferenceIdAndCreatedAtGreaterThanEqual(
                userId, type, referenceId, startOfToday)) {
            return false;
        }
        notify(userId, type, title, message, referenceType, referenceId);
        return true;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> forUser(Long userId, boolean unreadOnly, Pageable pageable) {
        SortGuard.check(pageable, Set.of());   // always newest first
        var page = unreadOnly
                ? repository.findByUserIdAndReadAtIsNullOrderByIdDesc(userId, pageable)
                : repository.findByUserIdOrderByIdDesc(userId, pageable);
        return PageResponse.from(page.map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long id, Long userId) {
        Notification n = repository.findById(id)
                .filter(x -> x.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        if (n.getReadAt() == null) {
            n.setReadAt(LocalDateTime.now(clock));
        }
        return NotificationResponse.from(n);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return repository.markAllRead(userId, LocalDateTime.now(clock));
    }
}
