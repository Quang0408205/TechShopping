package com.example.Tech.service.impl.upload;

import com.example.Tech.config.UploadProperties;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.service.upload.ImageStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Stores uploaded images on the local disk ({@code app.upload.dir}/products). File names are random UUIDs,
 * never the client's name, so a request cannot choose or overwrite a path.
 */
@Slf4j
@Service
public class LocalImageStorageService implements ImageStorageService {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private static final Pattern STORED_FILE_NAME =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    private final Path productDirectory;
    private final String productUrlPrefix;

    public LocalImageStorageService(UploadProperties properties) {
        this.productDirectory = properties.rootDirectory().resolve("products");
        this.productUrlPrefix = properties.publicBaseUrl() + PRODUCT_IMAGE_PATH;
    }

    @Override
    public String storeProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE, "Image file is empty");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessException(ErrorCode.IMAGE_TOO_LARGE, ErrorCode.IMAGE_TOO_LARGE.getDefaultMessage());
        }

        String extension;
        try (InputStream in = file.getInputStream()) {
            extension = detectExtension(in.readNBytes(12));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE, "Image file cannot be read");
        }
        if (extension == null) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE, ErrorCode.INVALID_IMAGE_FILE.getDefaultMessage());
        }

        String fileName = UUID.randomUUID() + "." + extension;
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(productDirectory);
            Files.copy(in, productDirectory.resolve(fileName));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store uploaded image in " + productDirectory, e);
        }
        log.info("Stored uploaded product image {} ({} bytes)", fileName, file.getSize());
        return productUrlPrefix + fileName;
    }

    @Override
    public void deleteAfterCommit(String url) {
        if (url == null || !url.startsWith(productUrlPrefix)) {
            return;
        }
        String fileName = url.substring(productUrlPrefix.length());
        if (!STORED_FILE_NAME.matcher(fileName).matches()) {
            return;
        }
        Path file = productDirectory.resolve(fileName);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteQuietly(file);
                }
            });
        } else {
            deleteQuietly(file);
        }
    }

    /** Recognises the file by its first bytes: JPEG, PNG or WebP; anything else gives null. */
    static String detectExtension(byte[] header) {
        if (header.length >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (header.length >= PNG_SIGNATURE.length
                && Arrays.equals(Arrays.copyOf(header, PNG_SIGNATURE.length), PNG_SIGNATURE)) {
            return "png";
        }
        if (header.length >= 12
                && "RIFF".equals(new String(header, 0, 4, StandardCharsets.US_ASCII))
                && "WEBP".equals(new String(header, 8, 4, StandardCharsets.US_ASCII))) {
            return "webp";
        }
        return null;
    }

    private void deleteQuietly(Path file) {
        try {
            if (Files.deleteIfExists(file)) {
                log.info("Deleted uploaded product image {}", file.getFileName());
            }
        } catch (IOException e) {
            log.warn("Could not delete uploaded image {}: {}", file, e.getMessage());
        }
    }
}
