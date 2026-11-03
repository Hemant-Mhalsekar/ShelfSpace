package com.shelfspace.circulation;

import com.shelfspace.auth.AuthUser;
import com.shelfspace.circulation.dto.LoanRequest;
import com.shelfspace.circulation.dto.LoanResponse;
import com.shelfspace.circulation.dto.RejectRequest;
import com.shelfspace.circulation.dto.ReturnResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Loans", description = "Borrow, renew and return books")
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    // ----- Members -----

    @Operation(summary = "Request to borrow a book. One copy is held for you until a librarian approves.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponse request(@Valid @RequestBody LoanRequest request, @AuthenticationPrincipal AuthUser user) {
        return loanService.request(user.id(), request.bookId());
    }

    @Operation(summary = "My loans: current and history. Filter by status.")
    @GetMapping("/me")
    public PageResponse<LoanResponse> mine(
            @RequestParam(required = false) LoanStatus status,
            @PageableDefault(size = 20, sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthUser user) {
        return loanService.search(user.id(), status, pageable);
    }

    @Operation(summary = "One loan (your own, or any loan for admins)")
    @GetMapping("/{id}")
    public LoanResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return loanService.get(id, user);
    }

    @Operation(summary = "Cancel a request that hasn't been approved yet")
    @PostMapping("/{id}/cancel")
    public LoanResponse cancel(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return loanService.cancel(id, user);
    }

    @Operation(summary = "Extend the due date by one loan period")
    @PostMapping("/{id}/renew")
    public LoanResponse renew(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return loanService.renew(id, user);
    }

    // ----- Admins -----

    @Operation(summary = "All loans, filterable by member and status, e.g. status=REQUESTED for the approval queue (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PageResponse<LoanResponse> search(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) LoanStatus status,
            @PageableDefault(size = 20, sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return loanService.search(memberId, status, pageable);
    }

    @Operation(summary = "Approve a request and issue the book (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/approve")
    public LoanResponse approve(@PathVariable Long id, @AuthenticationPrincipal AuthUser admin) {
        return loanService.approve(id, admin.id());
    }

    @Operation(summary = "Reject a request (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/reject")
    public LoanResponse reject(@PathVariable Long id,
                               @Valid @RequestBody(required = false) RejectRequest request,
                               @AuthenticationPrincipal AuthUser admin) {
        return loanService.reject(id, request == null ? null : request.reason(), admin.id());
    }

    @Operation(summary = "Check a book back in; charges any overdue fine (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/return")
    public ReturnResponse returnBook(@PathVariable Long id, @AuthenticationPrincipal AuthUser admin) {
        return loanService.returnBook(id, admin.id());
    }

    @Operation(summary = "Mark a borrowed book as lost; charges replacement cost (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/lost")
    public ReturnResponse lost(@PathVariable Long id, @AuthenticationPrincipal AuthUser admin) {
        return loanService.markLost(id, admin.id());
    }
}
