"""CBF, Hybrid, khách mới, mua kèm, nâng cấp (báo cáo mục 3.5.4–3.5.6); số liệu trùng ví dụ tính tay của báo cáo."""

from datetime import datetime

import numpy as np
import pandas as pd
import pytest

from app import config
from app.recommender import engine
from app.recommender.evaluation import intra_list_diversity
from tests.conftest import interactions_frame

# Ví dụ "khách An": vector [điện thoại, laptop, Apple, giá chuẩn hoá]
VECTORS = {
    "A": [1, 0, 1, 0.8],   # iPhone 17 (đã thêm giỏ + bấm)
    "B": [0, 1, 0, 0.6],   # laptop Lenovo (đã mua)
    "C": [1, 0, 1, 0.4],   # iPhone 16e
    "D": [1, 0, 0, 0.5],   # Samsung
    "E": [0, 1, 0, 0.7],   # Lenovo Yoga
}


def hand_model() -> engine.Model:
    names = list(VECTORS)
    created = datetime(2026, 10, 1)
    items = pd.DataFrame({"name": names, "category_id": [1, 2, 1, 1, 2], "brand": ["x"] * 5, "price": [1.0] * 5,
                          "is_active": [True] * 5, "review_count": [0] * 5, "updated_at": [created] * 5,
                          "created_at": [created] * 5}, index=pd.Index(range(1, 6), name="product_id"))
    vectors = engine._unit_rows(np.array([VECTORS[k] for k in names], dtype=float))
    return engine.Model(items=items, vectors=vectors, index={pid: row for row, pid in enumerate(items.index)},
                        item_factors=np.zeros((5, 2)), popularity=np.zeros(5), co_purchase={},
                        relations=pd.DataFrame(), trained_at=created)


def test_cbf_profile_and_cosine_match_the_worked_example(monkeypatch):
    monkeypatch.setattr(config, "COLD_START_K", 5)       # ví dụ tính tay của báo cáo dùng k = 5
    # An: iPhone 17 thêm giỏ + bấm (r = 4,159), laptop Lenovo mua (r = 3,466)
    events = interactions_frame([(1, 1, "ADD_TO_CART", 1), (1, 1, "CLICK", 1), (1, 2, "PURCHASE", 1)])
    rec = engine.Recommender(load_interactions=lambda user_id=None: events)
    rec._model = hand_model()

    cbf, cf = rec._cbf_cf(rec._user_state(1))

    # các vector sản phẩm được chuẩn hoá độ dài 1 trước khi lấy trung bình có trọng số thành hồ sơ khách
    assert round(cbf[2], 3) == 0.748   # iPhone 16e
    assert round(cbf[3], 3) == 0.662   # Samsung
    assert round(cbf[4], 3) == 0.765   # Lenovo Yoga
    assert cf is None                  # 3 lượt < k = 5: chưa dùng ALS


def test_hybrid_min_max_then_weighted_sum_matches_the_worked_example():
    # iPhone 16e, Samsung, Lenovo Yoga: CBF từ ví dụ trên, CF = x_An · y với x_An = [0,9; 0,3]
    cbf = np.array([0.748, 0.662, 0.765])
    cf = np.array([0.93, 0.18, 0.36])

    score, s_cbf, s_cf = engine.Recommender()._hybrid(cbf, cf, np.arange(3), alpha=0.4)

    assert s_cbf == pytest.approx([0.835, 0.0, 1.0], abs=1e-3)
    assert s_cf == pytest.approx([1.0, 0.0, 0.24])
    assert score == pytest.approx([0.934, 0.0, 0.544], abs=1e-3)
    assert list(np.argsort(-score)) == [0, 2, 1]   # iPhone 16e nhờ ALS vượt Lenovo Yoga dù CBF thấp hơn


def test_hybrid_without_cf_is_cbf_only():
    score, s_cbf, s_cf = engine.Recommender()._hybrid(np.array([0.2, 0.6, 0.4]), None, np.arange(3), alpha=0.4)

    assert s_cf is None
    assert score == pytest.approx([0.0, 1.0, 0.5])


def test_unit_rows_and_min_max_helpers():
    rows = engine._unit_rows(np.array([[3.0, 4.0], [0.0, 0.0]]))
    assert rows[0] == pytest.approx([0.6, 0.8])
    assert list(rows[1]) == [0.0, 0.0]
    assert list(engine._min_max(np.array([2.0, 2.0]))) == [0.0, 0.0]


def test_co_purchase_keeps_pairs_with_lift_above_one_and_two_orders():
    # 20 đơn: iPhone (1) ở 10 đơn, ốp (8) ở 6 đơn, cả hai ở 5 đơn; sạc (4) ở 13 đơn, cùng iPhone 5 đơn
    baskets = ([(o, p) for o in range(1, 6) for p in (1, 8, 4)] + [(o, 1) for o in range(6, 11)] + [(11, 8)]
               + [(o, 4) for o in range(12, 20)] + [(20, 3), (20, 5)])
    result = engine._co_purchase(pd.DataFrame(baskets, columns=["order_id", "product_id"]))

    partner, confidence, lift = next(entry for entry in result[1] if entry[0] == 8)
    assert confidence == pytest.approx(0.5)            # 5 / 10
    assert lift == pytest.approx(0.5 / (6 / 20))       # 1,667: mua iPhone → mua ốp nhiều hơn bình thường
    assert 4 not in [entry[0] for entry in result[1]]  # lift(iPhone → sạc) = 0,5 / 0,65 < 1
    assert 3 not in result                             # cặp chỉ xuất hiện trong 1 đơn


