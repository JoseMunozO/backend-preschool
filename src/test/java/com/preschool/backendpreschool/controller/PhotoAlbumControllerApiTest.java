package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.config.JwtAuthenticationFilter;
import com.preschool.backendpreschool.config.SecurityConfig;
import com.preschool.backendpreschool.dto.PhotoAlbumResponse;
import com.preschool.backendpreschool.service.CustomUserDetailsService;
import com.preschool.backendpreschool.service.JwtService;
import com.preschool.backendpreschool.service.PhotoAlbumService;
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

@WebMvcTest(PhotoAlbumController.class)
@Import({SecurityConfig.class, PhotoAlbumControllerApiTest.SecurityTestConfig.class})
class PhotoAlbumControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PhotoAlbumService photoAlbumService;

    @Test
    @WithMockUser(username = "teacher@school.com", roles = "TEACHER")
    void teacherCanListGetAndCreateAlbums() throws Exception {
        when(photoAlbumService.getAlbums(1L, null, "teacher@school.com")).thenReturn(List.of(album()));
        when(photoAlbumService.getAlbum(1L, "teacher@school.com")).thenReturn(album());
        when(photoAlbumService.createAlbum(any(), eq("teacher@school.com"))).thenReturn(album());

        mockMvc.perform(get("/api/photo-albums").param("groupId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].photoAlbumId").value(1));

        mockMvc.perform(get("/api/photo-albums/1"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/photo-albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAlbumRequest()))
                .andExpect(status().isCreated());

        verify(photoAlbumService).getAlbums(1L, null, "teacher@school.com");
    }

    @Test
    @WithMockUser(username = "admin@school.com", roles = "ADMIN")
    void adminCanUpdateApproveAndDeleteAlbumsAndPhotos() throws Exception {
        when(photoAlbumService.updateAlbum(eq(1L), any(), eq("admin@school.com"))).thenReturn(album());

        mockMvc.perform(put("/api/photo-albums/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAlbumRequest()))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/photo-albums/1/photos/5/approve"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/photo-albums/1/photos/5"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/photo-albums/1"))
                .andExpect(status().isNoContent());

        verify(photoAlbumService).deleteAlbum(1L, "admin@school.com");
        verify(photoAlbumService).approvePhoto(1L, 5L, "admin@school.com");
        verify(photoAlbumService).deletePhoto(1L, 5L, "admin@school.com");
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void parentCannotAccessPhotoAlbums() throws Exception {
        mockMvc.perform(get("/api/photo-albums"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotAccessPhotoAlbums() throws Exception {
        mockMvc.perform(get("/api/photo-albums"))
                .andExpect(status().isUnauthorized());
    }

    private PhotoAlbumResponse album() {
        return new PhotoAlbumResponse(
                1L,
                "Spring outing",
                "Field trip photos",
                1L,
                "Rainbow Room",
                null,
                null,
                4L,
                "admin@school.com",
                null,
                true,
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private String validAlbumRequest() {
        return """
                {
                  "title": "Spring outing",
                  "description": "Field trip photos",
                  "groupId": 1
                }
                """;
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
