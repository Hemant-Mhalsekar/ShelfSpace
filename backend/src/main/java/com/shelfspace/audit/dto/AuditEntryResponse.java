package com.shelfspace.audit.dto;

import java.time.LocalDateTime;

/**
 * One audit row, with names filled in so it reads as a sentence.
 *
 * @param entityLabel what was acted on, e.g. a book title, a member's name, or "Clean Code · Priya Sharma" for a loan
 * @param bookId      set when the entry relates to a book, so the UI can link to it
 * @param memberId    set when the entry relates to a member
 */
public record AuditEntryResponse(
        Long id,
        LocalDateTime createdAt,
        String action,
        Long actorId,
        String actorName,
        String entityType,
        Long entityId,
        String entityLabel,
        Long bookId,
        Long memberId,
        String details
) {
}
