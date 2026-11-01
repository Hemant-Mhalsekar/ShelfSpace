package com.shelfspace.reservation.dto;

import com.shelfspace.reservation.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * position: place in the queue (1 = next), only while WAITING.
 * estimatedAvailableDate: rough guess from due dates of copies currently out, only while WAITING.
 * heldCopyBarcode / expiresAt: only while READY.
 */
public record ReservationResponse(
        Long id,
        Long bookId,
        String bookTitle,
        ReservationStatus status,
        Integer position,
        LocalDate estimatedAvailableDate,
        String heldCopyBarcode,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
}
