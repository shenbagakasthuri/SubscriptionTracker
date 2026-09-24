package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.request.LoginRequest;
import com.subscriptiontracker.dto.request.RegisterRequest;
import com.subscriptiontracker.dto.response.AuthResponse;
import com.subscriptiontracker.exception.DuplicateResourceException;
import com.subscriptiontracker.mapper.UserMapper;
import com.subscriptiontracker.model.entity.User;
import com.subscriptiontracker.repository.UserRepository;
import com.subscriptiontracker.security.JwtTokenProvider;
import com.subscriptiontracker.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .name("Arun Kumar")
                .email("arun@example.com")
                .password("encoded_secret_hash")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new user and return a JWT token")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Arun Kumar")
                .email("Arun@Example.com")
                .password("Password123")
                .build();

        when(userRepository.existsByEmail("arun@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded_secret_hash");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtTokenProvider.generateToken("arun@example.com", 1L)).thenReturn("mock.jwt.token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("arun@example.com", response.getEmail());
        assertEquals("Arun Kumar", response.getName());
        assertEquals(1L, response.getUserId());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when registering with an existing email")
    void register_DuplicateEmail_ThrowsDuplicateResourceException() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Arun Kumar")
                .email("arun@example.com")
                .password("Password123")
                .build();

        when(userRepository.existsByEmail("arun@example.com")).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> authService.register(request)
        );

        assertTrue(exception.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BadCredentialsException with 'Invalid email or password' on wrong password")
    void login_WrongPassword_ThrowsBadCredentialsException() {
        LoginRequest request = LoginRequest.builder()
                .email("arun@example.com")
                .password("WrongPassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
        verify(jwtTokenProvider, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("Should successfully authenticate and return token on valid login")
    void login_Success_ReturnsToken() {
        LoginRequest request = LoginRequest.builder()
                .email("ARUN@example.com")
                .password("Password123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("arun@example.com", "Password123"));
        when(userRepository.findByEmail("arun@example.com")).thenReturn(Optional.of(mockUser));
        when(jwtTokenProvider.generateToken("arun@example.com", 1L)).thenReturn("valid.jwt.token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("valid.jwt.token", response.getToken());
        assertEquals("arun@example.com", response.getEmail());
        assertEquals(1L, response.getUserId());
    }
}
