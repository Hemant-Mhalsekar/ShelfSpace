package com.shelfspace.reservation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Queue order is by id: ids only ever increase, so the lowest id in WAITING is first in line. */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findFirstByBookIdAndStatusOrderByIdAsc(Long bookId, ReservationStatus status);

    long countByBookIdAndStatus(Long bookId, ReservationStatus status);

    long countByStatus(ReservationStatus status);

    /** People ahead of this reservation in the queue. */
    long countByBookIdAndStatusAndIdLessThan(Long bookId, ReservationStatus status, Long id);

    boolean existsByMemberIdAndBookIdAndStatusIn(Long memberId, Long bookId, Collection<ReservationStatus> statuses);

    long countByMemberIdAndStatusIn(Long memberId, Collection<ReservationStatus> statuses);

    Optional<Reservation> findFirstByMemberIdAndBookIdAndStatus(Long memberId, Long bookId, ReservationStatus status);

    @EntityGraph(attributePaths = {"member", "book", "heldCopy"})
    Optional<Reservation> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"member", "book", "heldCopy"})
    List<Reservation> findByMemberIdAndStatusInOrderByIdAsc(Long memberId, Collection<ReservationStatus> statuses);

    @EntityGraph(attributePaths = {"member", "book", "heldCopy"})
    List<Reservation> findByMemberIdOrderByIdDesc(Long memberId);

    @EntityGraph(attributePaths = {"member", "book", "heldCopy"})
    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime time);
}
