package com.puppies.api.service;

import com.puppies.api.dto.response.PostResponseDTO;
import com.puppies.api.exception.NotFoundException;
import com.puppies.api.model.Like;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private LikeRepository likePostRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private PostService postService;

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
        Long userId = 1L;
        String imageUrl = " https://example.com/image.jpg ";
        String textContent = " Cute puppy! ";
        User creator = new User(userId, "Creator", "creator@example.com", "test");

        when(userService.getUserById(userId)).thenReturn(Optional.of(creator));

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        when(postRepository.save(postCaptor.capture())).thenAnswer(invocation ->
                invocation.<Post>getArgument(0));

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
    void getUserFeed_postsExist_shouldReturnPagedDTOsWithLikeCounts() {
        Pageable pageable = PageRequest.of(0, 5, Sort.by("date").descending());
        List<Post> feedContent = List.of(testPost2, testPost1);
        Page<Post> feedPage = new PageImpl<>(feedContent, pageable, 2);
        long likesForPost1 = 5;
        long likesForPost2 = 10;

        List<Like> likesForPost1List = Collections.nCopies((int)likesForPost1, new Like(null,testUser1, testPost1)); // Simplified mock likes
        List<Like> likesForPost2List = Collections.nCopies((int)likesForPost2, new Like(null,testUser2, testPost2)); // Simplified mock likes
        List<Like> allLikes = new java.util.ArrayList<>(likesForPost1List);
        allLikes.addAll(likesForPost2List);

        List<Long> expectedPostIds = List.of(testPost2.getId(), testPost1.getId());

        when(postRepository.findAll(pageable)).thenReturn(feedPage);
        when(likePostRepository.findByPostIdIn(expectedPostIds)).thenReturn(allLikes);


        Page<PostResponseDTO> actualDtoPage = postService.getUserFeed(pageable);

        assertNotNull(actualDtoPage);
        assertEquals(2, actualDtoPage.getContent().size());
        assertEquals(2, actualDtoPage.getTotalElements());

        PostResponseDTO dto1 = actualDtoPage.getContent().get(0);
        assertEquals(testPost2.getId(), dto1.getId());
        assertEquals(likesForPost2, dto1.getLikeCount());

        PostResponseDTO dto2 = actualDtoPage.getContent().get(1);
        assertEquals(testPost1.getId(), dto2.getId());
        assertEquals(likesForPost1, dto2.getLikeCount());

        verify(postRepository, times(1)).findAll(pageable);
        verify(likePostRepository, times(1)).findByPostIdIn(expectedPostIds);
        verify(likePostRepository, never()).countByPostId(anyLong());
    }


    @Test
    void getUserFeed_noPosts_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("date").descending());
        Page<Post> emptyPage = Page.empty(pageable);

        when(postRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<PostResponseDTO> actualPage = postService.getUserFeed(pageable);

        assertNotNull(actualPage);
        assertTrue(actualPage.isEmpty());
        assertFalse(actualPage.hasContent());
        assertEquals(0, actualPage.getTotalElements());
        assertEquals(0, actualPage.getContent().size());

        verify(postRepository, times(1)).findAll(pageable);
        verify(likePostRepository, never()).findByPostIdIn(anyList());
    }

    @Test
    void getUserFeed_nullPageable_shouldThrowIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> postService.getUserFeed(null));
        assertTrue(ex.getMessage().contains("Pageable object cannot be null"));
        verify(postRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getPostById_existingId_shouldReturnPostDTOWithLikeCount() {
        Long postId = testPost1.getId();
        long expectedLikeCount = 7;

        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost1));
        when(likePostRepository.countByPostId(postId)).thenReturn(expectedLikeCount);

        PostResponseDTO actualPostDto = postService.getPostById(postId);

        assertNotNull(actualPostDto);
        assertEquals(testPost1.getId(), actualPostDto.getId());
        assertEquals(testPost1.getImageUrl(), actualPostDto.getImageUrl());
        assertEquals(expectedLikeCount, actualPostDto.getLikeCount());

        verify(postRepository, times(1)).findById(postId);
        verify(likePostRepository, times(1)).countByPostId(postId);
    }


    @Test
    void getPostById_nonExistingId_shouldThrowNotFoundException() {
        Long postId = 99L;
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> postService.getPostById(postId));
        verify(postRepository, times(1)).findById(postId);
        verify(likePostRepository, never()).countByPostId(anyLong());
    }

    @Test
    void getLikedPosts_validInput_shouldReturnPagedDTOsWithLikeCounts() {
        Long userId = testUser1.getId();
        Pageable pageable = PageRequest.of(0, 10);
        List<Post> likedPostsContent = List.of(testPost2, testPost1);
        Page<Post> likedPostsPage = new PageImpl<>(likedPostsContent, pageable, 2);
        long likesForPost1 = 5;
        long likesForPost2 = 10;

        List<Like> likesForPost1List = Collections.nCopies((int)likesForPost1, new Like(null,testUser1, testPost1));
        List<Like> likesForPost2List = Collections.nCopies((int)likesForPost2, new Like(null,testUser2, testPost2));
        List<Like> allLikes = new java.util.ArrayList<>(likesForPost1List);
        allLikes.addAll(likesForPost2List);

        List<Long> expectedPostIds = List.of(testPost2.getId(), testPost1.getId());

        when(postRepository.findPostsLikedByUser(userId, pageable)).thenReturn(likedPostsPage);
        when(likePostRepository.findByPostIdIn(expectedPostIds)).thenReturn(allLikes);


        Page<PostResponseDTO> actualDtoPage = postService.getLikedPosts(userId, pageable);

        assertNotNull(actualDtoPage);
        assertEquals(2, actualDtoPage.getContent().size());
        assertEquals(2, actualDtoPage.getTotalElements());
        assertEquals(pageable.getPageNumber(), actualDtoPage.getNumber());

        PostResponseDTO dto1 = actualDtoPage.getContent().get(0);
        assertEquals(testPost2.getId(), dto1.getId());
        assertEquals(testUser2.getId(), dto1.getUserId());
        assertEquals(likesForPost2, dto1.getLikeCount());

        PostResponseDTO dto2 = actualDtoPage.getContent().get(1);
        assertEquals(testPost1.getId(), dto2.getId());
        assertEquals(testUser1.getId(), dto2.getUserId());
        assertEquals(likesForPost1, dto2.getLikeCount());

        verify(postRepository, times(1)).findPostsLikedByUser(userId, pageable);
        verify(likePostRepository, times(1)).findByPostIdIn(expectedPostIds);
        verify(likePostRepository, never()).countByPostId(anyLong());
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
        verify(likePostRepository, never()).findByPostIdIn(anyList());
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
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> postService.getLikedPosts(userId, null));
        assertTrue(ex.getMessage().contains("Pageable cannot be null"));
        verify(postRepository, never()).findPostsLikedByUser(any(), any());
    }

    @Test
    void getUserPosts_validInput_shouldReturnPagedDTOsWithLikeCounts() {
        Long userId = testUser1.getId();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("date").descending());
        List<Post> userPostsContent = List.of(testPost1);
        Page<Post> userPostsPage = new PageImpl<>(userPostsContent, pageable, 1);
        long likesForPost1 = 7;

        List<Like> likesForPost1List = Collections.nCopies((int)likesForPost1, new Like(null,testUser1, testPost1));
        List<Long> expectedPostIds = List.of(testPost1.getId());


        when(postRepository.findByUserIdOrderByDateDesc(userId, pageable)).thenReturn(userPostsPage);
        when(likePostRepository.findByPostIdIn(expectedPostIds)).thenReturn(likesForPost1List);


        Page<PostResponseDTO> actualDtoPage = postService.getUserPosts(userId, pageable);

        assertNotNull(actualDtoPage);
        assertEquals(1, actualDtoPage.getContent().size());
        assertEquals(1, actualDtoPage.getTotalElements());
        assertEquals(pageable.getPageNumber(), actualDtoPage.getNumber());

        PostResponseDTO dto1 = actualDtoPage.getContent().get(0);
        assertEquals(testPost1.getId(), dto1.getId());
        assertEquals(testUser1.getId(), dto1.getUserId());
        assertEquals(likesForPost1, dto1.getLikeCount());

        verify(postRepository, times(1)).findByUserIdOrderByDateDesc(userId, pageable);
        verify(likePostRepository, times(1)).findByPostIdIn(expectedPostIds);
        verify(likePostRepository, never()).countByPostId(anyLong());
    }

    @Test
    void getUserPosts_userHasNoPosts_shouldReturnEmptyPage() {
        Long userId = testUser2.getId();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("date").descending());
        Page<Post> emptyPage = Page.empty(pageable);

        when(postRepository.findByUserIdOrderByDateDesc(userId, pageable)).thenReturn(emptyPage);

        Page<PostResponseDTO> actualDtoPage = postService.getUserPosts(userId, pageable);

        assertNotNull(actualDtoPage);
        assertTrue(actualDtoPage.isEmpty());
        assertEquals(0, actualDtoPage.getTotalElements());

        verify(postRepository, times(1)).findByUserIdOrderByDateDesc(userId, pageable);
        verify(likePostRepository, never()).findByPostIdIn(anyList());
    }

    @Test
    void getUserPosts_nullUserId_shouldThrowIllegalArgumentException() {
        Pageable pageable = PageRequest.of(0, 10);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> postService.getUserPosts(null, pageable));
        assertTrue(ex.getMessage().contains("User ID cannot be null"));
        verify(postRepository, never()).findByUserIdOrderByDateDesc(any(), any());
    }

    @Test
    void getUserPosts_nullPageable_shouldThrowIllegalArgumentException() {
        Long userId = testUser1.getId();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                postService.getUserPosts(userId, null));
        assertTrue(ex.getMessage().contains("Pageable cannot be null"));
        verify(postRepository, never()).findByUserIdOrderByDateDesc(any(), any());
    }

}
