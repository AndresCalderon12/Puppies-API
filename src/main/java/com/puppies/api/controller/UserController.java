package com.puppies.api.controller;


import com.puppies.api.dto.request.CreateUserRequest;
import com.puppies.api.dto.response.PostResponseDTO;
import com.puppies.api.dto.response.UserResponseDTO;
import com.puppies.api.model.Post;
import com.puppies.api.model.User;
import com.puppies.api.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@RequestBody CreateUserRequest request) {
        User createdUser = userService.createUser(request.getName(), request.getEmail(), request.getPassword());
        return new ResponseEntity<>(mapToUserResponseDto(createdUser), HttpStatus.CREATED);
    }

    private UserResponseDTO mapToUserResponseDto(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        return dto;
    }
}

