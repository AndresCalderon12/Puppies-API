package com.puppies.api.service;


import com.puppies.api.dto.response.PostResponseDTO;
import com.puppies.api.exception.NotFoundException;
import com.puppies.api.model.Like;
import com.puppies.api.model.Post;
import com.puppies.api.model.User;
import com.puppies.api.repository.LikeRepository;
import com.puppies.api.repository.PostRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final LikeRepository likePostRepository;
    private final UserService userService;


    public PostService(PostRepository postRepository, LikeRepository likePostRepository, UserService userService) {
        this.postRepository = postRepository;
        this.likePostRepository = likePostRepository;
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

    @Transactional()
    public Page<PostResponseDTO> getLikedPosts(Long userId, Pageable pageable) {
        Assert.notNull(userId, "User ID cannot be null.");
        Assert.notNull(pageable, "Pageable cannot be null.");

        Page<Post> likedPostsPage = postRepository.findPostsLikedByUser(userId, pageable);

        List<Post> posts = likedPostsPage.getContent();

        List<PostResponseDTO> dtoList = posts.stream()
                .map(this::mapToPostResponseDTO) // Use the helper method
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, likedPostsPage.getPageable(), likedPostsPage.getTotalElements());
    }

    @Transactional()
    public Page<PostResponseDTO> getUserPosts(Long userId, Pageable pageable) {
        Assert.notNull(userId, "User ID cannot be null.");
        Assert.notNull(pageable, "Pageable cannot be null.");

        Page<Post> likedPostsPage = postRepository.findByUserIdOrderByDateDesc(userId, pageable);

        List<Post> posts = likedPostsPage.getContent();

        List<PostResponseDTO> dtoList = posts.stream()
                .map(this::mapToPostResponseDTO) // Use the helper method
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, likedPostsPage.getPageable(), likedPostsPage.getTotalElements());
    }

    private PostResponseDTO mapToPostResponseDTO(Post post) {
        if (post == null) {
            return null;
        }
        long likeCount = getLikeCount(post);

        PostResponseDTO dto = new PostResponseDTO();
        dto.setId(post.getId());
        dto.setImageUrl(post.getImageUrl());
        dto.setTextContent(post.getTextContent());
        dto.setLikeCount(likeCount);
        dto.setDate(post.getDate());
        if (post.getUser() != null) {
            dto.setUserId(post.getUser().getId());
            dto.setUserName(post.getUser().getName());
        }
        return dto;
    }

    public long getLikeCount (Post post) {
        return  likePostRepository.countByPostId(post.getId());
    }



}
