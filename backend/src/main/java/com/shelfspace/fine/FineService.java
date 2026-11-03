package com.shelfspace.fine;

import com.shelfspace.audit.AuditService;
import com.shelfspace.circulation.Loan;
import com.shelfspace.common.dto.PageResponse;
import com.shelfspace.common.dto.SortGuard;
import com.shelfspace.common.exception.BusinessRuleException;
import com.shelfspace.common.exception.ResourceNotFoundException;
import com.shelfspace.fine.dto.FineResponse;
import com.shelfspace.fine.dto.FineSummaryResponse;
import com.shelfspace.member.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FineService {

    private static final Set<String> SORTABLE = Set.of("createdAt", "amount", "status");

    private final FineRepository fineRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final Clock clock;

    /** Called by circulation when a loan closes. Joins the caller's transaction. Zero amounts are ignored. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Fine charge(Loan loan, BigDecimal amount, FineReason reason) {
        if (amount == null || amount.signum() <= 0) {
            return null;
        }
        Fine fine = new Fine();
        fine.setLoan(loan);
        fine.setAmount(amount);
        fine.setReason(reason);
        fine.setStatus(FineStatus.UNPAID);
        return fineRepository.save(fine);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalUnpaid(Long memberId) {
        return fineRepository.totalUnpaidForMember(memberId);
    }

    @Transactional(readOnly = true)
    public FineSummaryResponse forMember(Long memberId) {
        var fines = fineRepository.findByLoanMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(FineResponse::from)
                .toList();
        return new FineSummaryResponse(totalUnpaid(memberId), fines);
    }

    @Transactional(readOnly = true)
    public PageResponse<FineResponse> search(Long memberId, FineStatus status, Pageable pageable) {
        SortGuard.check(pageable, SORTABLE);
        return PageResponse.from(fineRepository.search(memberId, status, pageable).map(FineResponse::from));
    }

    @Transactional
    public FineResponse pay(Long fineId, Long adminId) {
        Fine fine = findUnpaid(fineId);
        fine.setStatus(FineStatus.PAID);
        fine.setPaidAt(LocalDateTime.now(clock));
        auditService.record(adminId, "FINE_PAID", "Fine", fineId, "amount=" + fine.getAmount());
        return FineResponse.from(fine);
    }

    @Transactional
    public FineResponse waive(Long fineId, String note, Long adminId) {
        Fine fine = findUnpaid(fineId);
        fine.setStatus(FineStatus.WAIVED);
        fine.setWaivedBy(userRepository.getReferenceById(adminId));
        fine.setNote(note.trim());
        auditService.record(adminId, "FINE_WAIVED", "Fine", fineId,
                "amount=" + fine.getAmount() + ", note=" + note.trim());
        return FineResponse.from(fine);
    }

    private Fine findUnpaid(Long fineId) {
        Fine fine = fineRepository.findWithDetailsById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine", fineId));
        if (fine.getStatus() != FineStatus.UNPAID) {
            throw new BusinessRuleException("Fine is already " + fine.getStatus());
        }
        return fine;
    }
}
