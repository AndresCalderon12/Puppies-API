package com.puppies.api.service;


import com.puppies.api.model.Post;
import com.puppies.api.model.User;
import com.puppies.api.repository.PostRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserService userService;


    public PostService(PostRepository postRepository, UserService userService) {
        this.postRepository = postRepository;
        this.userService = userService;
    }

    public Post createPost(Long userId, String imageUrl, String textContent) {
        User user = userService.getUserById(userId);
        if (user == null) {
            return null;
        }
        Post post = new Post();
        post.setUser(user);
        post.setImageUrl(imageUrl);
        post.setTextContent(textContent);
        post.setDate(LocalDateTime.now());
        return postRepository.save(post);
    }
    public List<Post> getUserFeed() {
        return postRepository.findAllByOrderByDateDesc();
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id).orElse(null); // Consider proper exception handling
    }
}
