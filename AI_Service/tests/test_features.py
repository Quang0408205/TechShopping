"""Điểm quan tâm r(u,i), chuẩn hoá Min-Max và vector đặc trưng sản phẩm."""

import math

import numpy as np
import pandas as pd
import pytest

from app.recommender.features import build_item_features, interaction_scores, min_max
from tests.conftest import catalog_frame, interactions_frame

LN2 = math.log(2)


def test_interaction_score_adds_weighted_log_counts():
    # thêm giỏ (w = 4) + bấm (w = 2), mỗi hành vi 1 lần: 4·ln 2 + 2·ln 2 = 4,159
    scores = interaction_scores(interactions_frame([(72, 11, "ADD_TO_CART", 1), (72, 11, "CLICK", 1)]))

    assert len(scores) == 1
    assert scores["r"].iloc[0] == pytest.approx(6 * LN2)
    assert round(scores["r"].iloc[0], 3) == 4.159


@pytest.mark.parametrize("behaviour, weight", [("VIEW", 1), ("CLICK", 2), ("LIKE", 3), ("ADD_TO_CART", 4),
                                               ("PURCHASE", 5)])
def test_each_behaviour_has_its_weight(behaviour, weight):
    scores = interaction_scores(interactions_frame([(1, 1, behaviour, 1)]))

    assert scores["r"].iloc[0] == pytest.approx(weight * LN2)


def test_repeated_views_grow_slower_than_the_count():
    # xem 3 lần = ln 4 = 1,386, không phải 3 lần ln 2 = 2,079
    once = interaction_scores(interactions_frame([(1, 1, "VIEW", 1)]))["r"].iloc[0]
    three = interaction_scores(interactions_frame([(1, 1, "VIEW", 3)]))["r"].iloc[0]

    assert three == pytest.approx(math.log(4))
    assert once < three < 3 * once


def test_scores_are_per_user_and_product():
    scores = interaction_scores(interactions_frame([(1, 1, "VIEW", 1), (1, 2, "VIEW", 1), (2, 1, "PURCHASE", 1)]))

    assert len(scores) == 3
    assert scores.set_index(["user_id", "product_id"]).loc[(2, 1), "r"] == pytest.approx(5 * LN2)


def test_min_max_maps_to_zero_one_and_constant_to_zero():
    assert list(min_max(pd.Series([10.0, 15.0, 20.0]))) == [0.0, 0.5, 1.0]
    assert list(min_max(pd.Series([7.0, 7.0]))) == [0.0, 0.0]


def test_item_vectors_one_hot_and_normalised_within_category():
    items, vectors = build_item_features(catalog_frame(), pd.DataFrame(columns=["product_id", "spec_name",
                                                                                  "spec_value"]))

    assert vectors.shape[0] == len(items)
    assert np.all((vectors >= 0) & (vectors <= 1))
    # giá chuẩn hoá trong từng danh mục: điện thoại rẻ nhất = 0, đắt nhất = 1
    phones = items[items["category_id"] == 1]
    assert phones["price_norm"].min() == 0.0 and phones["price_norm"].max() == 1.0
    assert phones.loc[1, "price_norm"] == 1.0          # iPhone 17 đắt nhất nhóm điện thoại
    # phân khúc giá theo phân vị 1/3, 2/3 trong danh mục
    assert phones.loc[4, "price_segment"] == "Thấp"    # Galaxy A56
    assert phones.loc[1, "price_segment"] == "Cao"
    # RAM / bộ nhớ đọc từ phiên bản ("1 TB" = 1024 GB)
    assert items.loc[6, "storage_gb"] == 1024
    assert items.loc[3, "ram_gb"] == 12


def test_time_decay_halves_the_score_after_one_half_life():
    now = pd.Timestamp("2026-10-31")
    events = interactions_frame([(1, 1, "PURCHASE", 1), (1, 2, "PURCHASE", 1)])
    events.loc[0, "last_at"] = now
    events.loc[1, "last_at"] = now - pd.Timedelta(days=30)

    scores = interaction_scores(events, half_life_days=30, now=now).set_index("product_id")["r"]

    assert scores[1] == pytest.approx(5 * LN2)          # hôm nay: giữ nguyên
    assert scores[2] == pytest.approx(5 * LN2 / 2)      # 30 ngày trước, nửa đời 30 ngày: còn một nửa


def test_no_decay_by_default():
    events = interactions_frame([(1, 1, "VIEW", 1)])
    events.loc[0, "last_at"] = pd.Timestamp("2020-01-01")

    assert interaction_scores(events)["r"].iloc[0] == pytest.approx(LN2)
