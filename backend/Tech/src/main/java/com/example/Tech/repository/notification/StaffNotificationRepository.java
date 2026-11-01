package com.example.Tech.repository.notification;

import com.example.Tech.dto.response.notification.StaffNotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

/**
 * Staff notifications and who has read them. A caller sees the notifications of {@code storeId} and those without a
 * store; {@code allStores} (ADMIN) sees every one. Plain SQL: the per-reader flag is a join, not an entity.
 */
@Repository
@RequiredArgsConstructor
public class StaffNotificationRepository {

    private static final String VISIBLE = " (:allStores or n.store_id = :storeId or n.store_id is null) ";

    private final NamedParameterJdbcTemplate jdbc;

    public void insert(Integer storeId, String kind, String title, String message, String link) {
        jdbc.update("insert into staff_notifications (store_id, kind, title, message, link) "
                        + "values (:storeId, :kind, :title, :message, :link)",
                new MapSqlParameterSource().addValue("storeId", storeId).addValue("kind", kind)
                        .addValue("title", title).addValue("message", message).addValue("link", link));
    }

    public List<StaffNotificationResponse> page(Long userId, boolean allStores, int storeId, boolean unreadOnly,
                                                int limit, long offset) {
        return jdbc.query("select n.notification_id, n.kind, n.title, n.message, n.link, n.store_id, s.name store_name, "
                        + "n.created_at, (r.user_id is not null) is_read from staff_notifications n "
                        + "left join stores s on s.store_id = n.store_id "
                        + "left join staff_notification_reads r on r.notification_id = n.notification_id and r.user_id = :userId "
                        + "where" + VISIBLE + (unreadOnly ? "and r.user_id is null " : "")
                        + "order by n.created_at desc, n.notification_id desc limit :limit offset :offset",
                params(userId, allStores, storeId).addValue("limit", limit).addValue("offset", offset),
                (rs, i) -> new StaffNotificationResponse(rs.getLong("notification_id"), rs.getString("kind"),
                        rs.getString("title"), rs.getString("message"), rs.getString("link"),
                        (Integer) rs.getObject("store_id"), rs.getString("store_name"), rs.getBoolean("is_read"),
                        rs.getObject("created_at", Timestamp.class).toLocalDateTime()));
    }

    public long count(Long userId, boolean allStores, int storeId, boolean unreadOnly) {
        Long total = jdbc.queryForObject("select count(*) from staff_notifications n "
                        + "left join staff_notification_reads r on r.notification_id = n.notification_id and r.user_id = :userId "
                        + "where" + VISIBLE + (unreadOnly ? "and r.user_id is null" : ""),
                params(userId, allStores, storeId), Long.class);
        return total == null ? 0 : total;
    }

    public boolean isVisible(Long id, boolean allStores, int storeId) {
        Long found = jdbc.queryForObject("select count(*) from staff_notifications n where n.notification_id = :id and"
                + VISIBLE, params(0L, allStores, storeId).addValue("id", id), Long.class);
        return found != null && found > 0;
    }

    public void markRead(Long id, Long userId) {
        jdbc.update("insert into staff_notification_reads (notification_id, user_id) values (:id, :userId) "
                + "on conflict do nothing", new MapSqlParameterSource("id", id).addValue("userId", userId));
    }

    public int markAllRead(Long userId, boolean allStores, int storeId) {
        return jdbc.update("insert into staff_notification_reads (notification_id, user_id) "
                + "select n.notification_id, :userId from staff_notifications n where" + VISIBLE
                + "on conflict do nothing", params(userId, allStores, storeId));
    }

    private static MapSqlParameterSource params(Long userId, boolean allStores, int storeId) {
        return new MapSqlParameterSource("userId", userId).addValue("allStores", allStores).addValue("storeId", storeId);
    }
}
