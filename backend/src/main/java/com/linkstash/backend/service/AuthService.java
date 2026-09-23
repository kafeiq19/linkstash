package com.linkstash.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linkstash.backend.common.ApiException;
import com.linkstash.backend.dto.AuthRequest;
import com.linkstash.backend.dto.AuthResponse;
import com.linkstash.backend.dto.UserDto;
import com.linkstash.backend.entity.User;
import com.linkstash.backend.mapper.UserMapper;
import com.linkstash.backend.security.JwtService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,32}$");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(AuthRequest req) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        String password = req.getPassword() == null ? "" : req.getPassword();
        validateCredentials(username, password);

        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (existing != null) {
            throw ApiException.badRequest("username already taken");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCreatedAt(Instant.now().toString());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw ApiException.badRequest("username already taken");
        }

        String token = jwtService.generateToken(user.getId());
        return new AuthResponse(token, new UserDto(user.getId(), user.getUsername()));
    }

    public AuthResponse login(AuthRequest req) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        String password = req.getPassword() == null ? "" : req.getPassword();
        if (username.isEmpty() || password.isEmpty()) {
            throw ApiException.unauthorized("invalid username or password");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.unauthorized("invalid username or password");
        }

        String token = jwtService.generateToken(user.getId());
        return new AuthResponse(token, new UserDto(user.getId(), user.getUsername()));
    }

    public UserDto me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw ApiException.unauthorized("unauthorized");
        }
        return new UserDto(user.getId(), user.getUsername());
    }

    private void validateCredentials(String username, String password) {
        if (!USERNAME.matcher(username).matches()) {
            throw ApiException.badRequest("username must be 3-32 chars: letters, digits, underscore");
        }
        if (password.length() < 8) {
            throw ApiException.badRequest("password must be at least 8 characters");
        }
    }
}
