package com.shelfspace.circulation;

import com.shelfspace.audit.AuditService;
import com.shelfspace.auth.AuthUser;
import com.shelfspace.catalog.Book;
import com.shelfspace.catalog.BookCopy;
import com.shelfspace.catalog.BookCopyRepository;
import com.shelfspace.catalog.BookRepository;
import com.shelfspace.catalog.CopyStatus;
import com.shelfspace.circulation.dto.LoanResponse;
import com.shelfspace.circulation.dto.ReturnResponse;
import com.shelfspace.common.dto.PageResponse;
import com.shelfspace.common.dto.SortGuard;
import com.shelfspace.common.exception.BusinessRuleException;
import com.shelfspace.common.exception.ResourceNotFoundException;
import com.shelfspace.fine.FineReason;
import com.shelfspace.fine.FineService;
import com.shelfspace.member.MemberStatus;
import com.shelfspace.member.MembershipTier;
import com.shelfspace.member.Role;
import com.shelfspace.member.User;
import com.shelfspace.member.UserRepository;
import com.shelfspace.policy.BorrowingPolicy;
import com.shelfspace.notification.NotificationService;
import com.shelfspace.notification.NotificationType;
import com.shelfspace.policy.PolicyService;
import com.shelfspace.reservation.Reservation;
import com.shelfspace.reservation.ReservationService;
import com.shelfspace.reservation.ReservationRepository;
import com.shelfspace.reservation.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Borrowing workflow: request -> approve (issue) -> renew -> return / lost.
 * All library rules come from the member's {@link BorrowingPolicy}, not from constants in code.
 */
@Service
@RequiredArgsConstructor
public class LoanService {

    /** Loans that count against the member's limit. */
    private static final Set<LoanStatus> ACTIVE = EnumSet.of(
            LoanStatus.REQUESTED, LoanStatus.APPROVED, LoanStatus.ISSUED, LoanStatus.OVERDUE);

    /** Loans where the member physically has the book. */
    private static final Set<LoanStatus> OUT = EnumSet.of(LoanStatus.ISSUED, LoanStatus.OVERDUE);

    private static final Set<String> SORTABLE = Set.of("requestedAt", "borrowedAt", "dueDate", "returnedAt", "status");

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final PolicyService policyService;
    private final FineService fineService;
    private final AuditService auditService;
    private final ReservationService reservationService;
    private final NotificationService notificationService;
    private final Clock clock;

    // ---------- Member actions ----------

