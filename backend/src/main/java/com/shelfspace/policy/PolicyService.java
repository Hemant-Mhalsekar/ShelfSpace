package com.shelfspace.policy;

import com.shelfspace.audit.AuditService;
import com.shelfspace.common.exception.InvalidRequestException;
import com.shelfspace.member.MembershipTier;
import com.shelfspace.policy.dto.PolicyResponse;
import com.shelfspace.policy.dto.PolicyUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final BorrowingPolicyRepository repository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public BorrowingPolicy forTier(MembershipTier tier) {
        return repository.findByMembershipTier(tier)
                .orElseThrow(() -> new IllegalStateException("No borrowing policy configured for tier " + tier));
    }

    /** All policies keyed by tier; used when mapping a page of loans from different tiers. */
    @Transactional(readOnly = true)
    public Map<MembershipTier, BorrowingPolicy> all() {
        Map<MembershipTier, BorrowingPolicy> map = new EnumMap<>(MembershipTier.class);
        repository.findAll().forEach(p -> map.put(p.getMembershipTier(), p));
        return map;
    }

    @Transactional(readOnly = true)
    public List<PolicyResponse> list() {
        return repository.findAll(Sort.by("id")).stream().map(PolicyResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PolicyResponse get(MembershipTier tier) {
        return PolicyResponse.from(forTier(tier));
    }

    /**
     * Replaces a tier's rules. Applies to new loans, renewals and fines from now on;
     * due dates already set on existing loans don't change.
     */
    @Transactional
    public PolicyResponse update(MembershipTier tier, PolicyUpdateRequest req, Long adminId) {
        if (req.fineCap().compareTo(req.finePerDay()) < 0) {
            throw new InvalidRequestException("fineCap can't be lower than finePerDay");
        }
        BorrowingPolicy p = forTier(tier);
        List<String> changes = new ArrayList<>();

        p.setMaxBooks(track(changes, "maxBooks", p.getMaxBooks(), req.maxBooks()));
        p.setLoanDays(track(changes, "loanDays", p.getLoanDays(), req.loanDays()));
        p.setMaxRenewals(track(changes, "maxRenewals", p.getMaxRenewals(), req.maxRenewals()));
        p.setFinePerDay(trackMoney(changes, "finePerDay", p.getFinePerDay(), req.finePerDay()));
        p.setFineCap(trackMoney(changes, "fineCap", p.getFineCap(), req.fineCap()));
        p.setMaxUnpaidFines(trackMoney(changes, "maxUnpaidFines", p.getMaxUnpaidFines(), req.maxUnpaidFines()));
        p.setMaxReservations(track(changes, "maxReservations", p.getMaxReservations(), req.maxReservations()));
        p.setHoldHours(track(changes, "holdHours", p.getHoldHours(), req.holdHours()));

        if (!changes.isEmpty()) {
            auditService.record(adminId, "POLICY_UPDATED", "BorrowingPolicy", p.getId(),
                    tier + ": " + String.join(", ", changes));
        }
        return PolicyResponse.from(p);
    }

    private static int track(List<String> changes, String field, int oldValue, int newValue) {
        if (oldValue != newValue) {
            changes.add(field + " " + oldValue + " -> " + newValue);
        }
        return newValue;
    }

    private static java.math.BigDecimal trackMoney(List<String> changes, String field,
                                                   java.math.BigDecimal oldValue, java.math.BigDecimal newValue) {
        if (!Objects.equals(oldValue == null ? null : oldValue.stripTrailingZeros(), newValue.stripTrailingZeros())) {
            changes.add(field + " " + oldValue + " -> " + newValue);
        }
        return newValue;
    }
}
