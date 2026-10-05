package com.example.ragchatbot.service;

import com.example.ragchatbot.model.User;
import com.example.ragchatbot.repository.UserRepository;
import com.example.ragchatbot.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Authentication Service
 * 
 * Handles user registration, login, and JWT token management.
 */
@Service
public class AuthService implements UserDetailsService {
    
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    public AuthService(UserRepository userRepository, 
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        log.info("AuthService initialized");
    }
    
    /**
     * Register a new user
     */
    public User register(String username, String password, String email) {
        log.info("Registering new user: {}", username);
        
        // Check if username exists
        if (userRepository.existsByUsername(username)) {
            log.warn("Registration failed - username already exists: {}", username);
            throw new RuntimeException("Username already exists");
        }
        
        // Check if email exists
        if (email != null && userRepository.existsByEmail(email)) {
            log.warn("Registration failed - email already exists: {}", email);
            throw new RuntimeException("Email already exists");
        }
        
        // Create user with hashed password
        User user = new User(
            username,
            passwordEncoder.encode(password),
            email
        );
        
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {} (ID: {})", savedUser.getUsername(), savedUser.getId());
        return savedUser;
    }
    
    /**
     * Authenticate user and return JWT token
     */
    public String login(String username, String password) {
        log.info("Login attempt for user: {}", username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.warn("Login failed - user not found: {}", username);
                return new RuntimeException("User not found");
            });
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Login failed - invalid password for user: {}", username);
            throw new RuntimeException("Invalid password");
        }
        
        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        // Generate and return JWT
        String token = jwtUtil.generateToken(username);
        log.info("Login successful for user: {}, token generated", username);
        return token;
    }
    
    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Spring Security UserDetailsService implementation
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for: {}", username);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            new ArrayList<>()
        );
    }
}
