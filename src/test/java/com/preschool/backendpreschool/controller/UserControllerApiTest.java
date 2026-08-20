package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.config.JwtAuthenticationFilter;
import com.preschool.backendpreschool.config.SecurityConfig;
import com.preschool.backendpreschool.dto.RoleResponse;
import com.preschool.backendpreschool.dto.UserResponse;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.UserStatus;
import com.preschool.backendpreschool.service.CustomUserDetailsService;
import com.preschool.backendpreschool.service.JwtService;
import com.preschool.backendpreschool.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, UserControllerApiTest.SecurityTestConfig.class})
class UserControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListAndGetUsers() throws Exception {
        when(userService.getAllUsers(UserStatus.ACTIVE, "admin")).thenReturn(List.of(user()));
        when(userService.getUserById(1L)).thenReturn(user());

        mockMvc.perform(get("/api/users").param("status", "ACTIVE").param("search", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(1));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@school.com"));

        verify(userService).getAllUsers(UserStatus.ACTIVE, "admin");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateUserAndManageRolesAndStatus() throws Exception {
        when(userService.createUser(any())).thenReturn(user());
        when(userService.updateUser(eq(1L), any())).thenReturn(user());
        when(userService.assignRole(eq(1L), any())).thenReturn(user());
        when(userService.removeRole(eq(1L), any())).thenReturn(user());
        when(userService.deactivateUser(1L)).thenReturn(user());
        when(userService.activateUser(1L)).thenReturn(user());
        when(userService.changeStatus(eq(1L), any())).thenReturn(user());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "teacher@school.com",
                                  "password": "123456",
                                  "roles": ["TEACHER"]
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "updated@school.com"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "DIRECTOR"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "DIRECTOR"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/users/1/deactivate"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/users/1/activate"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "LOCKED"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotAccessUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotAccessUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    private UserResponse user() {
        return new UserResponse(
                1L,
                "admin@school.com",
                "+46000000001",
                UserStatus.ACTIVE,
                true,
                false,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of(new RoleResponse(2L, RoleName.ADMIN, "Admin", "Administrative access", LocalDateTime.now()))
        );
    }

    static class SecurityTestConfig {

        @Bean
        CustomUserDetailsService customUserDetailsService() {
            return mock(CustomUserDetailsService.class);
        }

        @Bean
        JwtService jwtService() {
            return mock(JwtService.class);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(
                JwtService jwtService,
                CustomUserDetailsService customUserDetailsService
        ) {
            return new JwtAuthenticationFilter(jwtService, customUserDetailsService);
        }
    }
}
