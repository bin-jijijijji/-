package com.thesis.api;

import com.thesis.domain.UserEntity;
import com.thesis.domain.RoleEntity;
import com.thesis.repo.UserRepository;
import com.thesis.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String password = request.getPassword() == null ? "" : request.getPassword();

        if (username.isEmpty() || password.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.err("username/password不能为空"));
        }

        UserEntity user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.err("用户名或密码错误"));
        }

        if (!user.isEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.err("用户已禁用"));
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.err("用户名或密码错误"));
        }

        List<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toList());

        String token = jwtService.generateToken(user.getUsername(), roles);
        return ResponseEntity.ok(ApiResponse.ok(new LoginResponse(token, user.getUsername(), roles)));
    }
}

