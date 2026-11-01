package com.shelfspace.circulation;

import com.shelfspace.notification.NotificationService;
import com.shelfspace.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Nightly circulation jobs. Each is safe to run more than once: overdue marking only touches ISSUED loans,
 * and reminders are de-duplicated per day.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CirculationJobs {

    private final LoanRepository loanRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    /** ISSUED loans past their due date become OVERDUE, and the member is told. */
    @Scheduled(cron = "${app.jobs.overdue-cron}")
    @Transactional
    public int markOverdueLoans() {
        LocalDate today = LocalDate.now(clock);
        List<Loan> late = loanRepository.findByStatusAndDueDateBefore(LoanStatus.ISSUED, today);
        for (Loan loan : late) {
            loan.transitionTo(LoanStatus.OVERDUE);
            notificationService.notifyOncePerDay(loan.getMember().getId(), NotificationType.OVERDUE,
                    "'" + loan.getCopy().getBook().getTitle() + "' is overdue",
                    "It was due on " + loan.getDueDate() + ". Fines are adding up daily; please return it soon.",
                    "Loan", loan.getId());
        }
        if (!late.isEmpty()) {
            log.info("Marked {} loan(s) overdue", late.size());
        }
        return late.size();
    }

    /** "Due tomorrow" reminders. Returns how many were actually sent (duplicates on the same day are skipped). */
    @Scheduled(cron = "${app.jobs.due-reminder-cron}")
    @Transactional
    public int sendDueTomorrowReminders() {
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        int sent = 0;
        for (Loan loan : loanRepository.findByStatusAndDueDate(LoanStatus.ISSUED, tomorrow)) {
            boolean created = notificationService.notifyOncePerDay(loan.getMember().getId(), NotificationType.DUE_SOON,
                    "Reminder: '" + loan.getCopy().getBook().getTitle() + "' is due tomorrow",
                    "Please return or renew it by " + loan.getDueDate() + " to avoid a fine.",
                    "Loan", loan.getId());
            if (created) {
                sent++;
            }
        }
        if (sent > 0) {
            log.info("Sent {} due-tomorrow reminder(s)", sent);
        }
        return sent;
    }
}
