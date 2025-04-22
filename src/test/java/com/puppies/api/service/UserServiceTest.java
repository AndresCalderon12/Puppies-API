package com.puppies.api.service;

import com.puppies.api.exception.NotFoundException;
import com.puppies.api.model.User;
import com.puppies.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_validInput_shouldSaveUserAndReturn() {
        String name = "Test User";
        String email = "test@example.com";
        User newUser = new User(null, name, email,"test");

        when(userRepository.save(any(User.class))).thenReturn(newUser);

        User createdUser = userService.createUser(name, email);

        assertNotNull(createdUser);
        assertEquals(name, createdUser.getName());
        assertEquals(email, createdUser.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void getUserById_existingId_shouldReturnUser() {
        Long userId = 1L;
        User expectedUser = new User(userId, "Test User", "test@example.com","test");

        when(userRepository.findById(userId)).thenReturn(Optional.of(expectedUser));

        Optional<User> actualUserOptional = userService.getUserById(userId);
        User actualUser= actualUserOptional.get();
        assertNotNull(actualUser);
        assertEquals(expectedUser.getId(), actualUser.getId());
        assertEquals(expectedUser.getName(), actualUser.getName());
        assertEquals(expectedUser.getEmail(), actualUser.getEmail());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getUserById_nonExistingId_shouldReturnEmptyOptional() {
        Long userId = 99L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Optional<User> actualUser = userService.getUserById(userId);

        assertTrue(actualUser.isEmpty());
        verify(userRepository, times(1)).findById(userId);
    }
}
