package com.puppies.api.service;

import com.puppies.api.exception.NotFoundException;
import com.puppies.api.model.Post;
import com.puppies.api.model.User;
import com.puppies.api.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {
    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

    @Mock
    private UserService userService;

    @Test
    void createPost_validInput_shouldSavePostAndReturn() {
        // Arrange
        Long userId = 1L;
        String imageUrl = " http://example.com/image.jpg ";
        String textContent = " Cute puppy! ";
        User creator = new User(userId, "Creator", "creator@example.com");

        when(userService.getUserById(userId)).thenReturn(Optional.of(creator));

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        when(postRepository.save(postCaptor.capture())).thenAnswer(invocation -> invocation.<Post>getArgument(0));

        Post createdPost = postService.createPost(userId, imageUrl, textContent);

        assertNotNull(createdPost);
        assertEquals(imageUrl.trim(), createdPost.getImageUrl());
        assertEquals(textContent.trim(), createdPost.getTextContent());
        assertEquals(creator, createdPost.getUser());
        assertNotNull(createdPost.getDate());

        Post capturedPost = postCaptor.getValue();
        assertNotNull(capturedPost);
        assertEquals(imageUrl.trim(), capturedPost.getImageUrl());
        assertEquals(textContent.trim(), capturedPost.getTextContent());
        assertEquals(creator, capturedPost.getUser());
        assertNotNull(capturedPost.getDate());

        verify(userService, times(1)).getUserById(userId);
        verify(postRepository, times(1)).save(any(Post.class));
    }


    @Test
    void createPost_invalidUserId_shouldThrowNotFoundException() {
        Long userId = 99L;
        String imageUrl = "http://example.com/image.jpg";
        String textContent = "Cute puppy!";
        when(userService.getUserById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> postService.createPost(userId, imageUrl, textContent));
        verify(userService, times(1)).getUserById(userId);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void getUserFeed_postsExist_shouldReturnPagedPosts() {
        User user1 = new User(1L, "User 1", "user1@example.com");
        User user2 = new User(2L, "User 2", "user2@example.com");
        LocalDateTime now = LocalDateTime.now();
        Post post1 = new Post(1L, "url1", "text1", now.minusDays(1), user1);
        Post post2 = new Post(2L, "url2", "text2", now, user2);
        Pageable pageable = PageRequest.of(0, 5, Sort.by("date").descending());
        List<Post> expectedContent = List.of(post2, post1);
        long totalElements = 25;

        Page<Post> expectedPage = new PageImpl<>(expectedContent, pageable, totalElements);

        when(postRepository.findAll(pageable)).thenReturn(expectedPage);

        Page<Post> actualPage = postService.getUserFeed(pageable);

        assertNotNull(actualPage);
        assertEquals(expectedContent.size(), actualPage.getContent().size());
        assertEquals(totalElements, actualPage.getTotalElements());
        assertEquals(expectedPage.getTotalPages(), actualPage.getTotalPages());
        assertEquals(pageable.getPageNumber(), actualPage.getNumber());
        assertEquals(expectedContent.get(0).getId(), actualPage.getContent().get(0).getId());
        assertEquals(expectedContent.get(1).getId(), actualPage.getContent().get(1).getId());

        verify(postRepository, times(1)).findAll(pageable);
    }

    @Test
    void getUserFeed_noPosts_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("date").descending());
        Page<Post> emptyPage = Page.empty(pageable);

        when(postRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<Post> actualPage = postService.getUserFeed(pageable);

        assertNotNull(actualPage);
        assertTrue(actualPage.isEmpty());
        assertFalse(actualPage.hasContent());
        assertEquals(0, actualPage.getTotalElements());
        assertEquals(0, actualPage.getContent().size());

        verify(postRepository, times(1)).findAll(pageable);
    }

    @Test
    void getPostById_existingId_shouldReturnPost() {
        Long postId = 10L;
        User user = new User(1L, "User", "user@example.com");
        Post expectedPost = new Post(postId, "url", "content", LocalDateTime.now(), user);
        when(postRepository.findById(postId)).thenReturn(Optional.of(expectedPost));

        Post actualPost = postService.getPostById(postId);

        assertNotNull(actualPost);
        assertEquals(expectedPost.getId(), actualPost.getId());
        assertEquals(expectedPost.getImageUrl(), actualPost.getImageUrl());

        verify(postRepository, times(1)).findById(postId);
    }

    @Test
    void getPostById_nonExistingId_shouldThrowNotFoundException() {
        Long postId = 99L;
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> postService.getPostById(postId));
        verify(postRepository, times(1)).findById(postId);
    }


}
