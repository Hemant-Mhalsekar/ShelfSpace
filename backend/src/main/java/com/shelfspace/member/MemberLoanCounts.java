package com.shelfspace.member;

/** Per-member loan counts for a page of members, from one grouped query. */
public interface MemberLoanCounts {

    Long getMemberId();

    Long getActive();

    Long getOverdue();
}
