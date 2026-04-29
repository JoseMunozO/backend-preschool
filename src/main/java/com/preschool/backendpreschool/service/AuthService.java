package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.AuthResponse;
import com.preschool.backendpreschool.dto.LoginRequest;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new BadRequestException("Usuario inactivo");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Password incorrecta");
        }

        Set<String> roles = user.getRoles()
                .stream()
                .map(role -> role.getCode().name())
                .collect(java.util.stream.Collectors.toSet());

        String token = jwtService.generateToken(user.getEmail(), roles);

        return new AuthResponse(token, user.getEmail(), roles);
    }
}
