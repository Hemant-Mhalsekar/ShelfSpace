package com.shelfspace.auth;

import com.shelfspace.member.MemberStatus;
import com.shelfspace.member.MembershipTier;
import com.shelfspace.member.Role;
import com.shelfspace.member.User;
import com.shelfspace.member.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates demo accounts on startup so the API can be tried in Swagger straight away.
 * Controlled by app.seed.enabled — turn it off in production.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-email}")
    private String adminEmail;
    @Value("${app.seed.admin-password}")
    private String adminPassword;
    @Value("${app.seed.member-email}")
    private String memberEmail;
    @Value("${app.seed.member-password}")
    private String memberPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createIfMissing("Library Admin", adminEmail, adminPassword, Role.ADMIN, MembershipTier.FACULTY);
        createIfMissing("Demo Member", memberEmail, memberPassword, Role.MEMBER, MembershipTier.STUDENT);
    }

    private void createIfMissing(String name, String email, String password, Role role, MembershipTier tier) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setMembershipTier(tier);
        user.setStatus(MemberStatus.ACTIVE);
        userRepository.save(user);
        log.info("Seeded demo {} account: {}", role, email);
    }
}
