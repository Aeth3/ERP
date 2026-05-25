package com.maiu.erp.modules.identity.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.identity.application.dto.AuthDto;
import com.maiu.erp.modules.identity.application.dto.LoginRequest;
import com.maiu.erp.modules.identity.application.dto.RegisterRequest;
import com.maiu.erp.modules.identity.application.service.AuthService;

import java.util.Map;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest user) {
        authService.register(user);
        return ResponseEntity.status(201).body(
                Map.of("message", "Account created. Please check your email to confirm your account."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto> login(@Valid @RequestBody LoginRequest request) {
        AuthDto dto = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        authService.confirmEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email confirmed successfully"));
    }
}
