"""Chỉ số đánh giá, cách chia dữ liệu theo thời gian và khoảng tin cậy bootstrap (báo cáo mục 3.7)."""

import math

import numpy as np
import pandas as pd
import pytest
from scipy.sparse import csr_matrix

from app.recommender.evaluation import (bootstrap_mean_ci, intra_list_diversity, item_knn_scores, metrics, split,
                                         split3, split_baskets)


def test_metrics_match_the_worked_example():
    # đúng ở vị trí 1 và 4, đáp án có 3 sản phẩm
    m = metrics([1, 9, 8, 2, 7, 6, 5, 4, 3, 0], {1, 2, 99}, n=10)

    assert m["precision"] == pytest.approx(0.2)
    assert m["recall"] == pytest.approx(2 / 3)
    assert m["hitRate"] == 1.0
    dcg = 1 / math.log2(2) + 1 / math.log2(5)
    idcg = 1 / math.log2(2) + 1 / math.log2(3) + 1 / math.log2(4)
    assert m["ndcg"] == pytest.approx(dcg / idcg)
    assert round(m["ndcg"], 3) == 0.671


def test_ndcg_rewards_hits_near_the_top():
    early = metrics([1, 2, 0, 0, 0, 0, 0, 0, 0, 0], {1, 2, 99}, n=10)["ndcg"]
    late = metrics([0, 0, 0, 0, 0, 0, 0, 0, 1, 2], {1, 2, 99}, n=10)["ndcg"]

    assert round(late, 3) == 0.277
    assert early > late


def test_metrics_with_no_hit_and_perfect_list():
    assert metrics([5, 6, 7], {1}, n=3) == {"precision": 0.0, "recall": 0.0, "hitRate": 0.0, "ndcg": 0.0}
    assert metrics([1, 2, 3], {1, 2, 3}, n=3)["ndcg"] == pytest.approx(1.0)


def events_of(users: int, per_user: int) -> pd.DataFrame:
    rows = [{"user_id": u, "product_id": u * 100 + k, "interaction_type": "VIEW",
             "timestamp": pd.Timestamp("2026-10-01") + pd.Timedelta(hours=k)}
            for u in range(users) for k in range(per_user)]
    return pd.DataFrame(rows)


def test_split_is_chronological_per_user():
    early, late = split(events_of(2, 10), 0.8)

    assert len(early) == 16 and len(late) == 4
    for user in (0, 1):
        assert early[early["user_id"] == user]["timestamp"].max() < late[late["user_id"] == user]["timestamp"].min()


def test_split3_gives_train_validation_test_in_time_order():
    train, validation, test = split3(events_of(3, 20), train_ratio=0.7, validation_ratio=0.1)

    assert (len(train), len(validation), len(test)) == (42, 6, 12)   # 70% / 10% / 20% của 60 lượt
    for user in range(3):
        t, v, s = (part[part["user_id"] == user]["timestamp"] for part in (train, validation, test))
        assert t.max() < v.min() and v.max() < s.min()
    assert set(train.columns) == {"user_id", "product_id", "interaction_type", "timestamp"}


def test_bootstrap_interval_contains_the_mean_and_is_reproducible():
    values = np.random.default_rng(0).uniform(0, 0.3, size=300)
    samples = np.random.default_rng(42).integers(0, 300, size=(1000, 300))

    low, high = bootstrap_mean_ci(values, samples)

    assert low < values.mean() < high
    assert high - low < 0.03
    assert bootstrap_mean_ci(values, samples) == (low, high)


def test_bootstrap_of_a_clear_difference_excludes_zero():
    rng = np.random.default_rng(1)
    better = rng.uniform(0.1, 0.3, size=200)
    worse = better - 0.05 + rng.normal(0, 0.01, size=200)
    samples = np.random.default_rng(42).integers(0, 200, size=(1000, 200))

    low, high = bootstrap_mean_ci(better - worse, samples)

    assert low > 0
    assert low < 0.05 < high


def test_item_knn_scores_items_seen_by_the_same_customers():
    # khách 0–2 cùng thích sản phẩm 0 và 1; khách 3 thích sản phẩm 2
    ratings = csr_matrix(np.array([[1, 1, 0], [1, 1, 0], [2, 1, 0], [0, 0, 3]], dtype=float))

    scores = item_knn_scores(ratings[np.array([0])], ratings)   # khách 0: điểm của từng sản phẩm

    assert scores[2] == 0                       # không ai vừa thích 0/1 vừa thích 2
    assert scores[0] > 0 and scores[1] > 0      # 0 và 1 được gợi ý từ nhau (sim(i, i) bị bỏ)


def test_intra_list_diversity():
    same = np.array([[1.0, 0.0], [1.0, 0.0]])
    opposite = np.array([[1.0, 0.0], [0.0, 1.0]])

    assert intra_list_diversity(same) == pytest.approx(0.0)
    assert intra_list_diversity(opposite) == pytest.approx(1.0)
    assert intra_list_diversity(same[:1]) == 0.0


def test_split_baskets_by_order_date():
    baskets = pd.DataFrame([{"order_id": o, "user_id": 1, "product_id": p,
                             "order_date": pd.Timestamp("2026-10-01") + pd.Timedelta(days=o)}
                            for o in range(10) for p in (1, 2)])

    early, late = split_baskets(baskets, 0.8)

    assert set(early["order_id"]) == set(range(8)) and set(late["order_id"]) == {8, 9}
    assert len(early) == 16                     # cả hai sản phẩm của mỗi đơn đi cùng nhau
