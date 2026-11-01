package com.shelfspace.catalog;

import com.shelfspace.catalog.dto.BookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Turns a list of books into responses with copy counts using one grouped query.
 * Lives apart from BookService so other modules (wishlist, recommendations) can use it without circular dependencies.
 */
@Component
@RequiredArgsConstructor
public class BookResponses {

    private final BookCopyRepository copyRepository;

    @Transactional(readOnly = true)
    public List<BookResponse> of(List<Book> books) {
        if (books.isEmpty()) {
            return List.of();
        }
        Map<Long, CopyCounts> counts = copyRepository.countByBookIds(books.stream().map(Book::getId).toList())
                .stream().collect(Collectors.toMap(CopyCounts::getBookId, Function.identity()));
        return books.stream().map(b -> {
            CopyCounts c = counts.get(b.getId());
            long total = c == null ? 0 : c.getTotal();
            long available = c == null || c.getAvailable() == null ? 0 : c.getAvailable();
            return BookResponse.from(b, total, available);
        }).toList();
    }

    public BookResponse of(Book book) {
        return of(List.of(book)).get(0);
    }
}
