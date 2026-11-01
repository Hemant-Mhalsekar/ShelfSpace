package com.shelfspace.reservation;

import com.shelfspace.auth.AuthUser;
import com.shelfspace.catalog.Book;
import com.shelfspace.catalog.BookCopy;
import com.shelfspace.catalog.BookCopyRepository;
import com.shelfspace.catalog.BookRepository;
import com.shelfspace.catalog.CopyStatus;
import com.shelfspace.circulation.LoanRepository;
import com.shelfspace.circulation.LoanStatus;
import com.shelfspace.common.exception.BusinessRuleException;
import com.shelfspace.common.exception.DuplicateResourceException;
import com.shelfspace.common.exception.ResourceNotFoundException;
import com.shelfspace.member.MemberStatus;
import com.shelfspace.member.Role;
import com.shelfspace.member.User;
import com.shelfspace.member.UserRepository;
import com.shelfspace.notification.NotificationService;
import com.shelfspace.notification.NotificationType;
import com.shelfspace.policy.BorrowingPolicy;
import com.shelfspace.policy.PolicyService;
import com.shelfspace.reservation.dto.ReservationResponse;
import com.shelfspace.wishlist.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * FIFO waiting list per book.
 *
 * Every place that frees a copy (return, reject, cancel, new copies, repaired copy, expired hold) calls
 * {@link #releaseCopy(BookCopy)}. That gives the copy to the next member in the queue, or makes it AVAILABLE
 * if nobody is waiting, so a member who isn't next can never take a copy someone else is waiting for.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final Set<ReservationStatus> ACTIVE = EnumSet.of(ReservationStatus.WAITING, ReservationStatus.READY);
    private static final Set<LoanStatus> ACTIVE_LOANS = EnumSet.of(
            LoanStatus.REQUESTED, LoanStatus.APPROVED, LoanStatus.ISSUED, LoanStatus.OVERDUE);
    private static final Set<LoanStatus> OUT = EnumSet.of(LoanStatus.ISSUED, LoanStatus.OVERDUE);
    private static final Set<CopyStatus> IN_CIRCULATION = EnumSet.of(CopyStatus.BORROWED, CopyStatus.ON_HOLD);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM, h:mm a");

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final PolicyService policyService;
    private final NotificationService notificationService;
    private final WishlistService wishlistService;
    private final Clock clock;

    // ---------- Member actions ----------

    @Transactional
    public ReservationResponse reserve(Long memberId, Long bookId) {
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("User", memberId));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));
        BorrowingPolicy policy = policyService.forTier(member.getMembershipTier());

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessRuleException("Your account is suspended. Please contact the library.");
        }
        if (loanRepository.existsByMemberIdAndCopyBookIdAndStatusIn(memberId, bookId, ACTIVE_LOANS)) {
            throw new BusinessRuleException("You already have a copy of '" + book.getTitle() + "'");
        }
        if (reservationRepository.existsByMemberIdAndBookIdAndStatusIn(memberId, bookId, ACTIVE)) {
            throw new DuplicateResourceException("You have already reserved '" + book.getTitle() + "'");
        }
        if (copyRepository.countByBookIdAndStatus(bookId, CopyStatus.AVAILABLE) > 0) {
            throw new BusinessRuleException("A copy is available now, so you can borrow it directly");
        }
        if (!copyRepository.existsByBookIdAndStatusIn(bookId, IN_CIRCULATION)) {
            throw new BusinessRuleException("This book has no copies in circulation to wait for");
        }
        if (reservationRepository.countByMemberIdAndStatusIn(memberId, ACTIVE) >= policy.getMaxReservations()) {
            throw new BusinessRuleException("Reservation limit reached (" + policy.getMaxReservations() + ")");
        }

        Reservation reservation = new Reservation();
        reservation.setMember(member);
        reservation.setBook(book);
        reservation.setStatus(ReservationStatus.WAITING);
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse cancel(Long reservationId, AuthUser user) {
        Reservation r = findVisible(reservationId, user);
        if (!ACTIVE.contains(r.getStatus())) {
            throw new BusinessRuleException("Reservation is already " + r.getStatus());
        }
        boolean wasHolding = r.getStatus() == ReservationStatus.READY;
        r.setStatus(ReservationStatus.CANCELLED);
        if (wasHolding) {
            releaseCopy(r.getHeldCopy());
        }
        return toResponse(r);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> mine(Long memberId) {
        return reservationRepository.findByMemberIdOrderByIdDesc(memberId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> activeFor(Long memberId) {
        return reservationRepository.findByMemberIdAndStatusInOrderByIdAsc(memberId, ACTIVE).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(Long id, AuthUser user) {
        return toResponse(findVisible(id, user));
    }

    // ---------- Called by circulation and catalog ----------

    /** Hands a freed copy to the next member in the queue, or makes it AVAILABLE. Joins the caller's transaction. */
    @Transactional
    public void releaseCopy(BookCopy copy) {
        Long bookId = copy.getBook().getId();
        Optional<Reservation> next =
                reservationRepository.findFirstByBookIdAndStatusOrderByIdAsc(bookId, ReservationStatus.WAITING);

        if (next.isEmpty()) {
            boolean wasAllOut = copyRepository.countByBookIdAndStatusAndIdNot(bookId, CopyStatus.AVAILABLE, copy.getId()) == 0;
            copy.setStatus(CopyStatus.AVAILABLE);
            if (wasAllOut) {
                wishlistService.notifyBookAvailable(copy.getBook());   // "back in stock" alert
            }
            return;
        }

        Reservation r = next.get();
        BorrowingPolicy policy = policyService.forTier(r.getMember().getMembershipTier());
        LocalDateTime expires = LocalDateTime.now(clock).plusHours(policy.getHoldHours());

        copy.setStatus(CopyStatus.ON_HOLD);
        r.setStatus(ReservationStatus.READY);
        r.setHeldCopy(copy);
        r.setExpiresAt(expires);

        notificationService.notify(r.getMember().getId(), NotificationType.RESERVATION_READY,
                "'" + r.getBook().getTitle() + "' is ready for you",
                "A copy is being held for you until " + expires.format(WHEN)
                        + ". Request it from the book page before then.",
                "Reservation", r.getId());
    }

    /** A READY reservation this member can turn into a loan, if any. */
    @Transactional(readOnly = true)
    public Optional<Reservation> readyFor(Long memberId, Long bookId) {
        return reservationRepository.findFirstByMemberIdAndBookIdAndStatus(memberId, bookId, ReservationStatus.READY);
    }

    @Transactional
    public void markFulfilled(Reservation reservation) {
        reservation.setStatus(ReservationStatus.FULFILLED);
    }

    // ---------- Job ----------

    /** Expires holds nobody collected in time and passes each copy to the next member. */
    @Scheduled(cron = "${app.jobs.hold-expiry-cron}")
    @Transactional
    public int expireHolds() {
        List<Reservation> expired = reservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.READY, LocalDateTime.now(clock));
        for (Reservation r : expired) {
            r.setStatus(ReservationStatus.EXPIRED);
            notificationService.notify(r.getMember().getId(), NotificationType.HOLD_EXPIRED,
                    "Your hold on '" + r.getBook().getTitle() + "' expired",
                    "The copy wasn't collected in time and has gone to the next person. You can reserve it again.",
                    "Reservation", r.getId());
            releaseCopy(r.getHeldCopy());
        }
        if (!expired.isEmpty()) {
            log.info("Expired {} reservation hold(s)", expired.size());
        }
        return expired.size();
    }

    // ---------- helpers ----------

    private Reservation findVisible(Long id, AuthUser user) {
        Reservation r = reservationRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
        if (user.role() != Role.ADMIN && !r.getMember().getId().equals(user.id())) {
            throw new ResourceNotFoundException("Reservation", id);
        }
        return r;
    }

    private ReservationResponse toResponse(Reservation r) {
        Integer position = null;
        LocalDate estimate = null;
        if (r.getStatus() == ReservationStatus.WAITING) {
            long ahead = reservationRepository.countByBookIdAndStatusAndIdLessThan(
                    r.getBook().getId(), ReservationStatus.WAITING, r.getId());
            position = (int) ahead + 1;
            List<LocalDate> dueDates = loanRepository.findDueDatesForBook(r.getBook().getId(), OUT);
            if (position <= dueDates.size()) {
                estimate = dueDates.get(position - 1);
            }
        }
        boolean ready = r.getStatus() == ReservationStatus.READY;
        return new ReservationResponse(r.getId(), r.getBook().getId(), r.getBook().getTitle(),
                r.getBook().getAuthor(), r.getBook().getIsbn(), r.getStatus(),
                position, estimate,
                ready && r.getHeldCopy() != null ? r.getHeldCopy().getBarcode() : null,
                ready ? r.getExpiresAt() : null,
                r.getCreatedAt());
    }
}
