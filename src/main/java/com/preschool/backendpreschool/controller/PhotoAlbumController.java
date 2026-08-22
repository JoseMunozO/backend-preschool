package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.dto.PhotoAlbumPhotoResponse;
import com.preschool.backendpreschool.dto.PhotoAlbumRequest;
import com.preschool.backendpreschool.dto.PhotoAlbumResponse;
import com.preschool.backendpreschool.service.PhotoAlbumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/photo-albums")
@RequiredArgsConstructor
public class PhotoAlbumController {

    private final PhotoAlbumService photoAlbumService;

    @GetMapping
    public List<PhotoAlbumResponse> getAlbums(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long studentId,
            Authentication authentication
    ) {
        return photoAlbumService.getAlbums(groupId, studentId, authentication.getName());
    }

    @GetMapping("/{albumId}")
    public PhotoAlbumResponse getAlbum(
            @PathVariable Long albumId,
            Authentication authentication
    ) {
        return photoAlbumService.getAlbum(albumId, authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PhotoAlbumResponse createAlbum(
            @Valid @RequestBody PhotoAlbumRequest request,
            Authentication authentication
    ) {
        return photoAlbumService.createAlbum(request, authentication.getName());
    }

    @PutMapping("/{albumId}")
    public PhotoAlbumResponse updateAlbum(
            @PathVariable Long albumId,
            @Valid @RequestBody PhotoAlbumRequest request,
            Authentication authentication
    ) {
        return photoAlbumService.updateAlbum(albumId, request, authentication.getName());
    }

    @DeleteMapping("/{albumId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAlbum(
            @PathVariable Long albumId,
            Authentication authentication
    ) {
        photoAlbumService.deleteAlbum(albumId, authentication.getName());
    }

    @PostMapping(value = "/{albumId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PhotoAlbumPhotoResponse addPhoto(
            @PathVariable Long albumId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String caption,
            Authentication authentication
    ) {
        return photoAlbumService.addPhoto(albumId, file, studentId, caption, authentication.getName());
    }

    @PatchMapping("/{albumId}/photos/{photoId}/approve")
    public PhotoAlbumPhotoResponse approvePhoto(
            @PathVariable Long albumId,
            @PathVariable Long photoId,
            Authentication authentication
    ) {
        return photoAlbumService.approvePhoto(albumId, photoId, authentication.getName());
    }

    @DeleteMapping("/{albumId}/photos/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(
            @PathVariable Long albumId,
            @PathVariable Long photoId,
            Authentication authentication
    ) {
        photoAlbumService.deletePhoto(albumId, photoId, authentication.getName());
    }
}
