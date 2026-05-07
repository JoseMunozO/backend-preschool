package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.PhotoAlbum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoAlbumRepository extends JpaRepository<PhotoAlbum, Long> {

    List<PhotoAlbum> findByActiveTrueOrderByEventDateDescCreatedAtDesc();

    List<PhotoAlbum> findByClassGroupGroupIdAndActiveTrueOrderByEventDateDescCreatedAtDesc(Long groupId);

    List<PhotoAlbum> findByStudentStudentIdAndActiveTrueOrderByEventDateDescCreatedAtDesc(Long studentId);
}
