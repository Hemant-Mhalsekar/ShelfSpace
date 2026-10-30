package com.shelfspace.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** Next member in the FIFO queue for a title. */
    Optional<Reservation> findFirstByBookIdAndStatusOrderByCreatedAtAsc(Long bookId, ReservationStatus status);

    long countByBookIdAndStatus(Long bookId, ReservationStatus status);

    boolean existsByMemberIdAndBookIdAndStatusIn(Long memberId, Long bookId, List<ReservationStatus> statuses);

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime time);
}
