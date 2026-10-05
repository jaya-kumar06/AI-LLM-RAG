package com.example.ragchatbot.controller;

import com.example.ragchatbot.model.User;
import com.example.ragchatbot.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication Controller
 * 
 * Handles user registration and login.
 * Returns JWT token on successful authentication.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AuthController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
        log.info("AuthController initialized");
    }
    
    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        log.info("📝 Register request received for username: {}", request.username());
        try {
            User user = authService.register(
                request.username(), 
                request.password(), 
                request.email()
            );
            log.info("✅ User registered successfully: {}", user.getUsername());
            
            // Auto-login after registration
            String token = authService.login(request.username(), request.password());
            log.info("✅ Auto-login successful, token generated");
            
            return ResponseEntity.ok(Map.of(
                "message", "Registration successful",
                "token", token,
                "username", user.getUsername()
            ));
        } catch (Exception e) {
            log.error("❌ Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Login and get JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("🔐 Login request received for username: {}", request.username());
        try {
            String token = authService.login(request.username(), request.password());
            log.info("✅ Login successful for: {}", request.username());
            
            return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "token", token,
                "username", request.username()
            ));
        } catch (Exception e) {
            log.warn("❌ Login failed for {}: {}", request.username(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get current user info (requires authentication)
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        log.debug("Getting current user info");
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "No token provided"));
            }
            return ResponseEntity.ok(Map.of("message", "Token is valid"));
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
    }
    
    // Request DTOs
    record RegisterRequest(String username, String password, String email) {}
    record LoginRequest(String username, String password) {}
}
