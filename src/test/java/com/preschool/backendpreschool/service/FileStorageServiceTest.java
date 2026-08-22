package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storeSavesFileAndReturnsPublicUrlUnderSubdirectory() throws IOException {
        FileStorageService service = new FileStorageService(tempDir.toString(), "/uploads");
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        String url = service.store(file, "students/7");

        assertThat(url).matches("/uploads/students/7/[0-9a-f-]+\\.jpg");
        Path stored = tempDir.resolve(url.substring("/uploads/".length()));
        assertThat(Files.exists(stored)).isTrue();
        assertThat(Files.readAllBytes(stored)).containsExactly(1, 2, 3);
    }

    @Test
    void storeRejectsUnsupportedContentType() {
        FileStorageService service = new FileStorageService(tempDir.toString(), "/uploads");
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.store(file, "students/7"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void storeRejectsEmptyFile() {
        FileStorageService service = new FileStorageService(tempDir.toString(), "/uploads");
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.store(file, "students/7"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteRemovesStoredFile() {
        FileStorageService service = new FileStorageService(tempDir.toString(), "/uploads");
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
        String url = service.store(file, "students/7");
        Path stored = tempDir.resolve(url.substring("/uploads/".length()));
        assertThat(Files.exists(stored)).isTrue();

        service.delete(url);

        assertThat(Files.exists(stored)).isFalse();
    }

    @Test
    void deleteIgnoresUrlNotUnderPublicPrefix() {
        FileStorageService service = new FileStorageService(tempDir.toString(), "/uploads");

        service.delete("https://cdn.example.com/legacy/photo.jpg");
        service.delete(null);
    }

    @Test
    void deleteIgnoresPathTraversalAttempt() throws IOException {
        Path outsideFile = Files.createTempFile("outside", ".jpg");
        try {
            FileStorageService service = new FileStorageService(tempDir.toString(), "/uploads");

            service.delete("/uploads/../../" + outsideFile.getFileName());

            assertThat(Files.exists(outsideFile)).isTrue();
        } finally {
            Files.deleteIfExists(outsideFile);
        }
    }
}
