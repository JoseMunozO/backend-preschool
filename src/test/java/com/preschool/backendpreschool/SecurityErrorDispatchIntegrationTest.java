package com.preschool.backendpreschool;

import com.preschool.backendpreschool.dto.AuthResponse;
import com.preschool.backendpreschool.dto.LoginRequest;
import com.preschool.backendpreschool.model.Role;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.model.UserStatus;
import com.preschool.backendpreschool.repository.RoleRepository;
import com.preschool.backendpreschool.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses a real embedded server (not @WebMvcTest) because the bug this guards
 * against only reproduces with a real servlet container: response.sendError()
 * triggers a server-side forward to /error, which @WebMvcTest slices don't
 * replicate, so a role-mismatch there would falsely report 403 in that layer
 * while the real app returned 401.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SecurityErrorDispatchIntegrationTest {

    private static final String TEST_EMAIL = "security-error-dispatch-test@example.com";
    private static final String TEST_PASSWORD = "Test1234!";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanUp() {
        userRepository.findByEmail(TEST_EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    void insufficientRoleReturnsForbiddenNotUnauthorized() {
        Role parentRole = roleRepository.findByCode(RoleName.PARENT)
                .orElseThrow(() -> new IllegalStateException("PARENT role not seeded"));

        User user = User.builder()
                .email(TEST_EMAIL)
                .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(false)
                .roles(Set.of(parentRole))
                .build();
        userRepository.save(user);

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(TEST_EMAIL, TEST_PASSWORD),
                AuthResponse.class
        );
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResponse.getBody().token();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        // PARENT has no access to /api/students/**; an authenticated user
        // denied by role must get 403, not 401 (401 means "not authenticated"
        // and would incorrectly trigger a frontend logout+redirect-to-login).
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/students",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
