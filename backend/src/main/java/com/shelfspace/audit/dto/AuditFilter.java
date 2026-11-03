package com.shelfspace.audit.dto;

import com.shelfspace.audit.AuditCategory;

import java.time.LocalDate;

/** Every field is optional. Dates are inclusive and in the server's time zone. */
public record AuditFilter(
        AuditCategory category,
        String action,
        Long actorId,
        String entityType,
        Long entityId,
        LocalDate from,
        LocalDate to
) {
}
