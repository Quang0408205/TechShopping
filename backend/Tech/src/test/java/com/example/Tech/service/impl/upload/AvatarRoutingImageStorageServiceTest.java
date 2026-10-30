package com.example.Tech.service.impl.upload;

import com.example.Tech.config.CloudinaryProperties;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.service.upload.CloudinaryClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Avatars to Cloudinary when its keys are set, everything else (and avatars without keys) on disk. */
@ExtendWith(MockitoExtension.class)
class AvatarRoutingImageStorageServiceTest {

    private static final byte[] PNG = Arrays.copyOf(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, 64);
    private static final String OURS = "https://res.cloudinary.com/demo/image/upload/v1790000000/techshopping/avatars/"
            + "0123456789abcdef0123456789abcdef.png";

    @Mock
    private LocalImageStorageService local;

    @Mock
    private CloudinaryClient client;

    @Test
    void withoutKeys_avatarsStayOnTheDisk() {
        AvatarRoutingImageStorageService storage = storage(false);
        MockMultipartFile file = new MockMultipartFile("file", "me.png", "image/png", PNG);
        when(local.storeAvatar(file)).thenReturn("http://localhost:8080/uploads/avatars/x.png");

        assertThat(storage.storeAvatar(file)).isEqualTo("http://localhost:8080/uploads/avatars/x.png");
        verify(client, never()).upload(any(), anyString());
    }

    @Test
    void withKeys_avatarsGoToCloudinary_afterTheSameFileChecks() {
        AvatarRoutingImageStorageService storage = storage(true);
        when(client.upload(any(), anyString())).thenReturn(OURS);

        assertThat(storage.storeAvatar(new MockMultipartFile("file", "me.png", "image/png", PNG))).isEqualTo(OURS);
        assertThatThrownBy(() -> storage.storeAvatar(new MockMultipartFile("file", "me.gif", "image/gif", "GIF89a....".getBytes())))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_IMAGE_FILE));
        assertThatThrownBy(() -> storage.storeAvatar(new MockMultipartFile("file", "big.png", "image/png", Arrays.copyOf(PNG, 2 * 1024 * 1024 + 1))))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.IMAGE_TOO_LARGE));
        // product images never leave the disk
        MockMultipartFile product = new MockMultipartFile("file", "p.png", "image/png", PNG);
        storage.storeProductImage(product);
        verify(local).storeProductImage(product);
    }

    @Test
    void onlyOurCloudAndFolder_count_andOldDiskAvatarsStayValid() {
        AvatarRoutingImageStorageService storage = storage(true);
        when(local.isStoredAvatar(anyString())).thenReturn(false);
        when(local.isStoredAvatar("http://localhost:8080/uploads/avatars/old.png")).thenReturn(true);

        assertThat(storage.isStoredAvatar(OURS)).isTrue();
        assertThat(storage.isStoredAvatar(OURS.replace("/demo/", "/other/"))).isFalse();
        assertThat(storage.isStoredAvatar(OURS.replace("techshopping/avatars", "someone/else"))).isFalse();
        assertThat(storage.isStoredAvatar(OURS.replace(".png", ".svg"))).isFalse();
        assertThat(storage.isStoredAvatar("https://res.cloudinary.com/demo/image/upload/techshopping/avatars/x.png")).isFalse();
        assertThat(storage.isStoredAvatar("http://localhost:8080/uploads/avatars/old.png")).isTrue();
    }

    @Test
    void replacingAnAvatar_deletesItWhereItLives() {
        AvatarRoutingImageStorageService storage = storage(true);

        storage.deleteAfterCommit(OURS);
        verify(client).destroy("techshopping/avatars/0123456789abcdef0123456789abcdef");

        storage.deleteAfterCommit("http://localhost:8080/uploads/avatars/old.png");
        verify(local).deleteAfterCommit("http://localhost:8080/uploads/avatars/old.png");
    }

    private AvatarRoutingImageStorageService storage(boolean keys) {
        CloudinaryProperties properties = keys
                ? new CloudinaryProperties("demo", "key", "secret", null, null, null)
                : new CloudinaryProperties("", "", "", null, null, null);
        return new AvatarRoutingImageStorageService(local, properties, client);
    }
}
