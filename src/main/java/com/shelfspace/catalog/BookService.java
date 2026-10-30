package com.shelfspace.catalog;

import com.shelfspace.audit.AuditService;
import com.shelfspace.catalog.dto.BookRequest;
import com.shelfspace.catalog.dto.BookResponse;
import com.shelfspace.catalog.dto.CopyResponse;
import com.shelfspace.common.dto.PageResponse;
import com.shelfspace.common.exception.BusinessRuleException;
import com.shelfspace.common.exception.DuplicateResourceException;
import com.shelfspace.common.exception.InvalidRequestException;
import com.shelfspace.common.exception.ResourceNotFoundException;
import com.shelfspace.reservation.ReservationRepository;
import com.shelfspace.reservation.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {

    /** Copies in these states are tied to a member, so admins can't change them by hand. */
    private static final Set<CopyStatus> IN_CIRCULATION = EnumSet.of(CopyStatus.BORROWED, CopyStatus.ON_HOLD);

    /** States an admin may set directly. BORROWED / ON_HOLD are set only by loans and reservations. */
    private static final Set<CopyStatus> ADMIN_SETTABLE = EnumSet.of(CopyStatus.AVAILABLE, CopyStatus.DAMAGED, CopyStatus.LOST);

    /** Whitelisted so ?sort=anything can't reach the query and blow up as a 500. */
    private static final Set<String> SORTABLE = Set.of("title", "author", "category", "publicationYear", "createdAt");

    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final ReservationRepository reservationRepository;
    private final AuditService auditService;

    // ---------- Queries (any logged-in user) ----------

    @Transactional(readOnly = true)
    public PageResponse<BookResponse> search(String q, String category, boolean availableOnly, Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new InvalidRequestException("Cannot sort by '" + order.getProperty() + "'. Allowed: " + SORTABLE);
            }
        });
        Page<Book> page = bookRepository.search(blankToNull(q), blankToNull(category), availableOnly, pageable);

        List<Long> ids = page.map(Book::getId).getContent();
        Map<Long, CopyCounts> counts = ids.isEmpty() ? Map.of()
                : copyRepository.countByBookIds(ids).stream()
                .collect(Collectors.toMap(CopyCounts::getBookId, Function.identity()));

        return PageResponse.from(page.map(b -> toResponse(b, counts.get(b.getId()))));
    }

    @Transactional(readOnly = true)
    public BookResponse get(Long id) {
        return toResponse(findBook(id));
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return bookRepository.findAllCategories();
    }

    // ---------- Admin: books ----------

    @Transactional
    public BookResponse create(BookRequest req, Long adminId) {
        String isbn = Isbn.normalize(req.isbn());
        if (bookRepository.existsByIsbn(isbn)) {
            throw new DuplicateResourceException("A book with ISBN " + isbn + " already exists");
        }
        validateYear(req.publicationYear());

        Book book = new Book();
        apply(book, req, isbn);
        book = bookRepository.save(book);

        int copies = req.initialCopies() == null ? 0 : req.initialCopies();
        if (copies > 0) {
            createCopies(book, copies);
        }
        auditService.record(adminId, "BOOK_CREATED", "Book", book.getId(),
                "isbn=" + isbn + ", copies=" + copies);
        return toResponse(book);
    }

    @Transactional
    public BookResponse update(Long id, BookRequest req, Long adminId) {
        Book book = findBook(id);
        String isbn = Isbn.normalize(req.isbn());
        if (!isbn.equals(book.getIsbn()) && bookRepository.existsByIsbn(isbn)) {
            throw new DuplicateResourceException("A book with ISBN " + isbn + " already exists");
        }
        validateYear(req.publicationYear());

        apply(book, req, isbn);
        auditService.record(adminId, "BOOK_UPDATED", "Book", id, "isbn=" + isbn);
        return toResponse(book);
    }

    /** Soft delete. Refused while a copy is out with a member or someone is waiting for the book. */
    @Transactional
    public void delete(Long id, Long adminId) {
        Book book = findBook(id);
        if (copyRepository.existsByBookIdAndStatusIn(id, IN_CIRCULATION)) {
            throw new BusinessRuleException("Cannot delete a book while copies are borrowed or on hold");
        }
        if (reservationRepository.countByBookIdAndStatus(id, ReservationStatus.WAITING) > 0) {
            throw new BusinessRuleException("Cannot delete a book that members are waiting for");
        }
        book.setDeletedAt(LocalDateTime.now());
        auditService.record(adminId, "BOOK_DELETED", "Book", id, "isbn=" + book.getIsbn());
    }

    // ---------- Admin: copies ----------

    @Transactional(readOnly = true)
    public List<CopyResponse> listCopies(Long bookId) {
        findBook(bookId);
        return copyRepository.findByBookIdOrderByIdAsc(bookId).stream().map(CopyResponse::from).toList();
    }

    @Transactional
    public List<CopyResponse> addCopies(Long bookId, int count, Long adminId) {
        Book book = findBook(bookId);
        List<BookCopy> created = createCopies(book, count);
        auditService.record(adminId, "COPIES_ADDED", "Book", bookId, "count=" + count);
        return created.stream().map(CopyResponse::from).toList();
    }

    @Transactional
    public CopyResponse updateCopyStatus(Long copyId, CopyStatus target, String note, Long adminId) {
        BookCopy copy = copyRepository.findById(copyId)
                .orElseThrow(() -> new ResourceNotFoundException("Copy", copyId));

        if (!ADMIN_SETTABLE.contains(target)) {
            throw new BusinessRuleException("Status " + target + " is set by borrowing and reservations, not by hand");
        }
        if (IN_CIRCULATION.contains(copy.getStatus())) {
            throw new BusinessRuleException("Copy " + copy.getBarcode() + " is " + copy.getStatus()
                    + "; it must be returned or released first");
        }

        CopyStatus previous = copy.getStatus();
        copy.setStatus(target);
        auditService.record(adminId, "COPY_STATUS_CHANGED", "BookCopy", copyId,
                previous + " -> " + target + (note == null ? "" : " (" + note + ")"));
        return CopyResponse.from(copy);
    }

    // ---------- helpers ----------

    private Book findBook(Long id) {
        return bookRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book", id));
    }

    /** Barcodes look like SS000012-003: book id + running number. */
    private List<BookCopy> createCopies(Book book, int count) {
        long existing = copyRepository.countByBookId(book.getId());
        List<BookCopy> copies = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            BookCopy copy = new BookCopy();
            copy.setBook(book);
            copy.setBarcode("SS%06d-%03d".formatted(book.getId(), existing + i));
            copy.setStatus(CopyStatus.AVAILABLE);
            copies.add(copy);
        }
        return copyRepository.saveAll(copies);
    }

    private BookResponse toResponse(Book book) {
        long total = copyRepository.countByBookId(book.getId());
        long available = copyRepository.countByBookIdAndStatus(book.getId(), CopyStatus.AVAILABLE);
        return BookResponse.from(book, total, available);
    }

    private static BookResponse toResponse(Book book, CopyCounts counts) {
        if (counts == null) {
            return BookResponse.from(book, 0, 0);
        }
        long available = counts.getAvailable() == null ? 0 : counts.getAvailable();
        return BookResponse.from(book, counts.getTotal(), available);
    }

    private static void apply(Book book, BookRequest req, String isbn) {
        book.setTitle(req.title().trim());
        book.setAuthor(req.author().trim());
        book.setIsbn(isbn);
        book.setCategory(req.category().trim());
        book.setPublicationYear(req.publicationYear());
        book.setReplacementCost(req.replacementCost());
    }

    private static void validateYear(Integer year) {
        if (year != null && year > Year.now().getValue()) {
            throw new BusinessRuleException("Publication year cannot be in the future");
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
