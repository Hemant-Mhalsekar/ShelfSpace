package com.shelfspace.catalog;

import com.shelfspace.auth.AuthUser;
import com.shelfspace.catalog.dto.AddCopiesRequest;
import com.shelfspace.catalog.dto.BookRequest;
import com.shelfspace.catalog.dto.BookResponse;
import com.shelfspace.catalog.dto.CopyResponse;
import com.shelfspace.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Books", description = "Catalog search (all users) and book management (admins)")
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @Operation(summary = "Search books by title, author or ISBN, with optional category and availability filters")
    @GetMapping
    public PageResponse<BookResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean availableOnly,
            @PageableDefault(size = 20, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        return bookService.search(q, category, availableOnly, pageable);
    }

    @Operation(summary = "List all categories in the catalog")
    @GetMapping("/categories")
    public List<String> categories() {
        return bookService.categories();
    }

    @Operation(summary = "Get one book with its total and available copies")
    @GetMapping("/{id}")
    public BookResponse get(@PathVariable Long id) {
        return bookService.get(id);
    }

    @Operation(summary = "Add a book (admin). Optionally create copies in the same call.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@Valid @RequestBody BookRequest request, @AuthenticationPrincipal AuthUser admin) {
        return bookService.create(request, admin.id());
    }

    @Operation(summary = "Update book details (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public BookResponse update(@PathVariable Long id, @Valid @RequestBody BookRequest request,
                               @AuthenticationPrincipal AuthUser admin) {
        return bookService.update(id, request, admin.id());
    }

    @Operation(summary = "Soft-delete a book (admin). Refused while copies are borrowed or reserved.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser admin) {
        bookService.delete(id, admin.id());
    }

    @Operation(summary = "List the physical copies of a book (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/copies")
    public List<CopyResponse> copies(@PathVariable Long id) {
        return bookService.listCopies(id);
    }

    @Operation(summary = "Add physical copies to a book (admin). Barcodes are generated.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/copies")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CopyResponse> addCopies(@PathVariable Long id, @Valid @RequestBody AddCopiesRequest request,
                                        @AuthenticationPrincipal AuthUser admin) {
        return bookService.addCopies(id, request.count(), admin.id());
    }
}
