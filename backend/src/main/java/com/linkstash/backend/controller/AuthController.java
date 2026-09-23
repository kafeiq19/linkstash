package com.linkstash.backend.controller;

import com.linkstash.backend.common.ApiResponse;
import com.linkstash.backend.dto.AuthRequest;
import com.linkstash.backend.dto.AuthResponse;
import com.linkstash.backend.dto.UserDto;
import com.linkstash.backend.service.AuthService;
import com.linkstash.backend.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@RequestBody AuthRequest req) {
        return ApiResponse.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody AuthRequest req) {
        return ApiResponse.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, UserDto>> me() {
        UserDto user = authService.me(CurrentUser.id());
        return ApiResponse.ok(Map.of("user", user));
    }
}
