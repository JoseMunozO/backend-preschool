package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.dto.AuthResponse;
import com.preschool.backendpreschool.dto.LoginRequest;
import com.preschool.backendpreschool.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}