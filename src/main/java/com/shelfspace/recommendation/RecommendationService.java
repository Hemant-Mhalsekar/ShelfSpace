package com.shelfspace.recommendation;

import com.shelfspace.catalog.Book;
import com.shelfspace.catalog.BookRepository;
import com.shelfspace.catalog.BookResponses;
import com.shelfspace.catalog.dto.BookResponse;
import com.shelfspace.common.exception.ResourceNotFoundException;
import com.shelfspace.recommendation.dto.RecommendationResponse;
import com.shelfspace.wishlist.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * "Readers also borrowed" (item-to-item co-occurrence), with fallbacks so new books and new members
 * still get suggestions: popular in the same category, then popular overall, then newest.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    public static final int MAX_LIMIT = 12;
    /** NOT IN () is invalid SQL; this id never exists, so it keeps the list non-empty harmlessly. */
    private static final long NO_ID = -1L;

    private final RecommendationRepository repository;
    private final BookRepository bookRepository;
    private final WishlistRepository wishlistRepository;
    private final BookResponses bookResponses;

    @Transactional(readOnly = true)
    public List<RecommendationResponse> forBook(Long bookId, int limit) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book", bookId));
        int n = clamp(limit);
        Picks picks = new Picks(n, Set.of(bookId));

        picks.addScores(repository.coBorrowed(List.of(bookId), picks.excluded(), NO_ID, PageRequest.of(0, n)),
                "Readers of “" + book.getTitle() + "” also borrowed this");
        if (!picks.full()) {
            picks.addScores(repository.popularInCategories(List.of(book.getCategory()), picks.excluded(), PageRequest.of(0, n)),
                    "Popular in " + book.getCategory());
        }
        if (!picks.full()) {
            picks.addBooks(bookRepository.findByCategoryAndIdNotInOrderByCreatedAtDesc(
                    book.getCategory(), picks.excluded(), PageRequest.of(0, n)), "More in " + book.getCategory());
        }
        return picks.resolve();
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> forMember(Long memberId, int limit) {
        int n = clamp(limit);
        // Never suggest what they've already had or already saved
        Set<Long> exclude = new HashSet<>(repository.bookIdsForMember(memberId));
        exclude.addAll(wishlistRepository.findBookIdsForMember(memberId));
        Picks picks = new Picks(n, exclude);

        List<Long> borrowed = repository.borrowedBookIdsForMember(memberId);
        if (!borrowed.isEmpty()) {
            picks.addScores(repository.coBorrowed(borrowed, picks.excluded(), memberId, PageRequest.of(0, n)),
                    "Members who read what you read also borrowed this");
        }
        for (String category : repository.topCategoriesForMember(memberId, PageRequest.of(0, 3))) {
            if (picks.full()) {
                break;
            }
            picks.addScores(repository.popularInCategories(List.of(category), picks.excluded(), PageRequest.of(0, n)),
                    "Popular in " + category + ", a category you read");
        }
        if (!picks.full()) {
            picks.addScores(repository.popularOverall(picks.excluded(), PageRequest.of(0, n)), "Popular in the library");
        }
        if (!picks.full()) {
            picks.addBooks(bookRepository.findByIdNotInOrderByCreatedAtDesc(picks.excluded(), PageRequest.of(0, n)),
                    "New in the library");
        }
        return picks.resolve();
    }

    private static int clamp(int limit) {
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    /** Collects book ids in priority order, remembers the first reason for each, skips duplicates. */
    private final class Picks {
        private final int limit;
        private final Set<Long> excluded = new HashSet<>();
        private final Map<Long, String> picked = new LinkedHashMap<>();

        Picks(int limit, Set<Long> initiallyExcluded) {
            this.limit = limit;
            this.excluded.addAll(initiallyExcluded);
            this.excluded.add(NO_ID);
        }

        boolean full() {
            return picked.size() >= limit;
        }

        Set<Long> excluded() {
            Set<Long> all = new HashSet<>(excluded);
            all.addAll(picked.keySet());
            return all;
        }

        void addScores(List<BookScore> scores, String reason) {
            for (BookScore s : scores) {
                add(s.getBookId(), reason);
            }
        }

        void addBooks(List<Book> books, String reason) {
            for (Book b : books) {
                add(b.getId(), reason);
            }
        }

        private void add(Long bookId, String reason) {
            if (!full() && !excluded.contains(bookId)) {
                picked.putIfAbsent(bookId, reason);
            }
        }

        List<RecommendationResponse> resolve() {
            if (picked.isEmpty()) {
                return List.of();
            }
            Map<Long, Book> books = bookRepository.findAllById(picked.keySet()).stream()
                    .collect(Collectors.toMap(Book::getId, Function.identity()));
            List<Book> ordered = picked.keySet().stream().filter(books::containsKey).map(books::get).toList();
            List<BookResponse> responses = bookResponses.of(ordered);
            List<RecommendationResponse> out = new ArrayList<>(responses.size());
            for (BookResponse r : responses) {
                out.add(new RecommendationResponse(r, picked.get(r.id())));
            }
            return out;
        }
    }
}
