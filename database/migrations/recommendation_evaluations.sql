-- Lịch sử đánh giá offline của hệ thống khuyến nghị (AI_Service/app/recommender/evaluation.py ghi sau mỗi lần chạy:
-- scripts/evaluate.py hoặc nút "Đánh giá lại" ở trang admin "Gợi ý sản phẩm"). Một lần chạy = một dòng
-- recommendation_evaluations (cách chia dữ liệu, tham số chọn trên validation, mua kèm / nâng cấp) + các dòng
-- recommendation_evaluation_rows theo phần: VALIDATION (chọn α), TEST (kết quả kiểm thử, kèm khoảng tin cậy 95% của
-- NDCG; HYBRID_MMR = Hybrid xếp lại bằng MMR với mmr_lambda), TEST_STRONG (chỉ tính thêm giỏ / mua là đúng),
-- COLD_START (khách mới 1 đến k − 1 lượt).
-- Chạy lại được.

create table if not exists recommendation_evaluations (
    evaluation_id bigserial primary key,
    evaluated_at timestamp not null unique,
    top_n int not null,
    train_ratio decimal(3, 2) not null,
    validation_ratio decimal(3, 2) not null,
    cold_start_k int not null,
    users int not null,
    validation_users int not null,
    strong_users int not null,
    cold_users int not null,
    train_events int not null,
    validation_events int not null,
    test_events int not null,
    products int not null,
    configured_alpha decimal(3, 2) not null,
    best_alpha decimal(3, 2),
    als_factors int not null,
    als_regularization decimal(6, 3) not null,
    als_confidence decimal(6, 2) not null,
    half_life_days decimal(6, 1) not null,
    mmr_lambda decimal(3, 2) not null default 1,           -- λ của MMR chọn trên validation (1 = không xếp lại)
    cross_sell_hit_rate decimal(6, 4),
    cross_sell_baseline_hit_rate decimal(6, 4),
    upsell_share decimal(6, 4),
    bootstrap_samples int not null,
    seconds decimal(8, 1)
);

create table if not exists recommendation_evaluation_rows (
    evaluation_id bigint not null references recommendation_evaluations(evaluation_id) on delete cascade,
    position int not null,
    split varchar(12) not null,
    model varchar(50) not null,
    kind varchar(20) not null,
    alpha decimal(3, 2),
    mmr_lambda decimal(3, 2),
    precision_at_n decimal(6, 4) not null,
    recall_at_n decimal(6, 4) not null,
    ndcg_at_n decimal(6, 4) not null,
    ndcg_low decimal(6, 4),
    ndcg_high decimal(6, 4),
    hit_rate_at_n decimal(6, 4) not null,
    coverage decimal(6, 4),
    diversity decimal(6, 4),
    primary key (evaluation_id, position),
    constraint chk_recommendation_evaluation_rows_split
        check (split in ('VALIDATION', 'TEST', 'TEST_STRONG', 'COLD_START')),
    constraint chk_recommendation_evaluation_rows_kind
        check (kind in ('POPULAR', 'KNN', 'CBF', 'CF', 'HYBRID', 'HYBRID_MMR'))
);
