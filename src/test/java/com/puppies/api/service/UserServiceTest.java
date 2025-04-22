package com.puppies.api.service;

import com.puppies.api.dto.response.UserResponseDTO;
import com.puppies.api.exception.NotFoundException;
import com.puppies.api.exception.UserAlreadyExistsException;
import com.puppies.api.model.User;
import com.puppies.api.repository.LikeRepository;
import com.puppies.api.repository.PostRepository;
import com.puppies.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private String plainPassword;
    private String hashedPassword;

    @BeforeEach
    void setUp() {
        plainPassword = "password123";
        hashedPassword = "hashedPassword123";
        testUser = new User(1L, "Test User", "test@example.com", hashedPassword);
    }

    @Test
    void createUser_validInput_shouldHashPasswordAndSaveUserAndReturn() {
        String name = " Test User ";
        String email = " Test@Example.com ";
        String expectedNormalizedEmail = "test@example.com";
        String expectedTrimmedName = "Test User";

        when(userRepository.findByEmail(expectedNormalizedEmail)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(plainPassword)).thenReturn(hashedPassword);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            return new User(1L, savedUser.getName(), savedUser.getEmail(), savedUser.getPassword());
        });

        User createdUser = userService.createUser(name, email, plainPassword);

        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        assertEquals(expectedTrimmedName, createdUser.getName());
        assertEquals(expectedNormalizedEmail, createdUser.getEmail());
        assertEquals(hashedPassword, createdUser.getPassword());

        User capturedUser = userCaptor.getValue();
        assertNotNull(capturedUser);
        assertNull(capturedUser.getId());
        assertEquals(expectedTrimmedName, capturedUser.getName());
        assertEquals(expectedNormalizedEmail, capturedUser.getEmail());
        assertEquals(hashedPassword, capturedUser.getPassword());

        verify(userRepository, times(1)).findByEmail(expectedNormalizedEmail);
        verify(passwordEncoder, times(1)).encode(plainPassword);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUser_whenEmailExists_shouldThrowUserAlreadyExistsException() {
        String name = "New User";
        String email = testUser.getEmail();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class, () ->
                userService.createUser(name, email, plainPassword));
        assertTrue(ex.getMessage().contains(email));

        verify(userRepository, times(1)).findByEmail(email);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_existingId_shouldReturnOptionalUser() {
        Long userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        Optional<User> actualUserOptional = userService.getUserById(userId);

        assertTrue(actualUserOptional.isPresent());
        User actualUser = actualUserOptional.get();
        assertEquals(testUser.getId(), actualUser.getId());
        assertEquals(testUser.getName(), actualUser.getName());
        assertEquals(testUser.getEmail(), actualUser.getEmail());
        assertEquals(testUser.getPassword(), actualUser.getPassword());

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getUserById_nonExistingId_shouldReturnEmptyOptional() {
        Long userId = 99L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Optional<User> actualUserOptional = userService.getUserById(userId);

        assertTrue(actualUserOptional.isEmpty());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getUserById_nullId_shouldThrowIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.getUserById(null));
        assertTrue(ex.getMessage().contains("User ID cannot be null"));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void getUserProfile_existingUser_shouldReturnUserProfileDTO() {
        Long userId = testUser.getId();
        long expectedPostCount = 15;
        long expectedLikeCount = 30;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(postRepository.countByUserId(userId)).thenReturn(expectedPostCount);
        when(likeRepository.countByUserId(userId)).thenReturn(expectedLikeCount);

        UserResponseDTO profile = userService.getUserProfile(userId);

        assertNotNull(profile);
        assertEquals(testUser.getId(), profile.getId());
        assertEquals(testUser.getName(), profile.getName());
        assertEquals(testUser.getEmail(), profile.getEmail());
        assertEquals(expectedPostCount, profile.getPostCount());
        assertEquals(expectedLikeCount, profile.getLikedPostsCount());

        verify(userRepository, times(1)).findById(userId);
        verify(postRepository, times(1)).countByUserId(userId);
        verify(likeRepository, times(1)).countByUserId(userId);
    }

    @Test
    void getUserProfile_nonExistingUser_shouldThrowNotFoundException() {
        Long userId = 99L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () ->
                userService.getUserProfile(userId));
        assertTrue(ex.getMessage().contains("User not found with ID: " + userId));

        verify(userRepository, times(1)).findById(userId);
        verifyNoInteractions(postRepository, likeRepository);
    }

    @Test
    void getUserProfile_nullUserId_shouldThrowIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.getUserProfile(null));
        assertTrue(ex.getMessage().contains("User ID cannot be null"));
        verifyNoInteractions(userRepository, postRepository, likeRepository);
    }

    @Test
    void loadUserByUsername_existingEmail_shouldReturnUserDetails() {
        String email = testUser.getEmail();
        String searchEmail = " Test@Example.com ";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userService.loadUserByUsername(searchEmail);

        assertNotNull(userDetails);
        assertEquals(email, userDetails.getUsername());
        assertEquals(hashedPassword, userDetails.getPassword());

        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    void loadUserByUsername_nonExistingEmail_shouldThrowUsernameNotFoundException() {
        String email = "nonexistent@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class, () ->
                userService.loadUserByUsername(email));
        assertTrue(ex.getMessage().contains(email));

        verify(userRepository, times(1)).findByEmail(email);
    }
}
