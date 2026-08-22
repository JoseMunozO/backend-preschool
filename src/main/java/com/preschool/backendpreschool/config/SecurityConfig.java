package com.preschool.backendpreschool.config;

import com.preschool.backendpreschool.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        // response.sendError() triggers a server-side forward to /error; without this,
                        // that forwarded request hits .anyRequest().authenticated() unauthenticated
                        // (JwtAuthenticationFilter skips error dispatches) and overwrites the original
                        // 403 with a 401 from the entry point.
                        .requestMatchers("/error").permitAll()

                        .requestMatchers("/api/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/dashboard/summary").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR")
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/teacher-summary").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/admin-summary").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR")
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/finance-summary").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "FINANCE")
                        .requestMatchers("/api/dashboard/**").denyAll()

                        .requestMatchers(HttpMethod.GET, "/api/students/*/consents").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "TEACHER", "PARENT")
                        .requestMatchers(HttpMethod.POST, "/api/students/*/consents").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "PARENT")
                        .requestMatchers(HttpMethod.PATCH, "/api/students/*/consents/*/revoke").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "PARENT")
                        .requestMatchers("/api/students/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "TEACHER")
                        .requestMatchers("/api/class-groups/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "TEACHER")
                        .requestMatchers("/api/schedules/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "TEACHER")
                        .requestMatchers("/api/photo-albums/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "TEACHER")
                        .requestMatchers("/api/attendance/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "TEACHER")

                        .requestMatchers(HttpMethod.GET, "/api/parents/me").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "PARENT")
                        .requestMatchers(HttpMethod.GET, "/api/parents/me/students").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "PARENT")
                        .requestMatchers(HttpMethod.GET, "/api/parents").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/parents/*").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/parents/*/students").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "TEACHER")
                        .requestMatchers("/api/parents/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR")
                        .requestMatchers("/api/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR")
                        .requestMatchers("/api/roles/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR")
                        .requestMatchers(HttpMethod.GET, "/api/payments/me").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "FINANCE", "PARENT")
                        .requestMatchers(HttpMethod.GET, "/api/payments/me/charges").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "FINANCE", "PARENT")
                        .requestMatchers("/api/payments").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "FINANCE")
                        .requestMatchers("/api/payments/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "FINANCE")
                        .requestMatchers(HttpMethod.GET, "/api/materials").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "TEACHER", "FINANCE")
                        .requestMatchers(HttpMethod.GET, "/api/materials/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR", "TEACHER", "FINANCE")
                        .requestMatchers("/api/materials").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR")
                        .requestMatchers("/api/materials/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "DIRECTOR")

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
