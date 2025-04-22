package com.puppies.api.controller;


import com.puppies.api.dto.request.CreatePostRequest;
import com.puppies.api.dto.response.PostResponseDTO;
import com.puppies.api.model.Post;
import com.puppies.api.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<Post> createPost(@RequestParam Long userId, @RequestBody CreatePostRequest request) {
        Post post = postService.createPost(userId, request.getImageUrl(), request.getTextContent());
        if (post != null) {
            return new ResponseEntity<>(post, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
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

    private PostResponseDTO mapToPostResponseDTO(Post post) {
        PostResponseDTO dto = new PostResponseDTO();
        dto.setId(post.getId());
        dto.setImageUrl(post.getImageUrl());
        dto.setTextContent(post.getTextContent());
        dto.setDate(post.getDate());
        if (post.getUser() != null) {
            dto.setUserId(post.getUser().getId());
            dto.setUserName(post.getUser().getName());
        }
        return dto;
    }

}
