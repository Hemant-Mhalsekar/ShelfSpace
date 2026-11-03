package com.shelfspace.wishlist;

import com.shelfspace.catalog.Book;
import com.shelfspace.catalog.BookRepository;
import com.shelfspace.catalog.BookResponses;
import com.shelfspace.catalog.dto.BookResponse;
import com.shelfspace.common.exception.DuplicateResourceException;
import com.shelfspace.common.exception.ResourceNotFoundException;
import com.shelfspace.member.UserRepository;
import com.shelfspace.notification.NotificationService;
import com.shelfspace.notification.NotificationType;
import com.shelfspace.wishlist.dto.WishlistItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookResponses bookResponses;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<WishlistItemResponse> forMember(Long memberId) {
        List<WishlistItem> items = wishlistRepository.findForMember(memberId);
        List<BookResponse> books = bookResponses.of(items.stream().map(WishlistItem::getBook).toList());
        return java.util.stream.IntStream.range(0, items.size())
                .mapToObj(i -> new WishlistItemResponse(books.get(i), items.get(i).getCreatedAt()))
                .toList();
    }

    @Transactional
    public WishlistItemResponse add(Long memberId, Long bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book", bookId));
        if (wishlistRepository.existsByMemberIdAndBookId(memberId, bookId)) {
            throw new DuplicateResourceException("'" + book.getTitle() + "' is already on your wishlist");
        }
        WishlistItem item = wishlistRepository.save(new WishlistItem(userRepository.getReferenceById(memberId), book));
        return new WishlistItemResponse(bookResponses.of(book), item.getCreatedAt());
    }

    @Transactional
    public void remove(Long memberId, Long bookId) {
        if (wishlistRepository.deleteByMemberIdAndBookId(memberId, bookId) == 0) {
            throw new ResourceNotFoundException("Wishlist item for book", bookId);
        }
    }

    /**
     * Called when a copy goes back on the shelf with nobody in the reservation queue.
     * Tells everyone who wishlisted the book, at most once per member per book per day.
     */
    @Transactional
    public void notifyBookAvailable(Book book) {
        for (Long memberId : wishlistRepository.findActiveMemberIdsForBook(book.getId())) {
            notificationService.notifyOncePerDay(memberId, NotificationType.WISHLIST_AVAILABLE,
                    "'" + book.getTitle() + "' from your wishlist is available",
                    "A copy is back on the shelf. Borrow it before someone else does!",
                    "Book", book.getId());
        }
    }
}
