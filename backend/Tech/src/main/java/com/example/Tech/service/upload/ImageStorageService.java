package com.example.Tech.service.upload;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    /** Same limit as spring.servlet.multipart.max-file-size. */
    long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    /** Public path prefix of uploaded product images (served by UploadConfig). */
    String PRODUCT_IMAGE_PATH = "/uploads/products/";

    /**
     * Stores a JPEG, PNG or WebP image (checked by its content, not its name) under a random file name.
     *
     * @return the absolute public URL of the stored file
     */
    String storeProductImage(MultipartFile file);

    /**
     * Deletes the stored file behind {@code url} when it is one of our uploads; any other URL is ignored.
     * Inside a transaction the file is deleted only after commit, so a rollback keeps it.
     */
    void deleteAfterCommit(String url);
}
