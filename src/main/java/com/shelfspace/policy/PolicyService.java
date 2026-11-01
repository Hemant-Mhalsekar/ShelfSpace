package com.shelfspace.policy;

import com.shelfspace.member.MembershipTier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final BorrowingPolicyRepository repository;

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
}
