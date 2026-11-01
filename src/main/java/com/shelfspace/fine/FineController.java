package com.shelfspace.fine;

import com.shelfspace.auth.AuthUser;
import com.shelfspace.common.dto.PageResponse;
import com.shelfspace.fine.dto.FineResponse;
import com.shelfspace.fine.dto.FineSummaryResponse;
import com.shelfspace.fine.dto.WaiveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Fines", description = "Members see their fines; admins collect or waive them")
@RestController
@RequestMapping("/api/v1/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;

    @Operation(summary = "My fines and total amount owed")
    @GetMapping("/me")
    public FineSummaryResponse mine(@AuthenticationPrincipal AuthUser user) {
        return fineService.forMember(user.id());
    }

    @Operation(summary = "All fines, filterable by member and status (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PageResponse<FineResponse> search(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) FineStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return fineService.search(memberId, status, pageable);
    }

    @Operation(summary = "Record that a fine was paid (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/pay")
    public FineResponse pay(@PathVariable Long id, @AuthenticationPrincipal AuthUser admin) {
        return fineService.pay(id, admin.id());
    }

    @Operation(summary = "Waive a fine with a reason (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/waive")
    public FineResponse waive(@PathVariable Long id, @Valid @RequestBody WaiveRequest request,
                              @AuthenticationPrincipal AuthUser admin) {
        return fineService.waive(id, request.note(), admin.id());
    }
}
