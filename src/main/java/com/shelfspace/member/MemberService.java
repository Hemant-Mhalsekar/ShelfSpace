package com.shelfspace.member;

import com.shelfspace.audit.AuditService;
import com.shelfspace.circulation.LoanRepository;
import com.shelfspace.circulation.LoanService;
import com.shelfspace.circulation.LoanStatus;
import com.shelfspace.common.dto.PageResponse;
import com.shelfspace.common.dto.SortGuard;
import com.shelfspace.common.exception.BusinessRuleException;
import com.shelfspace.common.exception.DuplicateResourceException;
import com.shelfspace.common.exception.ResourceNotFoundException;
import com.shelfspace.fine.FineRepository;
import com.shelfspace.member.dto.CreateMemberRequest;
import com.shelfspace.member.dto.MemberDetailResponse;
import com.shelfspace.member.dto.MemberSummaryResponse;
import com.shelfspace.member.dto.UpdateMemberRequest;
import com.shelfspace.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Librarian-side member management. Every change is written to the audit log. */
@Service
@RequiredArgsConstructor
public class MemberService {

    private static final Set<LoanStatus> ACTIVE = EnumSet.of(
            LoanStatus.REQUESTED, LoanStatus.APPROVED, LoanStatus.ISSUED, LoanStatus.OVERDUE);
    private static final Set<String> SORTABLE = Set.of("name", "email", "createdAt", "membershipTier", "status");

    private final UserRepository userRepository;
    private final LoanRepository loanRepository;
    private final FineRepository fineRepository;
    private final LoanService loanService;
    private final ReservationService reservationService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<MemberSummaryResponse> search(String q, Role role, MemberStatus status, MembershipTier tier,
                                                      Pageable pageable) {
        SortGuard.check(pageable, SORTABLE);
        Page<User> page = userRepository.search(blankToNull(q), role, status, tier, pageable);
        List<Long> ids = page.map(User::getId).getContent();
        if (ids.isEmpty()) {
            return PageResponse.from(page.map(u -> MemberSummaryResponse.from(u, 0, 0, BigDecimal.ZERO)));
        }

        // Two grouped queries for the whole page instead of two queries per member
        Map<Long, MemberLoanCounts> loans = loanRepository.countsForMembers(ids, ACTIVE, LocalDate.now(clock))
                .stream().collect(Collectors.toMap(MemberLoanCounts::getMemberId, Function.identity()));
        Map<Long, BigDecimal> fines = fineRepository.unpaidTotalsForMembers(ids)
                .stream().collect(Collectors.toMap(MemberFineTotal::getMemberId, MemberFineTotal::getTotal));

        return PageResponse.from(page.map(u -> {
            MemberLoanCounts c = loans.get(u.getId());
            return MemberSummaryResponse.from(u,
                    c == null ? 0 : c.getActive(),
                    c == null || c.getOverdue() == null ? 0 : c.getOverdue(),
                    fines.getOrDefault(u.getId(), BigDecimal.ZERO));
        }));
    }

    @Transactional(readOnly = true)
    public MemberDetailResponse get(Long id) {
        User user = find(id);
        return new MemberDetailResponse(summary(user),
                loanService.activeFor(id), reservationService.activeFor(id), loanService.recentHistoryFor(id));
    }

    @Transactional
    public MemberSummaryResponse create(CreateMemberRequest req, Long adminId) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPhone(req.phone());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role() == null ? Role.MEMBER : req.role());
        user.setMembershipTier(req.membershipTier());
        user.setStatus(MemberStatus.ACTIVE);
        user = userRepository.save(user);

        auditService.record(adminId, "MEMBER_CREATED", "User", user.getId(),
                "email=" + email + ", role=" + user.getRole() + ", tier=" + user.getMembershipTier());
        return summary(user);
    }

    @Transactional
    public MemberSummaryResponse update(Long id, UpdateMemberRequest req, Long adminId) {
        User user = find(id);
        MembershipTier oldTier = user.getMembershipTier();

        user.setName(req.name().trim());
        user.setPhone(req.phone());
        user.setMembershipTier(req.membershipTier());

        String details = Objects.equals(oldTier, req.membershipTier())
                ? "profile updated"
                : "tier " + oldTier + " -> " + req.membershipTier();
        auditService.record(adminId, "MEMBER_UPDATED", "User", id, details);
        return summary(user);
    }

    /** Suspended members can't log in, borrow or reserve. Books they already have can still be returned. */
    @Transactional
    public MemberSummaryResponse suspend(Long id, String reason, Long adminId) {
        if (id.equals(adminId)) {
            throw new BusinessRuleException("You can't suspend your own account");
        }
        User user = find(id);
        if (user.getStatus() == MemberStatus.SUSPENDED) {
            throw new BusinessRuleException("Member is already suspended");
        }
        user.setStatus(MemberStatus.SUSPENDED);
        auditService.record(adminId, "MEMBER_SUSPENDED", "User", id, reason.trim());
        return summary(user);
    }

    @Transactional
    public MemberSummaryResponse activate(Long id, Long adminId) {
        User user = find(id);
        if (user.getStatus() == MemberStatus.ACTIVE) {
            throw new BusinessRuleException("Member is already active");
        }
        user.setStatus(MemberStatus.ACTIVE);
        auditService.record(adminId, "MEMBER_ACTIVATED", "User", id, null);
        return summary(user);
    }

    // ---------- helpers ----------

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Member", id));
    }

    private MemberSummaryResponse summary(User u) {
        List<MemberLoanCounts> counts = u.getId() == null ? List.of()
                : loanRepository.countsForMembers(List.of(u.getId()), ACTIVE, LocalDate.now(clock));
        MemberLoanCounts c = counts.isEmpty() ? null : counts.get(0);
        return MemberSummaryResponse.from(u,
                c == null ? 0 : c.getActive(),
                c == null || c.getOverdue() == null ? 0 : c.getOverdue(),
                fineRepository.totalUnpaidForMember(u.getId()));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
