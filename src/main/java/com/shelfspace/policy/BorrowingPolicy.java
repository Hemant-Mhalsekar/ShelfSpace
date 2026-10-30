package com.shelfspace.policy;

import com.shelfspace.common.entity.BaseEntity;
import com.shelfspace.member.MembershipTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/** Borrowing rules per membership tier, stored as data so they change without a redeploy. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "borrowing_policies")
public class BorrowingPolicy extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "membership_tier", nullable = false, unique = true, length = 20)
    private MembershipTier membershipTier;

    @Column(name = "max_books", nullable = false)
    private int maxBooks;

    @Column(name = "loan_days", nullable = false)
    private int loanDays;

    @Column(name = "max_renewals", nullable = false)
    private int maxRenewals;

    @Column(name = "fine_per_day", nullable = false, precision = 10, scale = 2)
    private BigDecimal finePerDay;

    @Column(name = "fine_cap", nullable = false, precision = 10, scale = 2)
    private BigDecimal fineCap;

    /** Members owing more than this cannot borrow. */
    @Column(name = "max_unpaid_fines", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxUnpaidFines;
}
