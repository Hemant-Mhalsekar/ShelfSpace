package com.shelfspace.wishlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {

    boolean existsByMemberIdAndBookId(Long memberId, Long bookId);

    long deleteByMemberIdAndBookId(Long memberId, Long bookId);

    /** Inner join on book, so soft-deleted books drop out of the list automatically. */
    @Query("""
            SELECT w FROM WishlistItem w JOIN FETCH w.book b
            WHERE w.member.id = :memberId
            ORDER BY w.createdAt DESC, w.id DESC
            """)
    List<WishlistItem> findForMember(@Param("memberId") Long memberId);

    @Query("SELECT w.book.id FROM WishlistItem w WHERE w.member.id = :memberId")
    List<Long> findBookIdsForMember(@Param("memberId") Long memberId);

    /** Active members who want this book. */
    @Query("""
            SELECT w.member.id FROM WishlistItem w
            WHERE w.book.id = :bookId AND w.member.status = com.shelfspace.member.MemberStatus.ACTIVE
            """)
    List<Long> findActiveMemberIdsForBook(@Param("bookId") Long bookId);
}
