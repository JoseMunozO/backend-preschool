package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class ReceiptStorageService {

    private final Path rootLocation;

    public ReceiptStorageService(@Value("${app.storage.receipts-dir}") String receiptsDir) {
        this.rootLocation = Paths.get(receiptsDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio de almacenamiento de recibos", e);
        }
    }

    public String store(byte[] content) {
        String filename = UUID.randomUUID() + ".pdf";

        try {
            Files.write(resolveSafe(filename), content);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el recibo generado", e);
        }

        return filename;
    }

    public byte[] readIfExists(String filename) {
        if (filename == null) {
            return null;
        }

        Path target = resolveSafe(filename);
        if (!Files.exists(target)) {
            return null;
        }

        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            log.warn("No se pudo leer el recibo {}", filename, e);
            return null;
        }
    }

    private Path resolveSafe(String filename) {
        Path target = rootLocation.resolve(filename).normalize();
        if (!target.startsWith(rootLocation)) {
            throw new BadRequestException("Ruta de almacenamiento invalida");
        }
        return target;
    }
}
