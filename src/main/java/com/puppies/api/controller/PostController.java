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
        Long userId = ((User) authentication.getPrincipal()).getId(); // Ensure Principal is User

        com.puppies.api.model.Post createdPostEntity = postService.createPost(userId, request.getImageUrl(), request.getTextContent());

        PostResponseDTO responseDto = postService.getPostById(createdPostEntity.getId());

        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @GetMapping("/feed")
    public ResponseEntity<Page<PostResponseDTO>> getUserFeed(Pageable pageable) {
        Page<PostResponseDTO> postPage = postService.getUserFeed(pageable);
        return new ResponseEntity<>(postPage, HttpStatus.OK);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponseDTO> getPostDetails(@PathVariable Long postId) {
        PostResponseDTO post = postService.getPostById(postId);
        return new ResponseEntity<>(post, HttpStatus.OK);
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

}
