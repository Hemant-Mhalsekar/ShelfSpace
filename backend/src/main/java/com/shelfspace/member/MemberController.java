package com.shelfspace.member;

import com.shelfspace.account.AccountService;
import com.shelfspace.account.dto.ResetPasswordRequest;
import com.shelfspace.auth.AuthUser;
import com.shelfspace.common.dto.PageResponse;
import com.shelfspace.member.dto.CreateMemberRequest;
import com.shelfspace.member.dto.MemberDetailResponse;
import com.shelfspace.member.dto.MemberSummaryResponse;
import com.shelfspace.member.dto.SuspendRequest;
import com.shelfspace.member.dto.UpdateMemberRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Members", description = "Librarians manage member accounts")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MemberController {

    private final MemberService memberService;
    private final AccountService accountService;

    @Operation(summary = "Search members by name, email or phone, with loan and fine counts")
    @GetMapping
    public PageResponse<MemberSummaryResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) MemberStatus status,
            @RequestParam(required = false) MembershipTier tier,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return memberService.search(q, role, status, tier, pageable);
    }

    @Operation(summary = "Member profile with current loans, reservations and recent history")
    @GetMapping("/{id}")
    public MemberDetailResponse get(@PathVariable Long id) {
        return memberService.get(id);
    }

    @Operation(summary = "Add a member (or another librarian with role=ADMIN)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberSummaryResponse create(@Valid @RequestBody CreateMemberRequest request,
                                        @AuthenticationPrincipal AuthUser admin) {
        return memberService.create(request, admin.id());
    }

    @Operation(summary = "Update name, phone or membership tier")
    @PutMapping("/{id}")
    public MemberSummaryResponse update(@PathVariable Long id, @Valid @RequestBody UpdateMemberRequest request,
                                        @AuthenticationPrincipal AuthUser admin) {
        return memberService.update(id, request, admin.id());
    }

    @Operation(summary = "Suspend a member (blocks login, borrowing and reservations)")
    @PostMapping("/{id}/suspend")
    public MemberSummaryResponse suspend(@PathVariable Long id, @Valid @RequestBody SuspendRequest request,
                                         @AuthenticationPrincipal AuthUser admin) {
        return memberService.suspend(id, request.reason(), admin.id());
    }

    @Operation(summary = "Re-activate a suspended member")
    @PostMapping("/{id}/activate")
    public MemberSummaryResponse activate(@PathVariable Long id, @AuthenticationPrincipal AuthUser admin) {
        return memberService.activate(id, admin.id());
    }

    @Operation(summary = "Set a temporary password for a member who is locked out")
    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request,
                              @AuthenticationPrincipal AuthUser admin) {
        accountService.resetPassword(id, request.newPassword(), admin.id());
    }
}
