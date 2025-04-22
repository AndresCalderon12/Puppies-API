package com.puppies.api.repository;

import com.puppies.api.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p JOIN Like l ON l.post = p WHERE l.user.id = :userId")
    Page<Post> findPostsLikedByUser(@Param("userId") Long userId, Pageable pageable);
    Page<Post> findByUserIdOrderByDateDesc(Long userId, Pageable pageable);
    long countByUserId(Long userId);
}