package com.puppies.api.service;


import com.puppies.api.exception.NotFoundException;
import com.puppies.api.model.Post;
import com.puppies.api.model.User;
import com.puppies.api.repository.PostRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserService userService;


    public PostService(PostRepository postRepository, UserService userService) {
        this.postRepository = postRepository;
        this.userService = userService;
    }

    public Post createPost(Long userId, String imageUrl, String textContent) {
        Optional<User> user = userService.getUserById(userId);
        String trimmedImageUrl = imageUrl.trim();
        String trimmedTextContent = textContent.trim();

        if (user.isEmpty()) {
            throw new NotFoundException("User not found");
        }

        Post post = new Post();
        post.setUser(user.get());
        post.setImageUrl(trimmedImageUrl);
        post.setTextContent(trimmedTextContent);
        post.setDate(LocalDateTime.now());
        return postRepository.save(post);
    }
    @Transactional()
    public Page<Post> getUserFeed(Pageable pageable) {
        Assert.notNull(pageable, "Pageable object cannot be null.");
        Pageable sortedByDateDesc = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("date").descending()
        );
        return postRepository.findAll(sortedByDateDesc);
    }

    public Post getPostById(Long id) {
        Optional<Post> post= postRepository.findById(id);
        if (post.isEmpty()){
            throw new NotFoundException("Post not found");
        }
        return post.get();
    }


}
