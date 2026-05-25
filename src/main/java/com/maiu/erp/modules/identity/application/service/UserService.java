package com.maiu.erp.modules.identity.application.service;

import java.util.List;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.maiu.erp.modules.identity.application.dto.UserDto;
import com.maiu.erp.modules.identity.domain.model.Role;
import com.maiu.erp.modules.identity.domain.model.User;
import com.maiu.erp.modules.identity.domain.repository.UserRepository;



@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final DefaultRoleService defaultRoleService;
    private final DefaultTenantService defaultTenantService;
    private final EmailVerificationService emailVerificationService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder encoder,
            DefaultRoleService defaultRoleService,
            DefaultTenantService defaultTenantService,
            EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.defaultRoleService = defaultRoleService;
        this.defaultTenantService = defaultTenantService;
        this.emailVerificationService = emailVerificationService;
    }

    public UserDto createUser(User user) {

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }

        String email = user.getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        Role userRole = defaultRoleService.getOrCreateUserRole();

        user.setEmail(email);
        user.setPassword(encoder.encode(user.getPassword()));
        user.setEmailVerified(false);
        user.setTenantId(defaultTenantService.getDefaultTenantId());
        user.setRoles(Set.of(userRole));
        User savedUser = userRepository.save(user);
        emailVerificationService.createAndSendVerification(savedUser);

        return new UserDto(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.isEmailVerified());
    }

    public List<UserDto> getUsers() {

        return userRepository.findAll().stream()
                .map(user -> new UserDto(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.isEmailVerified()))
                .toList();
    }

    


}
