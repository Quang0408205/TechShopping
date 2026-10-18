package com.example.Tech.service.impl.upload;

import com.example.Tech.config.CloudinaryProperties;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.service.upload.CloudinaryClient;
import com.example.Tech.service.upload.ImageStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plan v5 GĐ7: the image storage everyone gets. Avatars go to Cloudinary when its keys are set; every other image,
 * and avatars without keys, stay on the local disk ({@link LocalImageStorageService}). Avatars stored on disk before
 * the switch stay valid, and are deleted from disk when replaced.
 */
@Slf4j
@Primary
@Service
public class AvatarRoutingImageStorageService implements ImageStorageService {

    /** .../image/upload/v123/<folder>/<uuid>.<ext> */
    private static final Pattern VERSIONED_ASSET = Pattern.compile("^v\\d{1,12}/(.+)/([0-9a-f]{32})\\.(jpg|jpeg|png|webp)$");

    private final LocalImageStorageService local;
    private final CloudinaryProperties cloudinary;
    private final CloudinaryClient client;

    public AvatarRoutingImageStorageService(LocalImageStorageService local, CloudinaryProperties cloudinary,
                                            CloudinaryClient client) {
        this.local = local;
        this.cloudinary = cloudinary;
        this.client = client;
        log.info(cloudinary.enabled() ? "Avatars are stored on Cloudinary (cloud {}, folder {})"
                : "CLOUDINARY_* not set: avatars are stored on the local disk", cloudinary.cloudName(), cloudinary.folder());
    }

    @Override
    public String storeAvatar(MultipartFile file) {
        if (!cloudinary.enabled()) {
            return local.storeAvatar(file);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE, "Image file is empty");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new BusinessException(ErrorCode.IMAGE_TOO_LARGE, "Ảnh đại diện tối đa 2 MB");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE, "Image file cannot be read");
        }
        if (LocalImageStorageService.detectExtension(bytes.length > 12 ? java.util.Arrays.copyOf(bytes, 12) : bytes) == null) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE, ErrorCode.INVALID_IMAGE_FILE.getDefaultMessage());
        }
        String url = client.upload(bytes, UUID.randomUUID().toString().replace("-", ""));
        log.info("Stored avatar on Cloudinary ({} bytes)", bytes.length);
        return url;
    }

    @Override
    public boolean isStoredAvatar(String url) {
        return cloudinaryPublicId(url) != null || local.isStoredAvatar(url);
    }

    @Override
    public void deleteAfterCommit(String url) {
        String publicId = cloudinaryPublicId(url);
        if (publicId == null) {
            local.deleteAfterCommit(url);
            return;
        }
        Runnable destroy = () -> {
            try {
                client.destroy(publicId);
                log.info("Deleted avatar {} from Cloudinary", publicId);
            } catch (RuntimeException e) {
                log.warn("Could not delete avatar {} from Cloudinary: {}", publicId, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    destroy.run();
                }
            });
        } else {
            destroy.run();
        }
    }

    /**
     * "folder/uuid" when {@code url} is an avatar of ours on Cloudinary (our cloud, our folder, a random name of ours),
     * else null. Works for URLs stored while the keys were set, even if they are removed later.
     */
    String cloudinaryPublicId(String url) {
        if (url == null || cloudinary.cloudName().isEmpty() || !url.startsWith(cloudinary.deliveryPrefix())) {
            return null;
        }
        Matcher asset = VERSIONED_ASSET.matcher(url.substring(cloudinary.deliveryPrefix().length()));
        if (!asset.matches() || !asset.group(1).equals(cloudinary.folder())) {
            return null;
        }
        return asset.group(1) + "/" + asset.group(2);
    }

    // ---- everything else stays on the disk ----

    @Override
    public String storeProductImage(MultipartFile file) {
        return local.storeProductImage(file);
    }

    @Override
    public String storeReviewImage(MultipartFile file) {
        return local.storeReviewImage(file);
    }

    @Override
    public boolean isStoredReviewImage(String url) {
        return local.isStoredReviewImage(url);
    }

    @Override
    public String storeServiceImage(MultipartFile file) {
        return local.storeServiceImage(file);
    }

    @Override
    public boolean isStoredServiceImage(String url) {
        return local.isStoredServiceImage(url);
    }
}
