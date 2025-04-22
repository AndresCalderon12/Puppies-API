package com.puppies.api.service;

import com.puppies.api.exception.UserAlreadyExistsException;
import com.puppies.api.model.User;
import com.puppies.api.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Locale;

@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User createUser(String name, String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String trimmedName = name.trim();

        log.info("Attempting to create user with email: {}", normalizedEmail);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            String errorMessage = "User with email " + normalizedEmail + " already exists.";
            log.warn(errorMessage);
            throw new UserAlreadyExistsException(errorMessage); // Throw specific exception
        }

        User user = new User();
        user.setName(trimmedName);
        user.setEmail(normalizedEmail);

        User savedUser = userRepository.save(user);
        log.info("Successfully created user with ID: {}", savedUser.getId());
        return savedUser;
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        // 1. Input validation
        Assert.notNull(id, "User ID cannot be null.");

        log.debug("Attempting to retrieve user by ID: {}", id);
        Optional<User> userOptional = userRepository.findById(id);

        if (userOptional.isEmpty()) {
            log.debug("User not found with ID: {}", id);
        } else {
            log.debug("User found with ID: {}", id);
        }
        return userOptional;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}