package com.puppies.api.service;

import com.puppies.api.BaseIntegrationTest;
import com.puppies.api.dto.response.PostResponseDTO;
import com.puppies.api.exception.NotFoundException;
import com.puppies.api.model.Like;
import com.puppies.api.model.Post;
import com.puppies.api.model.User;
import com.puppies.api.repository.LikeRepository;
import com.puppies.api.repository.PostRepository;
import com.puppies.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PostServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private LikeRepository likeRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUpUsers() {
        user1 = userRepository.findByEmail("user1.post@example.com")
                .orElseGet(() -> userService.createUser("User One", "user1.post@example.com", "pw1"));
        user2 = userRepository.findByEmail("user2.post@example.com")
                .orElseGet(() -> userService.createUser("User Two", "user2.post@example.com", "pw2"));
    }

    @AfterEach
    void tearDownData() {
        likeRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteById(user1.getId());
        userRepository.deleteById(user2.getId());
    }


    @Test
    @Transactional
    void createPost_validInput_shouldSaveAndReturnPost() {
        String imageUrl = "http://images.com/dog1.jpg";
        String textContent = "My first post!";

        Post createdPost = postService.createPost(user1.getId(), imageUrl, textContent);

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getId()).isNotNull();
        assertThat(createdPost.getImageUrl()).isEqualTo(imageUrl);
        assertThat(createdPost.getTextContent()).isEqualTo(textContent);
        assertThat(createdPost.getDate()).isNotNull();
        assertThat(createdPost.getUser()).isNotNull();
        assertThat(createdPost.getUser().getId()).isEqualTo(user1.getId());

        Optional<Post> foundPost = postRepository.findById(createdPost.getId());
        assertThat(foundPost).isPresent();
        assertThat(foundPost.get().getTextContent()).isEqualTo(textContent);
    }

    @Test
    void createPost_invalidUserId_shouldThrowNotFoundException() {
        NotFoundException ex = assertThrows(NotFoundException.class, () -> {
            postService.createPost(9999L, "url", "text");
        });
        assertThat(ex.getMessage()).contains("User not found");
    }

    @Test
    @Transactional
    void getPostById_whenPostExists_shouldReturnPostDTOWithLikeCount() {
        Post post = new Post(null, "url", "text", LocalDateTime.now(), user1);
        post = postRepository.save(post);

        Like like1 = new Like(null, user1, post);
        Like like2 = new Like(null, user2, post);
        likeRepository.saveAll(List.of(like1, like2));

        PostResponseDTO postDetails = postService.getPostById(post.getId());

        assertThat(postDetails).isNotNull();
        assertThat(postDetails.getId()).isEqualTo(post.getId());
        assertThat(postDetails.getTextContent()).isEqualTo("text");
    }

    @Test
    void getPostById_whenPostDoesNotExist_shouldThrowNotFoundException() {
        NotFoundException ex = assertThrows(NotFoundException.class, () -> postService.getPostById(9999L));
        assertThat(ex.getMessage()).contains("Post not found");
    }

    @Test
    @Transactional
    void getUserFeed_shouldReturnPaginatedPostsSortedByDate() {
        LocalDateTime now = LocalDateTime.now();
        Post post3 = new Post(null, "url3", "Post 3", now.minusDays(2), user1);
        Post post2 = new Post(null, "url2", "Post 2", now.minusDays(1), user2);
        Post post1 = new Post(null, "url1", "Post 1", now, user1);
        postRepository.saveAll(List.of(post1, post2, post3));

        Pageable pageable = PageRequest.of(0, 2, Sort.by("date").descending());

        Page<PostResponseDTO> feedPage = postService.getUserFeed(pageable);

        assertThat(feedPage).isNotNull();
        assertThat(feedPage.getTotalElements()).isEqualTo(3);
        assertThat(feedPage.getTotalPages()).isEqualTo(2);
        assertThat(feedPage.getContent()).hasSize(2);
        assertThat(feedPage.getContent().get(0).getTextContent()).isEqualTo("Post 1");
        assertThat(feedPage.getContent().get(1).getTextContent()).isEqualTo("Post 2");
    }

    @Test
    @Transactional
    void getUserPosts_shouldReturnPaginatedPostsForUserSortedByDate() {
        LocalDateTime now = LocalDateTime.now();
        Post postUser1_Old = new Post(null, "url3", "User1 Post 2", now.minusDays(2), user1);
        Post postUser2_Mid = new Post(null, "url2", "User2 Post 1", now.minusDays(1), user2);
        Post postUser1_New = new Post(null, "url1", "User1 Post 1", now, user1);
        postRepository.saveAll(List.of(postUser1_Old, postUser2_Mid, postUser1_New));

        Like like1 = new Like(null, user2, postUser1_New);
        Like like2 = new Like(null, user2, postUser1_Old);
        likeRepository.saveAll(List.of(like1, like2));

        Pageable pageable = PageRequest.of(0, 5, Sort.by("date").descending());

        Page<PostResponseDTO> userPostsPage = postService.getUserPosts(user1.getId(), pageable);

        assertThat(userPostsPage).isNotNull();
        assertThat(userPostsPage.getTotalElements()).isEqualTo(2);
        assertThat(userPostsPage.getContent()).hasSize(2);
        assertThat(userPostsPage.getContent().get(0).getTextContent()).isEqualTo("User1 Post 1");
        assertThat(userPostsPage.getContent().get(0).getLikeCount()).isEqualTo(1);
        assertThat(userPostsPage.getContent().get(1).getTextContent()).isEqualTo("User1 Post 2");
        assertThat(userPostsPage.getContent().get(1).getLikeCount()).isEqualTo(1);
    }

    @Test
    @Transactional
    void getLikedPosts_shouldReturnPaginatedLikedPostsSortedByPostDate() {
        LocalDateTime now = LocalDateTime.now();
        Post post1 = new Post(null, "url1", "P1", now.minusDays(3), user1);
        Post post2 = new Post(null, "url2", "P2", now.minusDays(1), user2);
        Post post3 = new Post(null, "url3", "P3", now.minusDays(2), user1);
        postRepository.saveAll(List.of(post1, post2, post3));

        Like like1_user1 = new Like(null, user1, post2);
        Like like2_user1 = new Like(null, user1, post3);

        Like like3_user2 = new Like(null, user2, post1);
        Like like4_user2 = new Like(null, user2, post2);

        likeRepository.saveAll(List.of(like1_user1, like2_user1, like3_user2, like4_user2));

        Pageable pageable = PageRequest.of(0, 5);

        Page<PostResponseDTO> likedPostsPage = postService.getLikedPosts(user1.getId(), pageable);

        assertThat(likedPostsPage).isNotNull();
        assertThat(likedPostsPage.getTotalElements()).isEqualTo(2);
        assertThat(likedPostsPage.getContent()).hasSize(2);
        assertThat(likedPostsPage.getContent().get(0).getTextContent()).isEqualTo("P2");
        assertThat(likedPostsPage.getContent().get(0).getLikeCount()).isEqualTo(2);
        assertThat(likedPostsPage.getContent().get(1).getTextContent()).isEqualTo("P3");
        assertThat(likedPostsPage.getContent().get(1).getLikeCount()).isEqualTo(1);
    }
}