package com.puppies.api.controller;


import com.puppies.api.service.LikeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/likes")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping
    public ResponseEntity<Void> likePost(@PathVariable Long postId, @RequestParam Long userId) {
        likeService.toggleLike(userId, postId);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
