package com.puppies.api.service;

import com.puppies.api.BaseIntegrationTest;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class LikeServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LikeService likeService;

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    private User user;
    private Post post;

    @BeforeEach
    void setUpData() {
        user = userRepository.findByEmail("like@example.com")
                .orElseGet(() -> userService.createUser("Like User", "like@example.com", "pw"));
        post = postService.createPost(user.getId(), "like_url", "like_text");
    }

    @AfterEach
    void tearDownData() {
        likeRepository.deleteAll();
        postRepository.deleteAll();
        if (user != null && user.getId() != null) {
            userRepository.deleteById(user.getId());
        }
        user = null;
        post = null;
    }

    @Test
    @Transactional
    void toggleLike_whenNotLiked_shouldCreateLikeAndReturnTrue() {
        boolean result = likeService.toggleLike(user.getId(), post.getId());

        assertThat(result).isTrue();
        Optional<Like> foundLike = likeRepository.findByUserAndPost(user, post);
        assertThat(foundLike).isPresent();
        assertThat(foundLike.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(foundLike.get().getPost().getId()).isEqualTo(post.getId());
        assertThat(likeRepository.countByPostId(post.getId())).isEqualTo(1);
    }

    @Test
    @Transactional
    void toggleLike_whenAlreadyLiked_shouldDeleteLikeAndReturnFalse() {
        likeService.toggleLike(user.getId(), post.getId());
        assertThat(likeRepository.countByPostId(post.getId())).isEqualTo(1);

        boolean result = likeService.toggleLike(user.getId(), post.getId());

        assertThat(result).isFalse();
        Optional<Like> foundLike = likeRepository.findByUserAndPost(user, post);
        assertThat(foundLike).isEmpty();
        assertThat(likeRepository.countByPostId(post.getId())).isEqualTo(0);
    }

    @Test
    void toggleLike_whenUserNotFound_shouldThrowNotFoundException() {
        Long existingPostId = post.getId();
        NotFoundException ex = assertThrows(NotFoundException.class, () -> {
            likeService.toggleLike(9999L, existingPostId);
        });
        assertThat(ex.getMessage()).contains("User not found");
    }

    @Test
    void toggleLike_whenPostNotFound_shouldThrowNotFoundException() {
        Long existingUserId = user.getId();
        NotFoundException ex = assertThrows(NotFoundException.class, () -> {
            likeService.toggleLike(existingUserId, 9999L);
        });
        assertThat(ex.getMessage()).contains("Post not found");
    }
}
