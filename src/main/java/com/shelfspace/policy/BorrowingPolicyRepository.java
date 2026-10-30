package com.shelfspace.policy;

import com.shelfspace.member.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BorrowingPolicyRepository extends JpaRepository<BorrowingPolicy, Long> {

    Optional<BorrowingPolicy> findByMembershipTier(MembershipTier tier);
}
