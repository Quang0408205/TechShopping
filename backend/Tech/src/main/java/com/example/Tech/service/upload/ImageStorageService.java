package com.example.Tech.service.upload;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    /** Same limit as spring.servlet.multipart.max-file-size. */
    long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    /** Public path prefix of uploaded product images (served by UploadConfig). */
    String PRODUCT_IMAGE_PATH = "/uploads/products/";

    /** Public path prefix of photos attached to product reviews. */
    String REVIEW_IMAGE_PATH = "/uploads/reviews/";

    /**
     * Stores a JPEG, PNG or WebP image (checked by its content, not its name) under a random file name.
     *
     * @return the absolute public URL of the stored file
     */
    String storeProductImage(MultipartFile file);

    /** Same checks as storeProductImage, stored under the review photo folder. */
    String storeReviewImage(MultipartFile file);

    /**
     * True when {@code url} is a review photo this server stored and still has on disk (our prefix, a random
     * file name of ours, the file exists). Reviews accept only such URLs, never links to other sites.
     */
    boolean isStoredReviewImage(String url);

    /**
     * Deletes the stored file behind {@code url} when it is one of our uploads; any other URL is ignored.
     * Inside a transaction the file is deleted only after commit, so a rollback keeps it.
     */
    void deleteAfterCommit(String url);
}
