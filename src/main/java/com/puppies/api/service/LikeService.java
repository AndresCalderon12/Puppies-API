package com.puppies.api.service;

import com.puppies.api.exception.NotFoundException;
import com.puppies.api.model.Like;
import com.puppies.api.model.Post;
import com.puppies.api.model.User;
import com.puppies.api.repository.LikeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Optional;

@Service
public class LikeService {
    private final LikeRepository likeRepository;
    private final UserService userService;
    private final PostService postService;

    public LikeService(LikeRepository likeRepository, UserService userService, PostService postService) {
        this.likeRepository = likeRepository;
        this.userService = userService;
        this.postService = postService;
    }

    @Transactional
    public boolean toggleLike(Long userId, Long postId) {
        Assert.notNull(userId, "User ID cannot be null");
        Assert.notNull(postId, "Post ID cannot be null");

        User user = userService.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        Post post = postService.getPostById(postId);

        Optional<Like> existingLike = likeRepository.findByUserAndPost(user, post);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            return false;
        } else {
            Like newLike = new Like();
            newLike.setUser(user);
            newLike.setPost(post);
            likeRepository.save(newLike);
            return true;
        }
    }
}