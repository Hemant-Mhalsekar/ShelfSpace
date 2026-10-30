package com.shelfspace.auth;

import com.shelfspace.auth.dto.AuthResponse;
import com.shelfspace.auth.dto.LoginRequest;
import com.shelfspace.auth.dto.RegisterRequest;
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
    private final JwtService jwtService;

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

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(normalize(req.email()))
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        ensureActive(user);
        return issueTokens(user);
    }

    /** Exchanges a valid refresh token for a new token pair. Re-reads the user so role/status changes apply. */
    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        AuthUser claims = jwtService.parse(refreshToken, TokenType.REFRESH);
        User user = userRepository.findById(claims.id())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired token"));
        ensureActive(user);
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private AuthResponse issueTokens(User user) {
        return AuthResponse.bearer(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                jwtService.accessTokenTtlSeconds(),
                UserResponse.from(user));
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
