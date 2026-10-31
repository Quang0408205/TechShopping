package com.example.Tech.service.audit;

import com.example.Tech.dto.response.audit.AuditLogResponse;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.entity.audit.AuditLog;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.repository.audit.AuditLogRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.service.user.CurrentUserLoader;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Writes and reads the audit trail. Writing never throws: a failed log line must not undo the action it describes. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository repository;
    private final UserRepository userRepository;
    private final CurrentUserLoader currentUserLoader;

    /** What one request contributes; the service fills the label and the entity from the path. */
    public record Entry(Long userId, String username, String fullName, String role, String method, String path, int status,
                        String ip, String requestData) {
    }

    @Transactional
    public void record(Entry entry) {
        try {
            AuditRoute route = AuditRoute.of(entry.method(), entry.path());
            AuditLog log = new AuditLog();
            log.setUserId(entry.userId());
            log.setUsername(truncate(entry.username(), 50));
            log.setFullName(truncate(entry.fullName() != null ? entry.fullName()
                    : userRepository.findById(entry.userId()).map(user -> user.getFullname()).orElse(null), 120));
            log.setRole(entry.role());
            log.setMethod(entry.method());
            log.setPath(truncate(entry.path(), 300));
            log.setActionLabel(truncate(route.label(), 150));
            log.setEntityType(truncate(route.entityType(), 50));
            log.setEntityId(route.entityId());
            log.setStatus(entry.status());
            log.setIp(truncate(entry.ip(), 64));
            log.setRequestData(entry.requestData());
            repository.save(log);
        } catch (RuntimeException e) {
            AuditLogService.log.warn("Audit log of {} {} by user id={} failed: {}", entry.method(), entry.path(),
                    entry.userId(), e.toString());
        }
    }

    /** ADMIN only. keyword matches the username, the full name or the label; dates are inclusive. */
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(Long userId, String keyword, String entityType, String method,
                                               Long actorId, LocalDate from, LocalDate to, Pageable pageable) {
        currentUserLoader.loadWithAnyRole(userId, RoleName.ADMIN);
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                parts.add(cb.or(cb.like(cb.lower(root.get("username")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("fullName"), "")), like),
                        cb.like(cb.lower(root.get("actionLabel")), like),
                        cb.like(cb.lower(root.get("path")), like)));
            }
            if (entityType != null && !entityType.isBlank()) {
                parts.add(cb.equal(root.get("entityType"), entityType));
            }
            if (method != null && !method.isBlank()) {
                parts.add(cb.equal(root.get("method"), method.toUpperCase(Locale.ROOT)));
            }
            if (actorId != null) {
                parts.add(cb.equal(root.get("userId"), actorId));
            }
            if (from != null) {
                parts.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            }
            if (to != null) {
                parts.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            }
            return cb.and(parts.toArray(Predicate[]::new));
        };
        return PageResponse.from(repository.findAll(spec, pageable).map(AuditLogService::toResponse));
    }

    /** Entity types that appear in the trail, with their Vietnamese names, for the filter. */
    @Transactional(readOnly = true)
    public List<EntityTypeOption> entityTypes(Long userId) {
        currentUserLoader.loadWithAnyRole(userId, RoleName.ADMIN);
        return repository.findEntityTypes().stream()
                .map(type -> new EntityTypeOption(type, AuditRoute.entityLabel(type))).toList();
    }

    public record EntityTypeOption(String code, String label) {
    }

    private static AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getUserId(), log.getUsername(), log.getFullName(), log.getRole(),
                log.getMethod(), log.getPath(), log.getActionLabel(), log.getEntityType(),
                AuditRoute.entityLabel(log.getEntityType()), log.getEntityId(), log.getStatus(), log.getIp(),
                log.getRequestData(), log.getCreatedAt());
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
