package com.shelfspace.dashboard;

import com.shelfspace.catalog.BookCopyRepository;
import com.shelfspace.catalog.BookRepository;
import com.shelfspace.catalog.CopyStatus;
import com.shelfspace.circulation.LoanRepository;
import com.shelfspace.circulation.LoanService;
import com.shelfspace.circulation.LoanStatus;
import com.shelfspace.circulation.dto.LoanResponse;
import com.shelfspace.dashboard.dto.AdminDashboardResponse;
import com.shelfspace.dashboard.dto.AdminDashboardResponse.Circulation;
import com.shelfspace.dashboard.dto.AdminDashboardResponse.Fines;
import com.shelfspace.dashboard.dto.AdminDashboardResponse.Inventory;
import com.shelfspace.dashboard.dto.AdminDashboardResponse.MonthStat;
import com.shelfspace.dashboard.dto.AdminDashboardResponse.TopBook;
import com.shelfspace.dashboard.dto.MemberDashboardResponse;
import com.shelfspace.fine.FineRepository;
import com.shelfspace.fine.FineService;
import com.shelfspace.member.MemberStatus;
import com.shelfspace.member.Role;
import com.shelfspace.member.UserRepository;
import com.shelfspace.notification.NotificationService;
import com.shelfspace.policy.PolicyService;
import com.shelfspace.reservation.ReservationRepository;
import com.shelfspace.reservation.ReservationService;
import com.shelfspace.reservation.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int MONTHS = 6;

    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final LoanRepository loanRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;
    private final UserRepository userRepository;
    private final StatsRepository statsRepository;
    private final LoanService loanService;
    private final ReservationService reservationService;
    private final FineService fineService;
    private final NotificationService notificationService;
    private final PolicyService policyService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AdminDashboardResponse admin() {
        LocalDate today = LocalDate.now(clock);

        var inventory = new Inventory(
                bookRepository.count(),
                copyRepository.count(),
                copyRepository.countByStatus(CopyStatus.AVAILABLE),
                copyRepository.countByStatus(CopyStatus.DAMAGED),
                copyRepository.countByStatus(CopyStatus.LOST));

        var circulation = new Circulation(
                loanRepository.countByStatusIn(EnumSet.of(LoanStatus.ISSUED, LoanStatus.OVERDUE)),
                // counts loans past due even if tonight's overdue job hasn't flagged them yet
                loanRepository.countByStatusInAndDueDateBefore(EnumSet.of(LoanStatus.ISSUED, LoanStatus.OVERDUE), today),
                loanRepository.countByStatus(LoanStatus.REQUESTED),
                reservationRepository.countByStatus(ReservationStatus.WAITING));

        var fines = new Fines(
                orZero(fineRepository.sumAllUnpaid()),
                orZero(fineRepository.sumPaidSince(today.withDayOfMonth(1).atStartOfDay())));

        List<TopBook> top = statsRepository.mostBorrowed(PageRequest.of(0, 5)).stream()
                .map(b -> new TopBook(b.getBookId(), b.getTitle(), b.getAuthor(), b.getTimesBorrowed()))
                .toList();

        return new AdminDashboardResponse(inventory, circulation, fines,
                userRepository.countByRoleAndStatus(Role.MEMBER, MemberStatus.ACTIVE),
                top, borrowsPerMonth(today));
    }

    @Transactional(readOnly = true)
    public MemberDashboardResponse member(Long memberId) {
        LocalDate today = LocalDate.now(clock);
        List<LoanResponse> current = loanService.activeFor(memberId);

        int out = 0, dueSoon = 0, overdue = 0;
        for (LoanResponse l : current) {
            if (l.status() == LoanStatus.ISSUED || l.status() == LoanStatus.OVERDUE) {
                out++;
                if (l.dueDate().isBefore(today)) {
                    overdue++;
                } else if (!l.dueDate().isAfter(today.plusDays(3))) {
                    dueSoon++;
                }
            }
        }

        var member = userRepository.findById(memberId).orElseThrow();
        int allowed = policyService.forTier(member.getMembershipTier()).getMaxBooks();

        var summary = new MemberDashboardResponse.Summary(out, allowed, dueSoon, overdue,
                fineService.totalUnpaid(memberId), notificationService.unreadCount(memberId));

        return new MemberDashboardResponse(summary, current,
                reservationService.activeFor(memberId), loanService.recentHistoryFor(memberId));
    }

    /** Last N months including the current one, oldest first, with empty months filled in as 0. */
    private List<MonthStat> borrowsPerMonth(LocalDate today) {
        YearMonth current = YearMonth.from(today);
        YearMonth first = current.minusMonths(MONTHS - 1);

        Map<YearMonth, Long> counts = new HashMap<>();
        for (MonthlyCount m : statsRepository.borrowsPerMonth(first.atDay(1).atStartOfDay())) {
            counts.put(YearMonth.of(m.getYr(), m.getMon()), m.getTotal());
        }

        List<MonthStat> result = new ArrayList<>(MONTHS);
        for (YearMonth ym = first; !ym.isAfter(current); ym = ym.plusMonths(1)) {
            result.add(new MonthStat(ym.toString(), counts.getOrDefault(ym, 0L)));
        }
        return result;
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
