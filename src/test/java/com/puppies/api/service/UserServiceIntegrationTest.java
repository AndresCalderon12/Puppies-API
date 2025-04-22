package com.puppies.api.service;

import com.puppies.api.BaseIntegrationTest;
import com.puppies.api.dto.response.UserResponseDTO;
import com.puppies.api.exception.NotFoundException;
import com.puppies.api.exception.UserAlreadyExistsException;
import com.puppies.api.model.Like;
import com.puppies.api.model.Post;
import com.puppies.api.model.User;
import com.puppies.api.repository.LikeRepository;
import com.puppies.api.repository.PostRepository;
import com.puppies.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class UserServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Test
    @Transactional
    void createUser_shouldSaveUserWithHashedPassword() {
        String name = "Integration User";
        String email = "integration@example.com";
        String password = "password123";

        User createdUser = userService.createUser(name, email, password);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getName()).isEqualTo(name);
        assertThat(createdUser.getEmail()).isEqualTo(email);
        assertThat(createdUser.getPassword()).isNotEqualTo(password);

        Optional<User> foundUser = userRepository.findById(createdUser.getId());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(email);
        assertThat(foundUser.get().getPassword()).isEqualTo(createdUser.getPassword());
    }

    @Test
    @Transactional
    void createUser_whenEmailExists_shouldThrowUserAlreadyExistsException() {
        String name = "Existing User";
        String email = "existing@example.com";
        String password = "password123";
        userService.createUser(name, email, password);

        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class, () -> {
            userService.createUser("Another Name", email, "anotherPassword");
        });

        assertThat(ex.getMessage()).contains(email);
    }

    @Test
    @Transactional
    void getUserById_whenUserExists_shouldReturnUser() {
        User user = userService.createUser("Find Me", "findme@example.com", "pw");

        Optional<User> foundOptional = userService.getUserById(user.getId());

        assertThat(foundOptional).isPresent();
        assertThat(foundOptional.get().getId()).isEqualTo(user.getId());
        assertThat(foundOptional.get().getEmail()).isEqualTo("findme@example.com");
    }

    @Test
    void getUserById_whenUserDoesNotExist_shouldReturnEmptyOptional() {
        Optional<User> foundOptional = userService.getUserById(9999L);
        assertThat(foundOptional).isEmpty();
    }

    @Test
    @Transactional
    void getUserProfile_whenUserExists_shouldReturnProfileWithCounts() {
        User user = userService.createUser("Profile User", "profile@example.com", "pw");
        User otherUser = userService.createUser("Other User", "other@example.com", "pw");

        Post post1 = new Post(null, "url1", "text1", LocalDateTime.now(), user);
        Post post2 = new Post(null, "url2", "text2", LocalDateTime.now(), otherUser);
        postRepository.saveAll(List.of(post1, post2));

        Like like1 = new Like(null, user, post2);
        Like like2 = new Like(null, otherUser, post1);
        likeRepository.saveAll(List.of(like1, like2));

        UserResponseDTO profile = userService.getUserProfile(user.getId());

        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isEqualTo(user.getId());
        assertThat(profile.getName()).isEqualTo(user.getName());
        assertThat(profile.getEmail()).isEqualTo(user.getEmail());
        assertThat(profile.getPostCount()).isEqualTo(1);
        assertThat(profile.getLikedPostsCount()).isEqualTo(1);
    }

    @Test
    void getUserProfile_whenUserDoesNotExist_shouldThrowNotFoundException() {
        NotFoundException ex = assertThrows(NotFoundException.class, () -> {
            userService.getUserProfile(9999L);
        });
        assertThat(ex.getMessage()).contains("User not found");
    }

    @Test
    @Transactional
    void loadUserByUsername_whenUserExists_shouldReturnUserDetails() {
        String email = "loadme@example.com";
        User user = userService.createUser("Load Me", email, "pw");

        UserDetails userDetails = userService.loadUserByUsername(email);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    void loadUserByUsername_whenUserDoesNotExist_shouldThrowUsernameNotFoundException() {
        String email = "notfound@example.com";

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername(email);
        });
        assertThat(ex.getMessage()).contains(email);
    }
}
