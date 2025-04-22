package com.puppies.api.controller;

import com.puppies.api.dto.request.CreatePostRequest;
import com.puppies.api.dto.response.PostResponseDTO;
import com.puppies.api.model.Post;
import com.puppies.api.model.User;
import com.puppies.api.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<PostResponseDTO> createPost(@RequestBody CreatePostRequest request, Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        Post createdPost = postService.createPost(userId, request.getImageUrl(), request.getTextContent());
        PostResponseDTO responseDto = mapToPostResponseDTO(createdPost);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @GetMapping("/feed")
    public ResponseEntity<Page<PostResponseDTO>> getUserFeed(Pageable pageable) {
        Page<Post> postPage = postService.getUserFeed(pageable);
        Page<PostResponseDTO> dtoPage = postPage.map(this::mapToPostResponseDTO);
        return new ResponseEntity<>(dtoPage, HttpStatus.OK);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponseDTO> getPostDetails(@PathVariable Long postId) {
        Post post = postService.getPostById(postId);
        PostResponseDTO responseDto = mapToPostResponseDTO(post);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    @GetMapping("/{userId}/likes")
    public ResponseEntity<Page<PostResponseDTO>> getUserLikedPosts(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<PostResponseDTO> likedPosts = postService.getLikedPosts(userId, pageable);
        return ResponseEntity.ok(likedPosts);
    }

    @GetMapping("/{userId}/posts")
    public ResponseEntity<Page<PostResponseDTO>> getUserPosts(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<PostResponseDTO> likedPosts = postService.getUserPosts(userId, pageable);
        return ResponseEntity.ok(likedPosts);
    }

    private PostResponseDTO mapToPostResponseDTO(Post post) {
        PostResponseDTO dto = new PostResponseDTO();
        dto.setId(post.getId());
        dto.setImageUrl(post.getImageUrl());
        dto.setLikeCount(postService.getLikeCount(post));
        dto.setTextContent(post.getTextContent());
        dto.setDate(post.getDate());
        if (post.getUser() != null) {
            dto.setUserId(post.getUser().getId());
            dto.setUserName(post.getUser().getName());
        }
        return dto;
    }

}
