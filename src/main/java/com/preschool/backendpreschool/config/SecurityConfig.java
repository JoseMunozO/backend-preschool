package com.preschool.backendpreschool.config;

import com.preschool.backendpreschool.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()

                        .requestMatchers("/api/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN")

                        .requestMatchers("/api/dashboard/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR")

                        .requestMatchers("/api/students/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "TEACHER")
                        .requestMatchers("/api/class-groups/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "TEACHER")
                        .requestMatchers("/api/schedules/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "TEACHER")

                        .requestMatchers("/api/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR")
                        .requestMatchers("/api/roles/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR")
                        .requestMatchers("/api/payments/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "FINANCE")

                        .anyRequest().authenticated()
                )
                .userDetailsService(userDetailsService)
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
