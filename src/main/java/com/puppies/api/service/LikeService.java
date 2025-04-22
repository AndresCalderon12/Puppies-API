package com.puppies.api.service;

import com.puppies.api.model.Like;
import com.puppies.api.model.Post;
import com.puppies.api.model.User;
import com.puppies.api.repository.LikeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

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
    public boolean likePost(Long userId, Long postId) {
        User user = userService.getUserById(userId);
        Post post = postService.getPostById(postId);
        if (user == null || post == null) {
            return false;
        }
        if (likeRepository.findByUserAndPost(user, post).isEmpty()) {
            Like like = new Like();
            like.setUser(user);
            like.setPost(post);
            likeRepository.save(like);
            return true;
        }
        return false;
    }

}
