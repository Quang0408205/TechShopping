package com.example.Tech.service.upload;

/** The two Cloudinary calls the avatar storage needs; failures throw BusinessException(IMAGE_STORAGE_UNAVAILABLE). */
public interface CloudinaryClient {

    /** Uploads the image under {@code folder/publicId}, square-cropped and compressed; returns its https URL. */
    String upload(byte[] image, String publicId);

    /** Deletes {@code folder/publicId}; an image that is already gone is fine. */
    void destroy(String fullPublicId);
}
