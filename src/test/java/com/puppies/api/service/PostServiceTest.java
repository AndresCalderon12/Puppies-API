package com.puppies.api.service;

import com.puppies.api.dto.response.PostResponseDTO;
import com.puppies.api.exception.NotFoundException;
import com.puppies.api.model.Post;
import com.puppies.api.model.User;
import com.puppies.api.repository.LikeRepository;
import com.puppies.api.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
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

    @Mock
    private LikeRepository likePostRepository;

    @InjectMocks
    private PostService postService;

    @Mock
    private UserService userService;

    private User testUser1;
    private User testUser2;
    private Post testPost1;
    private Post testPost2;

    @BeforeEach
    void setUp() {
        testUser1 = new User(1L, "User 1", "user1@example.com", "test");
        testUser2 = new User(2L, "User 2", "user2@example.com", "test");
        LocalDateTime now = LocalDateTime.now();
        testPost1 = new Post(10L, "url1", "text1", now.minusDays(1), testUser1);
        testPost2 = new Post(20L, "url2", "text2", now, testUser2);
    }


    @Test
    void createPost_validInput_shouldSavePostAndReturn() {
        // Arrange
        Long userId = 1L;
        String imageUrl = " https://example.com/image.jpg ";
        String textContent = " Cute puppy! ";
        User creator = new User(userId, "Creator", "creator@example.com","test");

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
        String imageUrl = "https://example.com/image.jpg";
        String textContent = "Cute puppy!";
        when(userService.getUserById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> postService.createPost(userId, imageUrl, textContent));
        verify(userService, times(1)).getUserById(userId);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void getUserFeed_postsExist_shouldReturnPagedPosts() {
        User user1 = new User(1L, "User 1", "user1@example.com","test");
        User user2 = new User(2L, "User 2", "user2@example.com","test");
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
    void getUserFeed_nullPageable_shouldThrowIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> postService.getUserFeed(null));
        assertTrue(ex.getMessage().contains("Pageable object cannot be null"));
        verify(postRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getPostById_existingId_shouldReturnPost() {
        Long postId = 10L;
        User user = new User(1L, "User", "user@example.com","test");
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

    @Test
    void getLikedPosts_validInput_shouldReturnPagedDTOsWithLikeCounts() {
        Long userId = testUser1.getId();
        Pageable pageable = PageRequest.of(0, 10);
        List<Post> likedPostsContent = List.of(testPost2, testPost1); // User1 liked post2 then post1
        Page<Post> likedPostsPage = new PageImpl<>(likedPostsContent, pageable, 2);
        long likesForPost1 = 5;
        long likesForPost2 = 10;

        when(postRepository.findPostsLikedByUser(userId, pageable)).thenReturn(likedPostsPage);
        // Mock the like counts returned by the repository for each post in the list
        when(likePostRepository.countByPostId(testPost1.getId())).thenReturn(likesForPost1);
        when(likePostRepository.countByPostId(testPost2.getId())).thenReturn(likesForPost2);

        Page<PostResponseDTO> actualDtoPage = postService.getLikedPosts(userId, pageable);

        assertNotNull(actualDtoPage);
        assertEquals(2, actualDtoPage.getContent().size());
        assertEquals(2, actualDtoPage.getTotalElements());
        assertEquals(pageable.getPageNumber(), actualDtoPage.getNumber());

        // Verify DTO content and like counts
        PostResponseDTO dto1 = actualDtoPage.getContent().get(0); // Should be post2 based on query order (newest like first)
        assertEquals(testPost2.getId(), dto1.getId());
        assertEquals(testUser2.getId(), dto1.getUserId());
        assertEquals(likesForPost2, dto1.getLikeCount());

        PostResponseDTO dto2 = actualDtoPage.getContent().get(1); // Should be post1
        assertEquals(testPost1.getId(), dto2.getId());
        assertEquals(testUser1.getId(), dto2.getUserId());
        assertEquals(likesForPost1, dto2.getLikeCount());

        verify(postRepository, times(1)).findPostsLikedByUser(userId, pageable);
        verify(likePostRepository, times(1)).countByPostId(testPost1.getId());
        verify(likePostRepository, times(1)).countByPostId(testPost2.getId());
    }

    @Test
    void getLikedPosts_noLikedPosts_shouldReturnEmptyPage() {
        Long userId = testUser1.getId();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> emptyPage = Page.empty(pageable);

        when(postRepository.findPostsLikedByUser(userId, pageable)).thenReturn(emptyPage);

        Page<PostResponseDTO> actualDtoPage = postService.getLikedPosts(userId, pageable);

        assertNotNull(actualDtoPage);
        assertTrue(actualDtoPage.isEmpty());
        assertEquals(0, actualDtoPage.getTotalElements());

        verify(postRepository, times(1)).findPostsLikedByUser(userId, pageable);
        verify(likePostRepository, never()).countByPostId(anyLong()); // No posts, so no count needed
    }

    @Test
    void getLikedPosts_nullUserId_shouldThrowIllegalArgumentException() {
        Pageable pageable = PageRequest.of(0, 10);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> postService.getLikedPosts(null, pageable));
        assertTrue(ex.getMessage().contains("User ID cannot be null"));
        verify(postRepository, never()).findPostsLikedByUser(any(), any());
    }

    @Test
    void getLikedPosts_nullPageable_shouldThrowIllegalArgumentException() {
        Long userId = testUser1.getId();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                postService.getLikedPosts(userId, null));
        assertTrue(ex.getMessage().contains("Pageable cannot be null"));
        verify(postRepository, never()).findPostsLikedByUser(any(), any());
    }

    // --- getUserPosts Tests ---

    @Test
    void getUserPosts_validInput_shouldReturnPagedDTOsWithLikeCounts() {
        Long userId = testUser1.getId();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("date").descending());
        // User1 only created testPost1 in our setup
        List<Post> userPostsContent = List.of(testPost1);
        Page<Post> userPostsPage = new PageImpl<>(userPostsContent, pageable, 1);
        long likesForPost1 = 7;

        when(postRepository.findByUserIdOrderByDateDesc(userId, pageable)).thenReturn(userPostsPage);
        when(likePostRepository.countByPostId(testPost1.getId())).thenReturn(likesForPost1);

        Page<PostResponseDTO> actualDtoPage = postService.getUserPosts(userId, pageable);

        assertNotNull(actualDtoPage);
        assertEquals(1, actualDtoPage.getContent().size());
        assertEquals(1, actualDtoPage.getTotalElements());
        assertEquals(pageable.getPageNumber(), actualDtoPage.getNumber());

        // Verify DTO content and like count
        PostResponseDTO dto1 = actualDtoPage.getContent().get(0);
        assertEquals(testPost1.getId(), dto1.getId());
        assertEquals(testUser1.getId(), dto1.getUserId());
        assertEquals(likesForPost1, dto1.getLikeCount());

        verify(postRepository, times(1)).findByUserIdOrderByDateDesc(userId, pageable);
        verify(likePostRepository, times(1)).countByPostId(testPost1.getId());
    }

    @Test
    void getUserPosts_userHasNoPosts_shouldReturnEmptyPage() {
        Long userId = testUser2.getId(); // User who hasn't created posts in this setup
        Pageable pageable = PageRequest.of(0, 10, Sort.by("date").descending());
        Page<Post> emptyPage = Page.empty(pageable);

        when(postRepository.findByUserIdOrderByDateDesc(userId, pageable)).thenReturn(emptyPage);

        Page<PostResponseDTO> actualDtoPage = postService.getUserPosts(userId, pageable);

        assertNotNull(actualDtoPage);
        assertTrue(actualDtoPage.isEmpty());
        assertEquals(0, actualDtoPage.getTotalElements());

        verify(postRepository, times(1)).findByUserIdOrderByDateDesc(userId, pageable);
        verify(likePostRepository, never()).countByPostId(anyLong());
    }

    @Test
    void getUserPosts_nullUserId_shouldThrowIllegalArgumentException() {
        Pageable pageable = PageRequest.of(0, 10);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                postService.getUserPosts(null, pageable));
        assertTrue(ex.getMessage().contains("User ID cannot be null"));
        verify(postRepository, never()).findByUserIdOrderByDateDesc(any(), any());
    }

    @Test
    void getUserPosts_nullPageable_shouldThrowIllegalArgumentException() {
        Long userId = testUser1.getId();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()
                -> postService.getUserPosts(userId, null));
        assertTrue(ex.getMessage().contains("Pageable cannot be null"));
        verify(postRepository, never()).findByUserIdOrderByDateDesc(any(), any());
    }



}
