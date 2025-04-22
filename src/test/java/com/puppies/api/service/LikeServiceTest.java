package com.puppies.api.service;

import com.puppies.api.exception.NotFoundException;
import com.puppies.api.model.Like;
import com.puppies.api.model.Post;
import com.puppies.api.model.User;
import com.puppies.api.repository.LikeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private UserService userService;

    @Mock
    private PostService postService;

    @InjectMocks
    private LikeService likeService;

    private User testUser;
    private Post testPost;
    private Like testLike;

    @BeforeEach
    void setUp() {
        testUser = new User(1L, "Test User", "test@example.com", "hashedPassword");
        testPost = new Post(10L, "image.jpg", "Test Post", LocalDateTime.now(), testUser);
        testLike = new Like();
        testLike.setId(100L);
        testLike.setUser(testUser);
        testLike.setPost(testPost);
    }

    @Test
    void toggleLike_whenLikeDoesNotExist_shouldCreateLikeAndReturnTrue() {
        Long userId = testUser.getId();
        Long postId = testPost.getId();

        when(userService.getUserById(userId)).thenReturn(Optional.of(testUser));
        when(postService.getPostById(postId)).thenReturn(testPost);
        when(likeRepository.findByUserAndPost(testUser, testPost)).thenReturn(Optional.empty());

        ArgumentCaptor<Like> likeCaptor = ArgumentCaptor.forClass(Like.class);
        when(likeRepository.save(likeCaptor.capture())).thenReturn(testLike); // Return a mock saved like

        boolean result = likeService.toggleLike(userId, postId);

        assertTrue(result);
        Like capturedLike = likeCaptor.getValue();
        assertNotNull(capturedLike);
        assertEquals(testUser, capturedLike.getUser());
        assertEquals(testPost, capturedLike.getPost());
        assertNull(capturedLike.getId()); // ID should be null before save

        verify(userService, times(1)).getUserById(userId);
        verify(postService, times(1)).getPostById(postId);
        verify(likeRepository, times(1)).findByUserAndPost(testUser, testPost);
        verify(likeRepository, times(1)).save(any(Like.class));
        verify(likeRepository, never()).delete(any(Like.class));
    }

    @Test
    void toggleLike_whenLikeExists_shouldDeleteLikeAndReturnFalse() {
        Long userId = testUser.getId();
        Long postId = testPost.getId();

        when(userService.getUserById(userId)).thenReturn(Optional.of(testUser));
        when(postService.getPostById(postId)).thenReturn(testPost);
        when(likeRepository.findByUserAndPost(testUser, testPost)).thenReturn(Optional.of(testLike));
        // No need to mock delete, just verify it's called
        doNothing().when(likeRepository).delete(testLike);

        boolean result = likeService.toggleLike(userId, postId);

        assertFalse(result);

        verify(userService, times(1)).getUserById(userId);
        verify(postService, times(1)).getPostById(postId);
        verify(likeRepository, times(1)).findByUserAndPost(testUser, testPost);
        verify(likeRepository, times(1)).delete(testLike);
        verify(likeRepository, never()).save(any(Like.class));
    }

    @Test
    void toggleLike_whenUserNotFound_shouldThrowNotFoundException() {
        Long userId = 99L;
        Long postId = testPost.getId();

        when(userService.getUserById(userId)).thenReturn(Optional.empty());
        // postService.getPostById should not be called

        NotFoundException ex = assertThrows(NotFoundException.class, () -> {
            likeService.toggleLike(userId, postId);
        });

        assertTrue(ex.getMessage().contains("User not found"));

        verify(userService, times(1)).getUserById(userId);
        verifyNoInteractions(postService);
        verifyNoInteractions(likeRepository);
    }

    @Test
    void toggleLike_whenPostNotFound_shouldThrowNotFoundException() {
        Long userId = testUser.getId();
        Long postId = 99L;

        when(userService.getUserById(userId)).thenReturn(Optional.of(testUser));
        // Simulate PostService throwing NotFoundException
        when(postService.getPostById(postId)).thenThrow(new NotFoundException("Post not found"));

        NotFoundException ex = assertThrows(NotFoundException.class, () -> {
            likeService.toggleLike(userId, postId);
        });

        assertTrue(ex.getMessage().contains("Post not found"));

        verify(userService, times(1)).getUserById(userId);
        verify(postService, times(1)).getPostById(postId);
        verifyNoInteractions(likeRepository);
    }

    @Test
    void toggleLike_nullUserId_shouldThrowIllegalArgumentException() {
        Long postId = testPost.getId();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            likeService.toggleLike(null, postId);
        });

        assertTrue(ex.getMessage().contains("User ID cannot be null"));
        verifyNoInteractions(userService, postService, likeRepository);
    }

    @Test
    void toggleLike_nullPostId_shouldThrowIllegalArgumentException() {
        Long userId = testUser.getId();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            likeService.toggleLike(userId, null);
        });

        assertTrue(ex.getMessage().contains("Post ID cannot be null"));
        verifyNoInteractions(userService, postService, likeRepository);
    }
}
