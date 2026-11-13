package com.careerpilot.identity.application;

import com.careerpilot.identity.domain.User;
import com.careerpilot.identity.domain.UserRepository;
import com.careerpilot.shared.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record AuthResult(String accessToken, UUID userId, String email, String fullName, String role) {}

    @Transactional
    public AuthResult register(String email, String rawPassword, String fullName) {
        String normalizedEmail = email.toLowerCase().trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("Email is already registered: " + normalizedEmail);
        }
        String passwordHash = passwordEncoder.encode(rawPassword);
        User user = userRepository.save(new User(normalizedEmail, passwordHash, fullName));
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResult(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    @Transactional(readOnly = true)
    public AuthResult login(String email, String rawPassword) {
        String normalizedEmail = email.toLowerCase().trim();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResult(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }
}
