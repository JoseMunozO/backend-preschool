package com.preschool.backendpreschool.dto;

import java.util.Set;

public record AuthResponse(
        String token,
        String email,
        Set<String> roles
) {}