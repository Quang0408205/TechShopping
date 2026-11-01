package com.example.Tech.controller.notification;

import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.notification.StaffNotificationResponse;
import com.example.Tech.service.notification.StaffNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** The bell of the internal area: what happened in the caller's store (every store for an ADMIN). */
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@Tag(name = "Admin - Notifications", description = "Internal notifications by store, read state per person")
@ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN")
@ApiResponse(responseCode = "403", description = "ACCESS_DENIED (not a staff account)")
public class StaffNotificationController {

    private final StaffNotificationService notificationService;

    @GetMapping
    @Operation(summary = "Notifications, newest first", description = "unreadOnly=true keeps the unread ones")
    public ResponseEntity<ApiResult<PageResponse<StaffNotificationResponse>>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResult.ok(notificationService.list(Long.valueOf(jwt.getSubject()), unreadOnly,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)))));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "How many notifications the caller has not read")
    public ResponseEntity<ApiResult<Map<String, Long>>> unreadCount(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(Map.of("unread",
                notificationService.unreadCount(Long.valueOf(jwt.getSubject())))));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark one notification as read (404 when it is not the caller's)")
    public ResponseEntity<ApiResult<Void>> markRead(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable Long id) {
        notificationService.markRead(Long.valueOf(jwt.getSubject()), id);
        return ResponseEntity.ok(ApiResult.ok(null));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark every visible notification as read")
    public ResponseEntity<ApiResult<Map<String, Integer>>> markAllRead(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(Map.of("marked",
                notificationService.markAllRead(Long.valueOf(jwt.getSubject())))));
    }
}
