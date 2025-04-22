package com.puppies.api.service;

import com.puppies.api.exception.AuthenticationFailedException;
import com.puppies.api.model.User;
import com.puppies.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // --- Simple In-Memory Token Store (Exercise Only!) ---
    // Stores active tokens and maps them to the corresponding User ID
    // NOTE: This is NOT persistent and NOT suitable for production.
    private final Map<String, Long> activeTokens = new ConcurrentHashMap<>();
    // Optional reverse map for quick lookup if needed, or store a richer object
    private final Map<Long, String> userActiveTokens = new ConcurrentHashMap<>();
    // ---------------------------------------------------------

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String login(String email, String plainTextPassword) {
        log.debug("Attempting login for email: {}", email);
        Optional<User> userOptional = userRepository.findByEmail(email.trim().toLowerCase());

        if (userOptional.isEmpty()) {
            log.warn("Login failed: User not found for email {}", email);
            throw new AuthenticationFailedException("Invalid credentials");
        }

        User user = userOptional.get();
        if (!passwordEncoder.matches(plainTextPassword, user.getPassword())) {
            log.warn("Login failed: Password mismatch for email {}", email);
            throw new AuthenticationFailedException("Invalid credentials");
        }

        // --- Login successful, manage token ---
        // Invalidate existing token for this user if any (simple single-session)
        String existingToken = userActiveTokens.remove(user.getId());
        if (existingToken != null) {
            activeTokens.remove(existingToken);
            log.debug("Invalidated existing token for user ID: {}", user.getId());
        }

        // Generate a new simple token
        String token = UUID.randomUUID().toString();
        activeTokens.put(token, user.getId());
        userActiveTokens.put(user.getId(), token); // Store reverse mapping

        log.info("Login successful for user ID: {}, token generated.", user.getId());
        return token; // Return the generated token
    }

    /**
     * Validates a token and returns the associated User ID.
     * Returns Optional.empty() if the token is invalid or not found.
     */
    public Optional<Long> getUserIdFromToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        // Lookup token in our simple in-memory store
        return Optional.ofNullable(activeTokens.get(token));
    }

    public void logout(String token) {
        if (token != null) {
            Long userId = activeTokens.remove(token);
            if (userId != null) {
                userActiveTokens.remove(userId);
                log.info("User ID {} logged out (token invalidated).", userId);
            }
        }
    }
}