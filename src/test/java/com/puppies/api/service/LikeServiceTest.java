package com.puppies.api.service;

import com.puppies.api.exception.NotFoundException;
import com.puppies.api.model.Like;
import com.puppies.api.model.Post;
import com.puppies.api.model.User;
import com.puppies.api.repository.LikeRepository;
import org.junit.jupiter.api.BeforeEach;
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
public class LikeServiceTest {
    @Mock
    private LikeRepository likeRepository;

    @Mock
    private UserService userService;

    @Mock
    private PostService postService;

    @InjectMocks
    private LikeService likeService;

    // Define common test data
    private Long userId;
    private Long postId;
    private User user;
    private Post post;
    private Like existingLike;


    @BeforeEach
    void setUp() {
        userId = 1L;
        postId = 10L;
        user = new User(userId, "Test User", "test@example.com");
        User postAuthor = new User(2L, "Author", "author@example.com");
        post = new Post(postId, "http://example.com/post.jpg", "Test Post Content", null, postAuthor);
        existingLike = new Like();
        existingLike.setId(100L);
        existingLike.setUser(user);
        existingLike.setPost(post);
    }

    @Test
    void toggleLike_whenNotLiked_shouldCreateLikeAndReturnTrue() {
        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(postService.getPostById(postId)).thenReturn(post);
        when(likeRepository.findByUserAndPost(user, post)).thenReturn(Optional.empty());
        when(likeRepository.save(any(Like.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = likeService.toggleLike(userId, postId);

        assertTrue(result, "Should return true indicating the post is now liked.");

        verify(userService, times(1)).getUserById(userId);
        verify(postService, times(1)).getPostById(postId);
        verify(likeRepository, times(1)).findByUserAndPost(user, post);
        verify(likeRepository, times(1)).save(any(Like.class));
        verify(likeRepository, never()).delete(any(Like.class));
    }

    @Test
    void toggleLike_whenAlreadyLiked_shouldDeleteLikeAndReturnFalse() {
        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(postService.getPostById(postId)).thenReturn(post);
        when(likeRepository.findByUserAndPost(user, post)).thenReturn(Optional.of(existingLike));

        boolean result = likeService.toggleLike(userId, postId);

        assertFalse(result, "Should return false indicating the post is now not liked.");
        verify(userService, times(1)).getUserById(userId);
        verify(postService, times(1)).getPostById(postId);
        verify(likeRepository, times(1)).findByUserAndPost(user, post);
        verify(likeRepository, times(1)).delete(existingLike);
        verify(likeRepository, never()).save(any(Like.class));
    }

    @Test
    void toggleLike_whenUserNotFound_shouldThrowNotFoundException() {
        when(userService.getUserById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                likeService.toggleLike(userId, postId),
                "Should throw NotFoundException when user doesn't exist.");

        verify(userService, times(1)).getUserById(userId);
        verifyNoInteractions(postService);
        verifyNoInteractions(likeRepository);
    }

    @Test
    void toggleLike_whenPostNotFound_shouldThrowNotFoundException() {
        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(postService.getPostById(postId)).thenThrow(new NotFoundException("Post not found"));

        assertThrows(NotFoundException.class, () ->
                likeService.toggleLike(userId, postId),
                "Should throw NotFoundException when post doesn't exist.");

        verify(userService, times(1)).getUserById(userId);
        verify(postService, times(1)).getPostById(postId);
        verifyNoInteractions(likeRepository);
    }

    @Test
    void toggleLike_withNullUserId_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            likeService.toggleLike(null, postId);
        }, "Should throw IllegalArgumentException for null userId.");

        verifyNoInteractions(userService);
        verifyNoInteractions(postService);
        verifyNoInteractions(likeRepository);
    }

    @Test
    void toggleLike_withNullPostId_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                likeService.toggleLike(userId, null),
                "Should throw IllegalArgumentException for null postId.");

        verifyNoInteractions(userService);
        verifyNoInteractions(postService);
        verifyNoInteractions(likeRepository);
    }
}
