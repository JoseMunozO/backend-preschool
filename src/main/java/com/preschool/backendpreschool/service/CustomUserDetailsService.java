package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(!"active".equalsIgnoreCase(user.getStatus()))
                .authorities(
                        user.getRoles()
                                .stream()
                                .map(role -> "ROLE_" + role.getCode().name())
                                .toArray(String[]::new)
                )
                .build();
    }
}
