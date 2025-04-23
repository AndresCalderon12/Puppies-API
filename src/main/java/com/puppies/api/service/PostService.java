package com.puppies.api.service;

import com.puppies.api.dto.response.PostResponseDTO;
import com.puppies.api.exception.NotFoundException;
import com.puppies.api.model.Like;
import com.puppies.api.model.Post;
import com.puppies.api.model.User;
import com.puppies.api.repository.LikeRepository;
import com.puppies.api.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final UserService userService;
    private final LikeRepository likeRepository;

    @Autowired
    public PostService(PostRepository postRepository, UserService userService, LikeRepository likeRepository) {
        this.postRepository = postRepository;
        this.userService = userService;
        this.likeRepository = likeRepository;
    }

    @Transactional
    public Post createPost(Long userId, String imageUrl, String textContent) {
        log.debug("Attempting to create post for user ID: {}", userId);
        Assert.notNull(userId, "User ID cannot be null.");
        Assert.hasText(imageUrl, "Image URL cannot be blank.");

        User user = userService.getUserById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with ID {} during post creation", userId);
                    return new NotFoundException("User not found with ID: " + userId);
                });

        String trimmedImageUrl = imageUrl.trim();
        String trimmedTextContent = textContent != null ? textContent.trim() : null;

        Post post = new Post();
        post.setUser(user);
        post.setImageUrl(trimmedImageUrl);
        post.setTextContent(trimmedTextContent);
        post.setDate(LocalDateTime.now());

        Post savedPost = postRepository.save(post);
        log.info("Successfully created post with ID: {} for user ID: {}", savedPost.getId(), userId);
        return savedPost;
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDTO> getUserFeed(Pageable pageable) {
        Assert.notNull(pageable, "Pageable object cannot be null.");
        log.debug("Fetching user feed with pageable: {}", pageable);
        Pageable sortedPageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("date").descending());
            log.trace("Applied default sorting by date descending to feed request.");
        }
        Page<Post> feedPage = postRepository.findAll(sortedPageable);
        return mapPagePostToDtoWithCounts(feedPage);
    }

    @Transactional(readOnly = true)
    public PostResponseDTO getPostById(Long postId) {
        Assert.notNull(postId, "Post ID cannot be null.");
        log.info("Fetching details for post ID: {}", postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.warn("Post entity not found with ID: {}", postId);
                    return new NotFoundException("Post not found with ID: " + postId);
                });

        long likeCount = likeRepository.countByPostId(postId);
        log.debug("Like count for post ID {}: {}", postId, likeCount);
        return mapToPostResponseDTO(post, likeCount);
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDTO> getLikedPosts(Long userId, Pageable pageable) {
        Assert.notNull(userId, "User ID cannot be null.");
        Assert.notNull(pageable, "Pageable cannot be null.");
        log.info("Fetching posts liked by user ID: {}", userId);

        Page<Post> likedPostsPage = postRepository.findPostsLikedByUser(userId, pageable);
        return mapPagePostToDtoWithCounts(likedPostsPage);
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDTO> getUserPosts(Long userId, Pageable pageable) {
        Assert.notNull(userId, "User ID cannot be null.");
        Assert.notNull(pageable, "Pageable cannot be null.");
        log.info("Fetching posts created by user ID: {}", userId);

        Page<Post> userPostsPage = postRepository.findByUserIdOrderByDateDesc(userId, pageable);
        return mapPagePostToDtoWithCounts(userPostsPage);
    }

    private Page<PostResponseDTO> mapPagePostToDtoWithCounts(Page<Post> postPage) {
        List<Post> posts = postPage.getContent();
        if (CollectionUtils.isEmpty(posts)) {
            return new PageImpl<>(Collections.emptyList(), postPage.getPageable(), postPage.getTotalElements());
        }

        List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        List<Like> likesForPage = likeRepository.findByPostIdIn(postIds);
        Map<Long, Long> likeCountsMap = likesForPage.stream()
                .collect(Collectors.groupingBy(like -> like.getPost().getId(), Collectors.counting()));

        log.trace("Fetched {} likes for post IDs: {}. Counts map: {}", likesForPage.size(), postIds, likeCountsMap);

        List<PostResponseDTO> dtoList = posts.stream()
                .map(post -> {
                    long count = likeCountsMap.getOrDefault(post.getId(), 0L);
                    return mapToPostResponseDTO(post, count);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, postPage.getPageable(), postPage.getTotalElements());
    }

    private PostResponseDTO mapToPostResponseDTO(Post post, long likeCount) {
        if (post == null) {
            return null;
        }
        PostResponseDTO dto = new PostResponseDTO();
        dto.setId(post.getId());
        dto.setImageUrl(post.getImageUrl());
        dto.setTextContent(post.getTextContent());
        dto.setDate(post.getDate());
        if (post.getUser() != null) {
            dto.setUserId(post.getUser().getId());
            dto.setUserName(post.getUser().getName());
        }
        dto.setLikeCount(likeCount);
        return dto;
    }
}
