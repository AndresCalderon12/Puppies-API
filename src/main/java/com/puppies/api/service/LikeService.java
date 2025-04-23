package com.puppies.api.service;

import com.puppies.api.exception.NotFoundException;
import com.puppies.api.model.Like;
import com.puppies.api.model.Post;
import com.puppies.api.model.User;
import com.puppies.api.repository.LikeRepository;
import com.puppies.api.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Optional;

@Service
public class LikeService {

    private static final Logger log = LoggerFactory.getLogger(LikeService.class);

    private final LikeRepository likeRepository;
    private final UserService userService;
    private final PostRepository postRepository;

    @Autowired
    public LikeService(LikeRepository likeRepository, UserService userService, PostRepository postRepository) {
        this.likeRepository = likeRepository;
        this.userService = userService;
        this.postRepository = postRepository;
    }

    /**
     * Toggles the like status for a given user and post.
     * If the user has already liked the post, the like is removed.
     * If the user has not liked the post, a new like is created.
     *
     * @param userId The ID of the user toggling the like.
     * @param postId The ID of the post being liked/unliked.
     * @return true if the post is now liked, false if it is now unliked.
     * @throws NotFoundException if the user or post is not found.
     */
    @Transactional // Use Spring's Transactional
    public boolean toggleLike(Long userId, Long postId) {
        Assert.notNull(userId, "User ID cannot be null");
        Assert.notNull(postId, "Post ID cannot be null");
        log.debug("Attempting to toggle like for user ID {} on post ID {}", userId, postId);

        User user = userService.getUserById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with ID {} during like toggle", userId);
                    return new NotFoundException("User not found with ID: " + userId);
                });

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.warn("Post not found with ID {} during like toggle", postId);
                    return new NotFoundException("Post not found with ID: " + postId);
                });

        Optional<Like> existingLike = likeRepository.findByUserAndPost(user, post);

        if (existingLike.isPresent()) {
            log.info("Removing like for user ID {} on post ID {}", userId, postId);
            likeRepository.delete(existingLike.get());
            return false;
        } else {
            log.info("Adding like for user ID {} on post ID {}", userId, postId);
            Like newLike = new Like();
            newLike.setUser(user);
            newLike.setPost(post);
            return true;
        }
    }
}
