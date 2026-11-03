package com.shelfspace.policy;

import com.shelfspace.auth.AuthUser;
import com.shelfspace.member.MembershipTier;
import com.shelfspace.policy.dto.PolicyResponse;
import com.shelfspace.policy.dto.PolicyUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Borrowing rules", description = "Limits, loan length and fines per membership tier")
@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @Operation(summary = "Rules for every tier (any logged-in user)")
    @GetMapping
    public List<PolicyResponse> list() {
        return policyService.list();
    }

    @Operation(summary = "Rules for one tier")
    @GetMapping("/{tier}")
    public PolicyResponse get(@PathVariable MembershipTier tier) {
        return policyService.get(tier);
    }

    @Operation(summary = "Change a tier's rules (admin). Applies to new loans; existing due dates don't move.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{tier}")
    public PolicyResponse update(@PathVariable MembershipTier tier, @Valid @RequestBody PolicyUpdateRequest request,
                                 @AuthenticationPrincipal AuthUser admin) {
        return policyService.update(tier, request, admin.id());
    }
}
