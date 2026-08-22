package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );
    private static final Set<String> ALLOWED_CONTENT_TYPES = EXTENSION_BY_CONTENT_TYPE.keySet();

    private final Path rootLocation;
    private final String publicUrlPrefix;

    public FileStorageService(
            @Value("${app.storage.upload-dir}") String uploadDir,
            @Value("${app.storage.public-url-prefix}") String publicUrlPrefix
    ) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.publicUrlPrefix = publicUrlPrefix.endsWith("/")
                ? publicUrlPrefix.substring(0, publicUrlPrefix.length() - 1)
                : publicUrlPrefix;

        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio de almacenamiento de archivos", e);
        }
    }

    public String store(MultipartFile file, String subdirectory) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo esta vacio");
        }

        String contentType = file.getContentType();
        String extension = contentType != null ? EXTENSION_BY_CONTENT_TYPE.get(contentType.toLowerCase()) : null;
        if (extension == null) {
            throw new BadRequestException("Formato de imagen no soportado, se aceptan JPEG, PNG, WEBP o GIF");
        }

        Path targetDir = resolveSafe(subdirectory);
        String filename = UUID.randomUUID() + extension;

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetDir.resolve(filename));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo subido", e);
        }

        return publicUrlPrefix + "/" + subdirectory + "/" + filename;
    }

    public void delete(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(publicUrlPrefix + "/")) {
            return;
        }

        String relativePath = publicUrl.substring(publicUrlPrefix.length() + 1);
        Path target = rootLocation.resolve(relativePath).normalize();
        if (!target.startsWith(rootLocation)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo {}", relativePath, e);
        }
    }

    private Path resolveSafe(String subdirectory) {
        Path dir = rootLocation.resolve(subdirectory).normalize();
        if (!dir.startsWith(rootLocation)) {
            throw new BadRequestException("Ruta de almacenamiento invalida");
        }
        return dir;
    }
}
