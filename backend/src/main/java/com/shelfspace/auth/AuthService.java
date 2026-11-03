package com.shelfspace.auth;

import com.shelfspace.auth.dto.AuthResponse;
import com.shelfspace.auth.dto.LoginRequest;
import com.shelfspace.auth.dto.RegisterRequest;
import com.shelfspace.auth.ratelimit.LoginAttemptService;
import com.shelfspace.auth.token.TokenService;
import com.shelfspace.common.exception.BusinessRuleException;
import com.shelfspace.common.exception.DuplicateResourceException;
import com.shelfspace.common.exception.ResourceNotFoundException;
import com.shelfspace.member.MemberStatus;
import com.shelfspace.member.MembershipTier;
import com.shelfspace.member.Role;
import com.shelfspace.member.User;
import com.shelfspace.member.UserRepository;
import com.shelfspace.member.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginAttemptService loginAttempts;

    /** Self-registration always creates a STUDENT-tier MEMBER. Admins are created by other admins. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPhone(req.phone());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(Role.MEMBER);
        user.setMembershipTier(MembershipTier.STUDENT);
        user.setStatus(MemberStatus.ACTIVE);

        return issueTokens(userRepository.save(user));
    }

    /** Wrong passwords are counted per email + IP; after too many, even the right one is refused for a while. */
    @Transactional
    public AuthResponse login(LoginRequest req, String ip) {
        String email = normalize(req.email());
        loginAttempts.checkAllowed(email, ip);

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            loginAttempts.recordFailure(email, ip);
            throw new InvalidCredentialsException();
        }
        loginAttempts.recordSuccess(email, ip);
        ensureActive(user);
        return issueTokens(user);
    }

    /** Swaps a refresh token for a new pair (the old one stops working). */
    public AuthResponse refresh(String refreshToken) {
        return tokenService.rotate(refreshToken);
    }

    /** Ends the sign-in this refresh token belongs to. */
    public void logout(String refreshToken) {
        tokenService.revoke(refreshToken);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private AuthResponse issueTokens(User user) {
        return tokenService.issue(user);
    }

    private static void ensureActive(User user) {
        if (user.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessRuleException("This account is suspended. Please contact the library.");
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
