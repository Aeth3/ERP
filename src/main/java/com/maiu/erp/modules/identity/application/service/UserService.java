package com.maiu.erp.modules.identity.application.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.maiu.erp.modules.identity.application.dto.UserDto;
import com.maiu.erp.modules.identity.domain.model.User;
import com.maiu.erp.modules.identity.domain.repository.UserRepository;



@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public UserService(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    public UserDto createUser(User user) {

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }

        String email = user.getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        user.setEmail(email);
        user.setPassword(encoder.encode(user.getPassword()));
        // user.setRoles(roles);
        User savedUser = userRepository.save(user);

        return new UserDto(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail());
    }

    public List<UserDto> getUsers() {

        return userRepository.findAll().stream()
                .map(user -> new UserDto(
                        user.getId(),
                        user.getName(),
                        user.getEmail()))
                .toList();
    }

    


}
