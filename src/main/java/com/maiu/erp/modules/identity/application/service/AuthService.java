package com.maiu.erp.modules.identity.application.service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.maiu.erp.modules.identity.application.dto.AuthDto;
import com.maiu.erp.modules.identity.application.dto.RegisterRequest;
import com.maiu.erp.modules.identity.domain.model.Role;
import com.maiu.erp.modules.identity.domain.model.User;
import com.maiu.erp.modules.identity.domain.repository.UserRepository;
import com.maiu.erp.shared.utils.JwtUtil;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final DefaultRoleService defaultRoleService;
    private final DefaultTenantService defaultTenantService;
    private final EmailVerificationService emailVerificationService;

    public AuthService(UserRepository userRepository,
            PasswordEncoder encoder,
            JwtUtil jwtUtil,
            DefaultRoleService defaultRoleService,
            DefaultTenantService defaultTenantService,
            EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
        this.defaultRoleService = defaultRoleService;
        this.defaultTenantService = defaultTenantService;
        this.emailVerificationService = emailVerificationService;
    }

    public void register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        Role userRole = defaultRoleService.getOrCreateUserRole();

        Set<Role> roles = Set.of(userRole);

        User user = new User(
                request.getName(),
                email,
                encoder.encode(request.getPassword()),
                roles);
        user.setEmailVerified(false);
        user.setTenantId(defaultTenantService.getDefaultTenantId());

        User savedUser = userRepository.save(user);
        emailVerificationService.createAndSendVerification(savedUser);
    }

    public AuthDto login(String email, String password) {

        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!encoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        if (!user.isEmailVerified()) {
            throw new RuntimeException("Please confirm your email before signing in");
        }

        Set<String> roles = user.getRoles().stream()
                .map(Role::getAuthority)
                .collect(Collectors.toSet());

        String token = jwtUtil.generateToken(user.getEmail(), roles);

        return new AuthDto(user.getId(), token, user.getName(), roles);
    }

    public void confirmEmail(String token) {
        emailVerificationService.verifyToken(token);
    }
}
