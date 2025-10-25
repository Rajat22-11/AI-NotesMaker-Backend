package com.contentprocessor.controller;

import com.contentprocessor.model.entities.User;
import com.contentprocessor.repository.UserRepository;
import com.contentprocessor.security.oauth2.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling authentication-related requests.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        // @AuthenticationPrincipal automatically injects the principal object (our UserPrincipal)
        // This is the preferred way to access the logged-in user in controllers.

        if (userPrincipal == null) {
            // This shouldn't happen if the endpoint is correctly secured, but good practice to check
            logger.warn("/me endpoint accessed without authenticated principal.");
            return ResponseEntity.status(401).build(); // Unauthorized
        }

        logger.info("Fetching details for authenticated user ID: {}", userPrincipal.getId());

        // ...or reload from DB to ensure data is fresh (often preferred)
        return userRepository.findById(userPrincipal.getId())
                .map(user -> {
                    logger.debug("Found user details in DB: {}", user);
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> {
                    // This case indicates an inconsistency (JWT valid, but user deleted from DB)
                    logger.error("UserPrincipal ID {} exists but user not found in database!", userPrincipal.getId());
                    return ResponseEntity.notFound().build();
                });

    }
}
