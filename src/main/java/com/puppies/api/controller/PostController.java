package com.puppies.api.controller;


import com.puppies.api.dto.request.CreatePostRequest;
import com.puppies.api.model.Post;
import com.puppies.api.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // Or a more specific error
        }
    }

    @GetMapping("/feed")
    public ResponseEntity<List<Post>> getUserFeed() {
        List<Post> feed = postService.getUserFeed();
        return new ResponseEntity<>(feed, HttpStatus.OK);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<Post> getPostDetails(@PathVariable Long postId) {
        Post post = postService.getPostById(postId);
        if (post != null) {
            return new ResponseEntity<>(post, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