# ---------- trên catalogue nhỏ (conftest) ----------

def test_user_with_enough_interactions_gets_hybrid_without_seen_or_inactive_products(toy_recommender):
    recs = toy_recommender.recommend(200, n=5)
    ids = [r.product_id for r in recs]

    assert recs and all(r.cf_score is not None for r in recs)   # ≥ k lượt → có điểm ALS
    assert not {1, 2, 7} & set(ids)                             # đã tương tác
    assert 9 not in ids                                         # ngừng bán
    assert [r.score for r in recs] == sorted([r.score for r in recs], reverse=True)


def test_new_user_below_k_gets_cbf_only(toy_recommender, monkeypatch):
    monkeypatch.setattr(config, "COLD_START_K", 5)
    recs = toy_recommender.recommend(201, n=3)

    assert recs and all(r.cf_score is None for r in recs)


def test_cold_start_k_can_be_passed_without_changing_config(toy_recommender, monkeypatch):
    # đánh giá offline thử nhiều ngưỡng k: truyền tham số, không sửa config.COLD_START_K đang phục vụ khách
    monkeypatch.setattr(config, "COLD_START_K", 5)
    forced = toy_recommender.recommend_alphas(201, 3, [0.5], cold_start_k=1)[0]
    default = toy_recommender.recommend_alphas(201, 3, [0.5])[0]

    assert forced and any(r.cf_score is not None for r in forced)
    assert all(r.cf_score is None for r in default)
    assert config.COLD_START_K == 5


def test_unknown_user_gets_popular_products(toy_recommender):
    recs = toy_recommender.recommend(999, n=4)

    assert len(recs) == 4
    assert 9 not in [r.product_id for r in recs]
    assert {r.reason for r in recs} <= {"Được nhiều khách hàng quan tâm", "Sản phẩm nổi bật", "Sản phẩm mới"}


def test_upsell_same_category_at_most_30_percent_dearer_with_better_specs(toy_recommender):
    # iPhone 16e 16,99 triệu → tối đa 22,09 triệu: Galaxy S25 (20 triệu, RAM 12 GB, 256 GB) hợp lệ;
    # iPhone 17 (28,49 triệu) quá đắt, Galaxy A56 rẻ hơn
    recs = toy_recommender.upsell(2)

    assert [r.product_id for r in recs] == [3]
    assert recs[0].reason.startswith("Nâng cấp RAM 12GB, bộ nhớ 256GB")


def test_similar_stays_in_the_category(toy_recommender):
    ids = [r.product_id for r in toy_recommender.similar(5, n=3)]

    assert set(ids) <= {6, 7}


def test_recommend_alphas_gives_the_same_lists_as_one_alpha_at_a_time(toy_recommender):
    alphas = [0.0, 0.3, 1.0]
    together = toy_recommender.recommend_alphas(200, 4, alphas)

    for alpha, lists in zip(alphas, together):
        single = toy_recommender.recommend(200, n=4, alpha=alpha)
        assert [r.product_id for r in lists] == [r.product_id for r in single]


def test_cf_reason_names_the_closest_product_the_customer_showed_interest_in(toy_recommender):
    recs = toy_recommender.recommend(200, n=5, alpha=0.0)
    cf_reasons = [r.reason for r in recs if r.reason.startswith("Khách quan tâm")]

    assert cf_reasons
    assert all(any(name in reason for name in ("iPhone 17", "iPhone 16e", "MacBook Air")) for reason in cf_reasons)


def test_model_keeps_its_training_params_for_fold_in(monkeypatch, toy_recommender):
    model = toy_recommender.model

    assert model.param("factors") == model.item_factors.shape[1]
    assert model.param("confidence") == config.ALS_CONFIDENCE
    del model.params                                    # mô hình lưu từ trước khi có params
    assert model.param("regularization") == config.ALS_REGULARIZATION


def test_mmr_skips_a_near_duplicate_of_what_is_already_picked(toy_recommender):
    # 3 ứng viên: A và B giống hệt nhau (cos = 1), C khác hẳn (cos = 0); điểm Hybrid 1,0 / 0,9 / 0,6
    toy_recommender.model.vectors = np.zeros((9, 2))
    toy_recommender.model.vectors[[0, 1, 2]] = [[1, 0], [1, 0], [0, 1]]
    candidates, score = np.array([0, 1, 2]), np.array([1.0, 0.9, 0.6])

    def order(lam):
        return list(candidates[toy_recommender._mmr(candidates, score, 3, lam)])

    assert order(1.0) == [0, 1, 2]          # không xếp lại: theo điểm
    # λ = 0,5: chọn A (0,5·1); rồi B = 0,5·0,9 − 0,5·1 = −0,05 < C = 0,5·0,6 − 0 = 0,3 → C trước B
    assert order(0.5) == [0, 2, 1]
    # λ = 0,8: B = 0,72 − 0,2 = 0,52 > C = 0,48 → điểm vẫn thắng, giữ nguyên thứ tự
    assert order(0.8) == [0, 1, 2]


def test_mmr_keeps_the_same_top_pick_and_list_length(toy_recommender):
    plain = toy_recommender.recommend_alphas(200, 4, [0.5], mmr_lambda=1.0)[0]
    diverse = toy_recommender.recommend_alphas(200, 4, [0.5], mmr_lambda=0.5)[0]

    assert len(diverse) == len(plain)
    assert diverse[0].product_id == plain[0].product_id

    def diversity(recs):
        m = toy_recommender.model
        return intra_list_diversity(m.vectors[[m.index[r.product_id] for r in recs]])

    assert diversity(diverse) >= diversity(plain)
