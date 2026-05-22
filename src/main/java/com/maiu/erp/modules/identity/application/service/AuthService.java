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
import com.maiu.erp.modules.identity.domain.repository.RoleRepository;
import com.maiu.erp.modules.identity.domain.repository.UserRepository;
import com.maiu.erp.shared.utils.JwtUtil;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final RoleRepository roleRepository;

    public AuthService(UserRepository userRepository,
            PasswordEncoder encoder,
            JwtUtil jwtUtil,
            RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
        this.roleRepository = roleRepository;
    }

    public void register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        Optional<Role> roleOpt = roleRepository.findByName("USER");

        System.out.println("ROLE FOUND? " + roleOpt.isPresent());

        roleOpt.ifPresent(r -> System.out.println("ROLE: " + r.getName()));

        Role userRole = roleOpt
                .orElseThrow(() -> new RuntimeException("Default role USER not found"));

        Set<Role> roles = Set.of(userRole);

        User user = new User(
                request.getName(),
                email,
                encoder.encode(request.getPassword()),
                roles);

        userRepository.save(user);
    }

    public AuthDto login(String email, String password) {

        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!encoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        Set<String> roles = user.getRoles().stream()
                .map(Role::getAuthority)
                .collect(Collectors.toSet());

        String token = jwtUtil.generateToken(user.getEmail(), roles);

        return new AuthDto(user.getId(), token, user.getName(), roles);
    }
}
