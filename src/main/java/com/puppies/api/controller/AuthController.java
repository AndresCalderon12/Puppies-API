package com.puppies.api.controller;

import com.puppies.api.dto.request.LoginRequest;
import com.puppies.api.dto.response.LoginResponseDTO;
import com.puppies.api.exception.AuthenticationFailedException;
import com.puppies.api.model.User;
import com.puppies.api.service.AuthService;
import com.puppies.api.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequest loginRequest) {
        try {
            String token = authService.login(loginRequest.getEmail(), loginRequest.getPassword());

            Long userId = authService.getUserIdFromToken(token).orElseThrow();
            User user = userService.getUserById(userId).orElseThrow();

            LoginResponseDTO response = new LoginResponseDTO(token, userId, user.getName());
            return ResponseEntity.ok(response);
        } catch (AuthenticationFailedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Login failed", e);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            authService.logout(token);
        }
        return ResponseEntity.ok().build();
    }

}