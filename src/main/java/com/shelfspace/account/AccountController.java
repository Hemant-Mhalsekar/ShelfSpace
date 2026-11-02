package com.shelfspace.account;

import com.shelfspace.account.dto.ChangePasswordRequest;
import com.shelfspace.account.dto.UpdateProfileRequest;
import com.shelfspace.auth.AuthUser;
import com.shelfspace.auth.dto.AuthResponse;
import com.shelfspace.member.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account", description = "Your own profile and password")
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "Update my name and phone")
    @PutMapping("/profile")
    public UserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request, @AuthenticationPrincipal AuthUser user) {
        return accountService.updateProfile(user.id(), request);
    }

    @Operation(summary = "Change my password (requires the current one). Signs out other devices; returns new tokens for this one")
    @PostMapping("/password")
    public AuthResponse changePassword(@Valid @RequestBody ChangePasswordRequest request, @AuthenticationPrincipal AuthUser user) {
        return accountService.changePassword(user.id(), request);
    }

    @Operation(summary = "Sign out of every other device; returns new tokens for this one")
    @PostMapping("/sign-out-everywhere")
    public AuthResponse signOutEverywhere(@AuthenticationPrincipal AuthUser user) {
        return accountService.signOutEverywhere(user.id());
    }
}
