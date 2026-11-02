package com.shelfspace.account;

import com.shelfspace.account.dto.ChangePasswordRequest;
import com.shelfspace.account.dto.UpdateProfileRequest;
import com.shelfspace.audit.AuditService;
import com.shelfspace.auth.dto.AuthResponse;
import com.shelfspace.auth.token.TokenService;
import com.shelfspace.common.exception.BusinessRuleException;
import com.shelfspace.common.exception.ResourceNotFoundException;
import com.shelfspace.member.User;
import com.shelfspace.member.UserRepository;
import com.shelfspace.member.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Self-service account changes, plus the librarian's password reset. */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final TokenService tokenService;

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = find(userId);
        user.setName(req.name().trim());
        user.setPhone(req.phone() == null || req.phone().isBlank() ? null : req.phone().trim());
        return UserResponse.from(user);
    }

    /** Signs out every other device and returns fresh tokens for this one. */
    @Transactional
    public AuthResponse changePassword(Long userId, ChangePasswordRequest req) {
        User user = find(userId);
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Your current password is incorrect");
        }
        if (passwordEncoder.matches(req.newPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Choose a password different from your current one");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        tokenService.revokeAll(user);
        auditService.record(userId, "PASSWORD_CHANGED", "User", userId, null);
        return tokenService.issue(user);
    }

    /** "Sign out other devices": every existing token stops working; this device gets new ones. */
    @Transactional
    public AuthResponse signOutEverywhere(Long userId) {
        User user = find(userId);
        tokenService.revokeAll(user);
        auditService.record(userId, "SIGNED_OUT_EVERYWHERE", "User", userId, null);
        return tokenService.issue(user);
    }

    @Transactional
    public void resetPassword(Long memberId, String newPassword, Long adminId) {
        User user = find(memberId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        tokenService.revokeAll(user);
        auditService.record(adminId, "PASSWORD_RESET", "User", memberId, "by librarian");
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
