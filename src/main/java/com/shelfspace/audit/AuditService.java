package com.shelfspace.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records admin actions. Runs inside the caller's transaction, so a rolled-back change leaves no audit row. */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long actorId, String action, String entityType, Long entityId, String details) {
        String trimmed = details != null && details.length() > 500 ? details.substring(0, 500) : details;
        auditLogRepository.save(new AuditLog(actorId, action, entityType, entityId, trimmed));
    }
}
