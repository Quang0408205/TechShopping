package com.example.Tech.service.impl.upload;

import com.example.Tech.config.UploadProperties;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.service.upload.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalImageStorageServiceTest {

    private static final String BASE_URL = "http://localhost:8080";

    /** Smallest headers the service recognises, followed by some payload. */
    static final byte[] JPEG = bytes(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, 100);
    static final byte[] PNG = bytes(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, 100);
    static final byte[] WEBP = bytes("RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1), 100);

    @TempDir
    Path uploadRoot;

    private LocalImageStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new LocalImageStorageService(new UploadProperties(uploadRoot.toString(), BASE_URL + "/"));
    }

    @Test
    void store_jpegPngWebp_savesUnderARandomNameAndReturnsThePublicUrl() throws Exception {
        String jpg = storage.storeProductImage(file("../../evil.png", JPEG));
        String png = storage.storeProductImage(file("photo.jpg", PNG));
        String webp = storage.storeProductImage(file("x", WEBP));

        assertThat(jpg).startsWith(BASE_URL + ImageStorageService.PRODUCT_IMAGE_PATH).endsWith(".jpg");
        assertThat(png).endsWith(".png");
        assertThat(webp).endsWith(".webp");
        assertThat(jpg).doesNotContain("evil");

        Path stored = uploadRoot.resolve("products").resolve(jpg.substring(jpg.lastIndexOf('/') + 1));
        assertThat(stored).exists();
        assertThat(Files.readAllBytes(stored)).isEqualTo(JPEG);
        assertThat(uploadRoot.getParent().resolve("evil.png")).doesNotExist();
    }

    @Test
    void store_notAnImage_throwsInvalidImageFile() {
        MockMultipartFile html = file("image.png", "<html><script>alert(1)</script></html>".getBytes());

        assertThatThrownBy(() -> storage.storeProductImage(html))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_IMAGE_FILE);
        assertThat(uploadRoot.resolve("products")).doesNotExist();
    }

    @Test
    void store_emptyFile_throwsInvalidImageFile() {
        assertThatThrownBy(() -> storage.storeProductImage(file("a.jpg", new byte[0])))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_IMAGE_FILE);
    }

    @Test
    void store_largerThanTheLimit_throwsImageTooLarge() {
        byte[] big = bytes(JPEG, (int) ImageStorageService.MAX_IMAGE_BYTES + 1);

        assertThatThrownBy(() -> storage.storeProductImage(file("big.jpg", big)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_TOO_LARGE);
    }

    @Test
    void deleteAfterCommit_withoutTransaction_deletesOwnFileAtOnce() {
        String url = storage.storeProductImage(file("a.png", PNG));
        Path stored = uploadRoot.resolve("products").resolve(url.substring(url.lastIndexOf('/') + 1));

        storage.deleteAfterCommit(url);

        assertThat(stored).doesNotExist();
    }

    @Test
    void deleteAfterCommit_insideTransaction_waitsForTheCommit() {
        String url = storage.storeProductImage(file("a.png", PNG));
        Path stored = uploadRoot.resolve("products").resolve(url.substring(url.lastIndexOf('/') + 1));

        TransactionSynchronizationManager.initSynchronization();
        try {
            storage.deleteAfterCommit(url);
            assertThat(stored).exists();
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
        assertThat(stored).doesNotExist();
    }

    @Test
    void deleteAfterCommit_ignoresForeignAndCraftedUrls() throws Exception {
        Path products = Files.createDirectories(uploadRoot.resolve("products"));
        Path outside = Files.writeString(uploadRoot.resolve("keep.txt"), "x");
        Path other = Files.writeString(products.resolve("not-a-uuid.jpg"), "x");

        storage.deleteAfterCommit("https://cdn.tgdd.vn/Products/Images/42/1/a.jpg");
        storage.deleteAfterCommit(BASE_URL + "/uploads/products/../keep.txt");
        storage.deleteAfterCommit(BASE_URL + "/uploads/products/not-a-uuid.jpg");
        storage.deleteAfterCommit(null);

        assertThat(outside).exists();
        assertThat(other).exists();
    }

    @Test
    void reviewImages_storedInTheirOwnFolder_andOnlyThoseCountAsStoredReviewImages() {
        String review = storage.storeReviewImage(file("me.jpg", JPEG));
        String product = storage.storeProductImage(file("p.jpg", JPEG));

        assertThat(review).startsWith(BASE_URL + ImageStorageService.REVIEW_IMAGE_PATH).endsWith(".jpg");
        assertThat(uploadRoot.resolve("reviews").resolve(review.substring(review.lastIndexOf('/') + 1))).exists();
        assertThat(storage.isStoredReviewImage(review)).isTrue();
        assertThat(storage.isStoredReviewImage(product)).isFalse();
        assertThat(storage.isStoredReviewImage("https://cdn.tgdd.vn/Products/Images/42/1/a.jpg")).isFalse();
        assertThat(storage.isStoredReviewImage(BASE_URL + "/uploads/reviews/../products/x.jpg")).isFalse();
        assertThat(storage.isStoredReviewImage(BASE_URL + "/uploads/reviews/"
                + "00000000-0000-0000-0000-000000000000.jpg")).isFalse();
        assertThat(storage.isStoredReviewImage(null)).isFalse();

        storage.deleteAfterCommit(review);

        assertThat(storage.isStoredReviewImage(review)).isFalse();
    }

    @Test
    void storeReviewImage_notAnImage_isRefused() {
        assertThatThrownBy(() -> storage.storeReviewImage(file("x.jpg", "<svg onload=alert(1)>".getBytes())))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_IMAGE_FILE);
        assertThat(uploadRoot.resolve("reviews")).doesNotExist();
    }

    @Test
    void detectExtension_recognisesOnlyTheThreeFormats() {
        assertThat(LocalImageStorageService.detectExtension(JPEG)).isEqualTo("jpg");
        assertThat(LocalImageStorageService.detectExtension(PNG)).isEqualTo("png");
        assertThat(LocalImageStorageService.detectExtension(WEBP)).isEqualTo("webp");
        assertThat(LocalImageStorageService.detectExtension("GIF89a......".getBytes())).isNull();
        assertThat(LocalImageStorageService.detectExtension("RIFF....WAVE".getBytes())).isNull();
        assertThat(LocalImageStorageService.detectExtension(new byte[]{(byte) 0xFF})).isNull();
    }

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "application/octet-stream", content);
    }

    static byte[] bytes(byte[] header, int totalLength) {
        return Arrays.copyOf(header, Math.max(totalLength, header.length));
    }
}
