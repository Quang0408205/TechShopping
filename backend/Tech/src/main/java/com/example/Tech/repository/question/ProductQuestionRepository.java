package com.example.Tech.repository.question;

import com.example.Tech.dto.response.question.AdminQuestionResponse;
import com.example.Tech.dto.response.question.ProductQuestionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/** Product questions in plain SQL: the lists join the asker and the product, which no entity needs to know. */
@Repository
@RequiredArgsConstructor
public class ProductQuestionRepository {

    private static final String ADMIN_SELECT = "select q.question_id, q.product_id, p.name product_name, q.question, q.answer, "
            + "u.fullname asker_name, u.email asker_email, a.fullname answered_by_name, q.is_hidden, q.created_at, q.answered_at "
            + "from product_questions q join products p on p.product_id = q.product_id join users u on u.user_id = q.user_id "
            + "left join users a on a.user_id = q.answered_by ";

    private final NamedParameterJdbcTemplate jdbc;

    public boolean productIsOnSale(Long productId) {
        Long found = jdbc.queryForObject("select count(*) from products where product_id = :id and deleted_at is null and is_active",
                new MapSqlParameterSource("id", productId), Long.class);
        return found != null && found > 0;
    }

    public String productName(Long productId) {
        return jdbc.queryForObject("select name from products where product_id = :id",
                new MapSqlParameterSource("id", productId), String.class);
    }

    public long countSince(Long userId, LocalDateTime since) {
        Long total = jdbc.queryForObject("select count(*) from product_questions where user_id = :userId and created_at >= :since",
                new MapSqlParameterSource("userId", userId).addValue("since", Timestamp.valueOf(since)), Long.class);
        return total == null ? 0 : total;
    }

    public Long insert(Long productId, Long userId, String question) {
        return jdbc.queryForObject("insert into product_questions (product_id, user_id, question) values (:productId, :userId, :question) "
                        + "returning question_id",
                new MapSqlParameterSource("productId", productId).addValue("userId", userId).addValue("question", question), Long.class);
    }

    public List<ProductQuestionResponse> answeredPage(Long productId, int limit, long offset) {
        return jdbc.query("select q.question_id, q.question, q.answer, u.fullname asker_name, q.created_at, q.answered_at "
                        + "from product_questions q join users u on u.user_id = q.user_id "
                        + "where q.product_id = :productId and q.answer is not null and not q.is_hidden "
                        + "order by q.answered_at desc, q.question_id desc limit :limit offset :offset",
                new MapSqlParameterSource("productId", productId).addValue("limit", limit).addValue("offset", offset),
                (rs, i) -> new ProductQuestionResponse(rs.getLong("question_id"), rs.getString("question"), rs.getString("answer"),
                        givenName(rs.getString("asker_name")), time(rs, "created_at"), time(rs, "answered_at")));
    }

    public long answeredCount(Long productId) {
        Long total = jdbc.queryForObject("select count(*) from product_questions where product_id = :productId "
                + "and answer is not null and not is_hidden", new MapSqlParameterSource("productId", productId), Long.class);
        return total == null ? 0 : total;
    }

    /** status: PENDING (no answer, visible), ANSWERED (answered, visible), HIDDEN, or anything else for all. */
    public List<AdminQuestionResponse> adminPage(String status, String keyword, int limit, long offset) {
        MapSqlParameterSource params = filterParams(keyword).addValue("limit", limit).addValue("offset", offset);
        return jdbc.query(ADMIN_SELECT + adminWhere(status, keyword) + "order by q.created_at desc, q.question_id desc limit :limit offset :offset",
                params, (rs, i) -> new AdminQuestionResponse(rs.getLong("question_id"), rs.getLong("product_id"), rs.getString("product_name"),
                        rs.getString("question"), rs.getString("answer"), rs.getString("asker_name"), rs.getString("asker_email"),
                        rs.getString("answered_by_name"), rs.getBoolean("is_hidden"), time(rs, "created_at"), time(rs, "answered_at")));
    }

    public long adminCount(String status, String keyword) {
        Long total = jdbc.queryForObject("select count(*) from product_questions q join products p on p.product_id = q.product_id "
                + "join users u on u.user_id = q.user_id " + adminWhere(status, keyword), filterParams(keyword), Long.class);
        return total == null ? 0 : total;
    }

    public AdminQuestionResponse findForAdmin(Long id) {
        List<AdminQuestionResponse> rows = jdbc.query(ADMIN_SELECT + "where q.question_id = :id", new MapSqlParameterSource("id", id),
                (rs, i) -> new AdminQuestionResponse(rs.getLong("question_id"), rs.getLong("product_id"), rs.getString("product_name"),
                        rs.getString("question"), rs.getString("answer"), rs.getString("asker_name"), rs.getString("asker_email"),
                        rs.getString("answered_by_name"), rs.getBoolean("is_hidden"), time(rs, "created_at"), time(rs, "answered_at")));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public int answer(Long id, String answer, Long staffId, LocalDateTime now) {
        return jdbc.update("update product_questions set answer = :answer, answered_by = :staffId, answered_at = :now where question_id = :id",
                new MapSqlParameterSource("id", id).addValue("answer", answer).addValue("staffId", staffId).addValue("now", Timestamp.valueOf(now)));
    }

    public int setHidden(Long id, boolean hidden) {
        return jdbc.update("update product_questions set is_hidden = :hidden where question_id = :id",
                new MapSqlParameterSource("id", id).addValue("hidden", hidden));
    }

    private static String adminWhere(String status, String keyword) {
        String where = switch (status == null ? "" : status.toUpperCase()) {
            case "PENDING" -> "where q.answer is null and not q.is_hidden ";
            case "ANSWERED" -> "where q.answer is not null and not q.is_hidden ";
            case "HIDDEN" -> "where q.is_hidden ";
            default -> "where true ";
        };
        if (keyword != null && !keyword.isBlank()) {
            where += "and (lower(q.question) like :kw or lower(p.name) like :kw or lower(u.fullname) like :kw or lower(u.email) like :kw) ";
        }
        return where;
    }

    private static MapSqlParameterSource filterParams(String keyword) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (keyword != null && !keyword.isBlank()) {
            params.addValue("kw", "%" + keyword.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        return params;
    }

    /** "Nguyễn Văn Quang" is shown as "Quang": enough for a public page. */
    static String givenName(String fullname) {
        if (fullname == null || fullname.isBlank()) {
            return "Khách hàng";
        }
        String[] words = fullname.trim().split("\\s+");
        return words[words.length - 1];
    }

    private static LocalDateTime time(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }
}