    /**
     * Places a borrow request and puts one copy ON_HOLD for the member.
     *
     * Concurrency: two members can read the same AVAILABLE copy at once. The copy has a @Version column,
     * so the second UPDATE matches 0 rows and fails with an optimistic-lock error (mapped to 409).
     * A copy can therefore never be held for two members.
     */
    @Transactional
    public LoanResponse request(Long memberId, Long bookId) {
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("User", memberId));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));
        BorrowingPolicy policy = policyService.forTier(member.getMembershipTier());

        checkCanBorrow(member, book, policy);

        BookCopy copy;
        Optional<Reservation> ready = reservationService.readyFor(memberId, bookId);
        if (ready.isPresent()) {
            // A copy is already being held for this member from the waiting list.
            copy = ready.get().getHeldCopy();
            reservationService.markFulfilled(ready.get());
        } else {
            copy = copyRepository.findFirstByBookIdAndStatusOrderByIdAsc(bookId, CopyStatus.AVAILABLE)
                    .orElseThrow(() -> new BusinessRuleException(
                            "No copies of '" + book.getTitle() + "' are available right now. You can reserve it."));
            copy.setStatus(CopyStatus.ON_HOLD);
            copyRepository.saveAndFlush(copy);   // optimistic lock is checked here
        }

        Loan loan = new Loan();
        loan.setMember(member);
        loan.setCopy(copy);
        loan.setRequestedAt(LocalDateTime.now(clock));
        return toResponse(loanRepository.save(loan), policy);
    }

    @Transactional
    public LoanResponse cancel(Long loanId, AuthUser user) {
        Loan loan = findVisible(loanId, user);
        if (!loan.getMember().getId().equals(user.id())) {
            throw new BusinessRuleException("Only the member who requested a loan can cancel it");
        }
        loan.transitionTo(LoanStatus.CANCELLED);
        reservationService.releaseCopy(loan.getCopy());
        return toResponse(loan);
    }

    /** Extends the due date by one loan period. Allowed while not overdue, under the renewal limit, and nobody is waiting. */
    @Transactional
    public LoanResponse renew(Long loanId, AuthUser user) {
        Loan loan = findVisible(loanId, user);
        BorrowingPolicy policy = policyService.forTier(loan.getMember().getMembershipTier());
        LocalDate today = LocalDate.now(clock);

        if (loan.getStatus() != LoanStatus.ISSUED) {
            throw new BusinessRuleException("Only issued loans can be renewed (this one is " + loan.getStatus() + ")");
        }
        if (loan.getDueDate().isBefore(today)) {
            throw new BusinessRuleException("Overdue loans cannot be renewed; please return the book");
        }
        if (loan.getRenewCount() >= policy.getMaxRenewals()) {
            throw new BusinessRuleException("Renewal limit reached (" + policy.getMaxRenewals() + ")");
        }
        Long bookId = loan.getCopy().getBook().getId();
        if (reservationRepository.countByBookIdAndStatus(bookId, ReservationStatus.WAITING) > 0) {
            throw new BusinessRuleException("Another member is waiting for this book, so it cannot be renewed");
        }

        loan.setDueDate(loan.getDueDate().plusDays(policy.getLoanDays()));
        loan.setRenewCount(loan.getRenewCount() + 1);
        if (user.role() == Role.ADMIN) {
            auditService.record(user.id(), "LOAN_RENEWED", "Loan", loanId, "newDueDate=" + loan.getDueDate());
        }
        return toResponse(loan, policy);
    }

    // ---------- Admin actions ----------

    /** Approves a request and hands the book over: REQUESTED -> APPROVED -> ISSUED. */
    @Transactional
    public LoanResponse approve(Long loanId, Long adminId) {
        Loan loan = findDetailed(loanId);
        BorrowingPolicy policy = policyService.forTier(loan.getMember().getMembershipTier());

        loan.transitionTo(LoanStatus.APPROVED);
        loan.transitionTo(LoanStatus.ISSUED);
        loan.setApprovedBy(userRepository.getReferenceById(adminId));
        loan.setBorrowedAt(LocalDateTime.now(clock));
        loan.setDueDate(LocalDate.now(clock).plusDays(policy.getLoanDays()));
        loan.getCopy().setStatus(CopyStatus.BORROWED);

        auditService.record(adminId, "LOAN_APPROVED", "Loan", loanId, "dueDate=" + loan.getDueDate());
        notificationService.notify(loan.getMember().getId(), NotificationType.LOAN_APPROVED,
                "'" + loan.getCopy().getBook().getTitle() + "' is ready to collect",
                "Your request was approved. Due back on " + loan.getDueDate() + ".", "Loan", loanId);
        return toResponse(loan, policy);
    }

    @Transactional
    public LoanResponse reject(Long loanId, String reason, Long adminId) {
        Loan loan = findDetailed(loanId);
        loan.transitionTo(LoanStatus.REJECTED);
        reservationService.releaseCopy(loan.getCopy());
        auditService.record(adminId, "LOAN_REJECTED", "Loan", loanId, reason);
        notificationService.notify(loan.getMember().getId(), NotificationType.LOAN_REJECTED,
                "Request for '" + loan.getCopy().getBook().getTitle() + "' was declined",
                reason == null || reason.isBlank() ? "Please contact the library for details." : reason,
                "Loan", loanId);
        return toResponse(loan);
    }

    /** Book comes back: close the loan, free the copy, charge any overdue fine. */
    @Transactional
    public ReturnResponse returnBook(Long loanId, Long adminId) {
        Loan loan = findDetailed(loanId);
        BorrowingPolicy policy = policyService.forTier(loan.getMember().getMembershipTier());
        LocalDate today = LocalDate.now(clock);

        loan.transitionTo(LoanStatus.RETURNED);
        loan.setReturnedAt(LocalDateTime.now(clock));
        reservationService.releaseCopy(loan.getCopy());   // next in the queue, or back on the shelf

        long daysLate = FineCalculator.overdueDays(loan.getDueDate(), today);
        BigDecimal fine = FineCalculator.overdueFine(daysLate, policy.getFinePerDay(), policy.getFineCap());
        fineService.charge(loan, fine, FineReason.OVERDUE);

        auditService.record(adminId, "LOAN_RETURNED", "Loan", loanId, "daysLate=" + daysLate + ", fine=" + fine);
        return new ReturnResponse(toResponse(loan, policy), daysLate, fine);
    }

    /** Book reported lost: charge the replacement cost (or the tier's fine cap if unknown) plus any overdue fine. */
    @Transactional
    public ReturnResponse markLost(Long loanId, Long adminId) {
        Loan loan = findDetailed(loanId);
        BorrowingPolicy policy = policyService.forTier(loan.getMember().getMembershipTier());
        LocalDate today = LocalDate.now(clock);

        loan.transitionTo(LoanStatus.LOST);
        loan.setReturnedAt(LocalDateTime.now(clock));
        loan.getCopy().setStatus(CopyStatus.LOST);

        long daysLate = FineCalculator.overdueDays(loan.getDueDate(), today);
        BigDecimal overdue = FineCalculator.overdueFine(daysLate, policy.getFinePerDay(), policy.getFineCap());
        BigDecimal cost = loan.getCopy().getBook().getReplacementCost();
        BigDecimal replacement = cost != null ? cost : policy.getFineCap();

        fineService.charge(loan, overdue, FineReason.OVERDUE);
        fineService.charge(loan, replacement, FineReason.LOST);

        BigDecimal total = overdue.add(replacement);
        auditService.record(adminId, "LOAN_LOST", "Loan", loanId, "fine=" + total);
        return new ReturnResponse(toResponse(loan, policy), daysLate, total);
    }

    // ---------- Queries ----------

    @Transactional(readOnly = true)
    public LoanResponse get(Long loanId, AuthUser user) {
        return toResponse(findVisible(loanId, user));
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> search(Long memberId, LoanStatus status, Pageable pageable) {
        SortGuard.check(pageable, SORTABLE);
        Map<MembershipTier, BorrowingPolicy> policies = policyService.all();
        return PageResponse.from(loanRepository.search(memberId, status, pageable)
                .map(l -> toResponse(l, policies.get(l.getMember().getMembershipTier()))));
    }

    /** Books the member currently has or has requested, soonest due first. */
    @Transactional(readOnly = true)
    public List<LoanResponse> activeFor(Long memberId) {
        BorrowingPolicy policy = policyForMember(memberId);
        return loanRepository.findByMemberIdAndStatusInOrderByDueDateAsc(memberId, ACTIVE).stream()
                .map(l -> toResponse(l, policy)).toList();
    }

    /** Last five closed loans. */
    @Transactional(readOnly = true)
    public List<LoanResponse> recentHistoryFor(Long memberId) {
        BorrowingPolicy policy = policyForMember(memberId);
        return loanRepository.findTop5ByMemberIdAndStatusInOrderByReturnedAtDesc(
                        memberId, EnumSet.of(LoanStatus.RETURNED, LoanStatus.LOST)).stream()
                .map(l -> toResponse(l, policy)).toList();
    }

    private BorrowingPolicy policyForMember(Long memberId) {
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("User", memberId));
        return policyService.forTier(member.getMembershipTier());
    }

    // ---------- Rules ----------

    private void checkCanBorrow(User member, Book book, BorrowingPolicy policy) {
        Long memberId = member.getId();

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessRuleException("Your account is suspended. Please contact the library.");
        }
        if (loanRepository.existsByMemberIdAndStatusInAndDueDateBefore(memberId, OUT, LocalDate.now(clock))) {
            throw new BusinessRuleException("Please return your overdue books before borrowing more");
        }
        BigDecimal unpaid = fineService.totalUnpaid(memberId);
        if (unpaid.compareTo(policy.getMaxUnpaidFines()) > 0) {
            throw new BusinessRuleException("Unpaid fines of ₹" + unpaid + " exceed the limit of ₹"
                    + policy.getMaxUnpaidFines() + ". Please clear them first.");
        }
        long active = loanRepository.countByMemberIdAndStatusIn(memberId, ACTIVE);
        if (active >= policy.getMaxBooks()) {
            throw new BusinessRuleException("Borrowing limit reached (" + policy.getMaxBooks() + " books)");
        }
        if (loanRepository.existsByMemberIdAndCopyBookIdAndStatusIn(memberId, book.getId(), ACTIVE)) {
            throw new BusinessRuleException("You already have a copy of '" + book.getTitle() + "'");
        }
    }

    // ---------- helpers ----------

    private Loan findDetailed(Long loanId) {
        return loanRepository.findWithDetailsById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
    }

    /** Admins see every loan; members only their own. Others get 404 so loan ids can't be probed. */
    private Loan findVisible(Long loanId, AuthUser user) {
        Loan loan = findDetailed(loanId);
        if (user.role() != Role.ADMIN && !loan.getMember().getId().equals(user.id())) {
            throw new ResourceNotFoundException("Loan", loanId);
        }
        return loan;
    }

    private LoanResponse toResponse(Loan loan) {
        return toResponse(loan, policyService.forTier(loan.getMember().getMembershipTier()));
    }

    private LoanResponse toResponse(Loan loan, BorrowingPolicy policy) {
        Long overdueDays = null;
        BigDecimal accrued = null;
        if (OUT.contains(loan.getStatus())) {
            long days = FineCalculator.overdueDays(loan.getDueDate(), LocalDate.now(clock));
            overdueDays = days;
            accrued = FineCalculator.overdueFine(days, policy.getFinePerDay(), policy.getFineCap());
        }
        BookCopy copy = loan.getCopy();
        Book book = copy.getBook();
        User member = loan.getMember();
        return new LoanResponse(loan.getId(), loan.getStatus(),
                book == null ? null : book.getId(),
                book == null ? "(removed)" : book.getTitle(),
                copy.getBarcode(), member.getId(), member.getName(),
                loan.getRequestedAt(), loan.getBorrowedAt(), loan.getDueDate(), loan.getReturnedAt(),
                loan.getRenewCount(), overdueDays, accrued);
    }
}
