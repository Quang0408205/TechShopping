"""
Đánh giá offline các mô hình khuyến nghị (báo cáo mục 3.7 / 4.8.3), tinh chỉnh tham số và chọn α cho Hybrid.

Chia theo thời gian cho từng người dùng thành 3 phần: 70% hành vi đầu → huấn luyện, 10% tiếp theo → validation,
20% cuối → kiểm thử.
  1. Tinh chỉnh trên validation (học trên phần huấn luyện), lần lượt:
     a. tham số ALS (số chiều, λ, hệ số tin cậy a), chấm ALS riêng (α = 0);
     b. α = 0; 0,1; …; 1 với ALS đã chỉnh;
     c. nửa đời giảm dần trọng số hành vi cũ (không giảm / 30 / 90 ngày) với α đã chọn.
     Mỗi bước giữ giá trị có NDCG@N cao nhất.
     d. λ của MMR (xếp lại Top-N cho đa dạng): xếp lại gần như luôn làm NDCG giảm một chút, nên không chọn theo NDCG
        cao nhất mà chọn λ có độ đa dạng cao nhất trong các λ giữ được ít nhất 95% NDCG của λ = 1 (không xếp lại).
     e. ngưỡng cold-start k: khách mô phỏng nào cũng có ≥ 12 lượt, nên giả lập khách mới bằng cách chỉ giữ m lượt đầu
        của 30% khách (m = 1, 2, 3, 5, 8, 12; 70% còn lại giữ đủ để ALS vẫn học được), rồi so NDCG của phổ biến / chỉ CBF
        / Hybrid trên chính các khách đó. k = m nhỏ nhất mà từ đó trở đi Hybrid không kém CBF.
  2. Báo cáo: học lại trên huấn luyện + validation với tham số đã chọn, chấm trên phần kiểm thử. Phần kiểm thử không
     tham gia chọn tham số nào, nên con số báo cáo không bị "đẹp" nhờ chọn trên chính nó. Trên phần kiểm thử:
     - so với 4 đối chứng: danh sách phổ biến, item–item kNN, chỉ CBF, chỉ ALS;
     - Hybrid + MMR so với Hybrid không xếp lại: khoảng tin cậy của hiệu NDCG (mất bao nhiêu) và hiệu độ đa dạng
       (được bao nhiêu);
     - "đúng" theo hai cách: mọi tương tác (xem trở lên) và chỉ thêm giỏ / mua (tín hiệu mạnh);
     - khách mới (1 đến k − 1 lượt, chỉ dùng CBF) so với danh sách phổ biến; đường cong cold-start như bước 1e;
     - có / không có hành vi LIKE (danh sách yêu thích): học lại khi bỏ LIKE, so NDCG trên cùng người dùng;
     - mua kèm: đơn hàng 20% cuối (theo ngày đặt), mỗi sản phẩm trong đơn có từ 2 sản phẩm → danh sách mua kèm có chứa
       sản phẩm khác của cùng đơn không (luật kết hợp chỉ học trên đơn 80% đầu);
     - nâng cấp: bao nhiêu sản phẩm có gợi ý nâng cấp và đắt hơn bao nhiêu.
Sản phẩm liên quan (relevant) = sản phẩm người dùng tương tác trong phần được chấm mà CHƯA từng gặp trong phần học
(hệ thống không gợi ý lại sản phẩm đã xem). Min-Max, x_min/x_max và ALS chỉ học trên phần học.

Chỉ số @N:  Precision = |gợi ý ∩ liên quan| / N;   Recall = |gợi ý ∩ liên quan| / |liên quan|;
            HitRate = có ít nhất 1 sản phẩm đúng;   NDCG = Σ 1/log2(vị trí+1) / IDCG;
            Coverage = số sản phẩm khác nhau được gợi ý / số sản phẩm đang bán;
            Diversity = trung bình (1 − cosine) giữa các cặp sản phẩm trong cùng danh sách (vector thuộc tính).
Khoảng tin cậy 95%: bootstrap theo người dùng (lấy lại ngẫu nhiên có hoàn lại B lần, cùng một mẫu cho mọi mô hình),
phân vị 2,5% và 97,5% của trung bình; so sánh hai mô hình = khoảng tin cậy của hiệu NDCG trên cùng người dùng
(không chứa 0 → chênh lệch không phải do may rủi).

Mô hình đánh giá là các bản riêng (Recommender với nguồn hành vi = phần học), nên chạy được ngay trong AI Service mà
không đụng tới mô hình đang phục vụ khách. Kết quả ghi vào evals/results/recommender_latest.* (lần gần nhất, dán vào
báo cáo) và vào database (recommendation_evaluations: lịch sử mọi lần chạy, migration
recommendation_evaluations.sql) — bảng riêng của phần AI, nơi duy nhất AI Service ghi khi đang chạy.
"""

import csv
import dataclasses
import json
import logging
import math
import threading
from collections import Counter
from datetime import datetime
from pathlib import Path

import numpy as np
import pandas as pd
import psycopg
from scipy.sparse import csr_matrix, diags

from app import config
from app.db import connect, fetch_all
from app.recommender import engine
from app.recommender.features import interaction_scores

log = logging.getLogger(__name__)

ALPHAS = (0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0)
ALS_GRID = tuple((factors, regularization, confidence)
                 for factors in (16, 32, 64) for regularization in (0.1, 1.0) for confidence in (5.0, 10.0, 20.0))
HALF_LIVES = (0.0, 30.0, 90.0)
MMR_LAMBDAS = (1.0, 0.9, 0.8, 0.7, 0.6, 0.5)
MMR_MAX_LOSS = 0.05                     # λ được chọn phải giữ ≥ 95% NDCG của danh sách không xếp lại
COLD_START_STEPS = (1, 2, 3, 5, 8, 12)  # số lượt giữ lại khi giả lập khách mới
COLD_START_SHARE = 0.3                  # tỉ lệ khách bị cắt lịch sử trong mỗi lần giả lập
STRONG_TYPES = ("ADD_TO_CART", "PURCHASE")
METRICS = ("precision", "recall", "ndcg", "hitRate")
CROSS_SELL_N = 4
POPULAR, KNN = "popular", "knn"
LATEST = "recommender_latest"
EMPTY_BASKETS = pd.DataFrame(columns=["order_id", "user_id", "product_id"])


def load_events() -> pd.DataFrame:
    rows = fetch_all("""
        select ui.user_id, ui.product_id, ui.interaction_type, ui.timestamp
        from user_interactions ui join products p on p.product_id = ui.product_id and p.deleted_at is null
    """)
    return pd.DataFrame(rows, columns=["user_id", "product_id", "interaction_type", "timestamp"])


def load_baskets() -> pd.DataFrame:
    """Sản phẩm trong từng đơn chưa huỷ, kèm ngày đặt (chia đơn theo thời gian cho phần mua kèm)."""
    rows = fetch_all("""
        select distinct o.order_id, o.user_id, v.product_id, o.order_date
        from orders o
        join order_items oi on oi.order_id = o.order_id
        join product_variants v on v.variant_id = oi.variant_id
        where upper(o.status) <> 'CANCELLED'
    """)
    return pd.DataFrame(rows, columns=["order_id", "user_id", "product_id", "order_date"])


def split(events: pd.DataFrame, ratio: float):
    """Theo thời gian, từng người dùng: hành vi đến phân vị `ratio` của họ → phần đầu, sau đó → phần sau."""
    cutoff = events.groupby("user_id")["timestamp"].quantile(ratio).rename("cutoff")
    events = events.join(cutoff, on="user_id")
    early = events["timestamp"] <= events["cutoff"]
    return events[early].drop(columns="cutoff"), events[~early].drop(columns="cutoff")


def split3(events: pd.DataFrame, train_ratio: float, validation_ratio: float):
    """(huấn luyện, validation, kiểm thử) theo thời gian từng người dùng; kiểm thử = phần còn lại."""
    learn, test = split(events, train_ratio + validation_ratio)
    train, validation = split(learn, train_ratio / (train_ratio + validation_ratio))
    return train, validation, test


def split_baskets(baskets: pd.DataFrame, ratio: float):
    """Đơn đặt đến phân vị `ratio` của ngày đặt → học luật mua kèm, sau đó → chấm."""
    if baskets.empty:
        return baskets, baskets
    order_dates = baskets.groupby("order_id")["order_date"].first()
    cutoff = order_dates.quantile(ratio)
    early_orders = set(order_dates[order_dates <= cutoff].index)
    early = baskets["order_id"].isin(early_orders)
    return baskets[early], baskets[~early]


def aggregate(events: pd.DataFrame) -> pd.DataFrame:
    return (events.groupby(["user_id", "product_id", "interaction_type"], as_index=False)
            .agg(n=("timestamp", "size"), last_at=("timestamp", "max")))


def metrics(recommended: list[int], relevant: set[int], n: int) -> dict:
    hits = [1 if pid in relevant else 0 for pid in recommended[:n]]
    dcg = sum(h / math.log2(i + 2) for i, h in enumerate(hits))
    idcg = sum(1 / math.log2(i + 2) for i in range(min(len(relevant), n)))
    return {"precision": sum(hits) / n, "recall": sum(hits) / len(relevant), "hitRate": float(any(hits)),
            "ndcg": dcg / idcg if idcg else 0.0}


def intra_list_diversity(vectors: np.ndarray) -> float:
    """Trung bình (1 − cosine) giữa mọi cặp trong danh sách; vector đã chuẩn hoá độ dài 1. Dưới 2 sản phẩm → 0."""
    k = len(vectors)
    if k < 2:
        return 0.0
    similarity = vectors @ vectors.T
    upper = similarity[np.triu_indices(k, 1)]
    return float(np.mean(1 - upper))


def bootstrap_mean_ci(values: np.ndarray, samples: np.ndarray, level: float = 0.95) -> tuple[float, float]:
    """Khoảng tin cậy của trung bình: `samples` = ma trận chỉ số người dùng lấy lại (B × số người dùng)."""
    means = values[samples].mean(axis=1)
    tail = (1 - level) / 2 * 100
    low, high = np.percentile(means, [tail, 100 - tail])
    return float(low), float(high)


def item_knn_scores(r_user: csr_matrix, ratings: csr_matrix) -> np.ndarray:
    """
    Item–item kNN (đối chứng): sim(i, j) = cosine giữa hai cột của ma trận r(u,i) (hai sản phẩm được cùng những
    khách quan tâm); điểm của j với khách = Σ_i r(u,i) · sim(i, j), bỏ sim(i, i).
    """
    norms = np.sqrt(np.asarray(ratings.multiply(ratings).sum(axis=0)).ravel())
    normalized = ratings @ diags(np.where(norms > 0, 1 / np.where(norms > 0, norms, 1), 0))
    similarity = (normalized.T @ normalized).tocsr()
    similarity.setdiag(0)
    return np.asarray((r_user @ similarity).todense()).ravel()


def model_name(kind: str, alpha: float | None, mmr_lambda: float | None = None) -> str:
    if kind == "HYBRID_MMR":
        return f"Hybrid α={alpha:.1f} + MMR λ={mmr_lambda:.1f}"
    return {"POPULAR": "Phổ biến (baseline)", "KNN": "Item-kNN (baseline)", "CBF": "CBF (α=1)",
            "CF": "CF/ALS (α=0)", "COLD_CBF": "CBF (khách mới)"}.get(kind) or f"Hybrid α={alpha:.1f}"


def _kind(config_key) -> str:
    if config_key == POPULAR:
        return "POPULAR"
    if config_key == KNN:
        return "KNN"
    if isinstance(config_key, tuple):
        return "HYBRID_MMR"
    return "CBF" if config_key == 1.0 else "CF" if config_key == 0.0 else "HYBRID"


def _alpha_lambda(config_key) -> tuple[float | None, float | None]:
    """Khoá danh sách → (α, λ của MMR): số α = không xếp lại; (α, λ) = Hybrid + MMR; POPULAR / KNN → (None, None)."""
    if config_key in (POPULAR, KNN):
        return None, None
    return config_key if isinstance(config_key, tuple) else (config_key, None)


class Session:
    """Một mô hình học trên `learn_events` (và đơn hàng `baskets`) với một bộ tham số; sinh danh sách để chấm."""

    def __init__(self, learn_events: pd.DataFrame, params: dict, baskets: pd.DataFrame | None = None):
        self.learn_agg = aggregate(learn_events)
        self.now = learn_events["timestamp"].max()
        self.params = params

        def load(user_id=None):
            return self.learn_agg if user_id is None else self.learn_agg[self.learn_agg["user_id"] == user_id]

        self.rec = engine.Recommender(load_interactions=load, now=self.now)
        self.rec._model = engine.train_model(load_interactions=load,
                                             load_baskets=lambda: EMPTY_BASKETS if baskets is None else baskets,
                                             params=params, now=self.now)
        self.model = self.rec.model
        self.valid_count = int(self.model.valid.sum())
        self.seen = learn_events.groupby("user_id")["product_id"].apply(set)
        self.counts = self.learn_agg.groupby("user_id")["n"].sum()
        self._knn = None

    def relevant(self, judge_events: pd.DataFrame, strong_only: bool = False) -> dict[int, set]:
        if strong_only:
            judge_events = judge_events[judge_events["interaction_type"].isin(STRONG_TYPES)]
        return {u: set(g["product_id"]) - self.seen.get(u, set()) for u, g in judge_events.groupby("user_id")}

    def users(self, relevant: dict[int, set], cold: bool = False) -> list[int]:
        """Người dùng có sản phẩm liên quan; từ k lượt trở lên (mặc định) hoặc khách mới 1 đến k − 1 lượt."""
        k = config.COLD_START_K
        keep = (lambda c: 0 < c < k) if cold else (lambda c: c >= k)
        return sorted(u for u, items in relevant.items() if items and keep(self.counts.get(u, 0)))

    def _allowed(self, user: int) -> np.ndarray:
        allowed = self.model.valid.copy()
        allowed[[self.model.index[p] for p in self.seen.get(user, ()) if p in self.model.index]] = False
        return allowed

    def _popular(self, user: int, n: int) -> list[int]:
        return [r.product_id for r in self.rec.popular(n, self._allowed(user))]

    def _knn_list(self, user: int, n: int) -> list[int]:
        if self._knn is None:
            scores = interaction_scores(self.learn_agg, self.model.param("half_life_days"), self.now)
            scores = scores[scores["product_id"].isin(self.model.index.keys())]
            user_rows = {int(u): row for row, u in enumerate(sorted(scores["user_id"].unique()))}
            ratings = csr_matrix((scores["r"].to_numpy(float),
                                  (scores["user_id"].map(user_rows).to_numpy(int),
                                   scores["product_id"].map(self.model.index).to_numpy(int))),
                                 shape=(len(user_rows), len(self.model.index)))
            self._knn = (user_rows, ratings)
        user_rows, ratings = self._knn
        if user not in user_rows:
            return self._popular(user, n)
        score = item_knn_scores(ratings[user_rows[user]], ratings)
        allowed = self._allowed(user) & (score > 0)
        rows = np.flatnonzero(allowed)
        ranked = rows[np.lexsort((-self.model.popularity[rows], -score[rows]))][:n]
        picked = [self.model.items.index[row] for row in ranked]
        return picked + [p for p in self._popular(user, n) if p not in picked][:n - len(picked)]

    def lists(self, users: list[int], keys, n: int, cold_start_k: int | None = None) -> dict:
        """
        {khoá: {người dùng: [product_id]}}; khoá = α (số, không xếp lại), (α, λ) (Hybrid + MMR), POPULAR hoặc KNN;
        các α cùng λ của một khách tính một lần.
        """
        by_lambda: dict[float, list] = {}
        for key in keys:
            if key not in (POPULAR, KNN):
                alpha, lam = _alpha_lambda(key)
                by_lambda.setdefault(1.0 if lam is None else lam, []).append((key, alpha))
        out = {key: {} for key in keys}
        for user in users:
            for lam, group in by_lambda.items():
                recs = self.rec.recommend_alphas(user, n, [alpha for _, alpha in group], mmr_lambda=lam,
                                                 cold_start_k=cold_start_k)
                for (key, _), items in zip(group, recs):
                    out[key][user] = [r.product_id for r in items]
            if POPULAR in out:
                out[POPULAR][user] = self._popular(user, n)
            if KNN in out:
                out[KNN][user] = self._knn_list(user, n)
        return out

    def diversity(self, ids: list[int]) -> float:
        rows = [self.model.index[p] for p in ids if p in self.model.index]
        return intra_list_diversity(self.model.vectors[rows])


def _score(lists_by_user: dict, relevant: dict, users: list[int], n: int) -> pd.DataFrame:
    return pd.DataFrame([metrics(lists_by_user[u], relevant[u], n) for u in users], columns=list(METRICS))


def _row(name: str, kind: str, alpha, table: pd.DataFrame, shown: set | None = None, valid_count: int = 0,
         diversity: float | None = None) -> dict:
    row = {"model": name, "kind": kind, "alpha": alpha,
           **{key: round(float(table[key].mean()) if len(table) else 0.0, 4) for key in METRICS}}
    if shown is not None:
        row["coverage"] = round(len(shown) / valid_count, 4) if valid_count else 0.0
    if diversity is not None:
        row["diversity"] = round(diversity, 4)
    return row


def _samples(size: int, bootstrap: int, seed: int):
    return np.random.default_rng(seed).integers(0, size, size=(bootstrap, size)) if size and bootstrap else None


def _with_ci(row: dict, table: pd.DataFrame, samples) -> dict:
    row["ci"] = ({key: [round(v, 4) for v in bootstrap_mean_ci(table[key].to_numpy(float), samples)] for key in METRICS}
                 if samples is not None else None)
    return row


def _compare(tables: dict, best: str, others: list[str], samples, metric: str = "ndcg") -> list[dict]:
    result = []
    if samples is None:
        return result
    for other in others:
        if other == best or other not in tables:
            continue
        diff = tables[best][metric].to_numpy(float) - tables[other][metric].to_numpy(float)
        low, high = bootstrap_mean_ci(diff, samples)
        result.append({"model": best, "versus": other, "metric": metric, "diff": round(float(diff.mean()), 4),
                       "low": round(low, 4), "high": round(high, 4)})
    return result


def _mean_ndcg(session: Session, judge: pd.DataFrame, keys, n: int) -> tuple[dict, int]:
    relevant = session.relevant(judge)
    users = session.users(relevant)
    lists = session.lists(users, keys, n)
    return {key: float(_score(lists[key], relevant, users, n)["ndcg"].mean()) if users else 0.0 for key in keys}, len(users)


def choose_mmr_lambda(rows: list[dict], max_loss: float = MMR_MAX_LOSS) -> float:
    """rows: [{lambda, ndcg, diversity}] trên validation; λ đa dạng nhất trong các λ giữ ≥ (1 − max_loss)·NDCG(λ = 1)."""
    baseline = next(row["ndcg"] for row in rows if row["lambda"] == 1.0)
    kept = [row for row in rows if row["ndcg"] >= (1 - max_loss) * baseline]
    return max(kept, key=lambda row: (row["diversity"], row["lambda"]))["lambda"]


def _key(alpha: float, lam: float):
    """Khoá danh sách: α (không xếp lại) hoặc (α, λ) khi có MMR."""
    return (alpha, lam) if lam < 1 else alpha


def cold_start_curve(learn: pd.DataFrame, judge: pd.DataFrame, params: dict, alpha: float, lam: float, n: int,
                     seed: int, steps=COLD_START_STEPS, share: float = COLD_START_SHARE) -> list[dict]:
    """
    NDCG@N theo số lượt m mà khách mới có: cắt lịch sử của `share` khách (đủ max(steps) lượt, có sản phẩm mới trong
    `judge`) còn m lượt đầu rồi học lại. Hybrid được ép dùng ALS từ lượt đầu (cold_start_k = 1) để so với chỉ CBF.
    Sản phẩm khách đã gặp ở phần lịch sử bị cắt có thể nằm trong "liên quan"; ảnh hưởng như nhau cho mọi cách gợi ý.
    """
    ordered = learn.sort_values("timestamp", kind="stable")
    counts = ordered.groupby("user_id").size()
    judged = set(judge["user_id"])
    pool = sorted(int(u) for u, c in counts.items() if c >= max(steps) and u in judged)
    rng = np.random.default_rng(seed)
    sample = set(rng.choice(pool, size=int(len(pool) * share), replace=False).tolist()) if pool else set()
    rest, cut = ordered[~ordered["user_id"].isin(sample)], ordered[ordered["user_id"].isin(sample)]
    keys = {"popular": POPULAR, "cbf": _key(1.0, lam), "hybrid": _key(alpha, lam)}
    rows = []
    for m in steps:
        session = Session(pd.concat([rest, cut.groupby("user_id").head(m)]), params)
        relevant = session.relevant(judge)
        users = sorted(u for u in sample if relevant.get(u))
        lists = session.lists(users, list(keys.values()), n, cold_start_k=1)
        row = {"events": m, "users": len(users)}
        for name, key in keys.items():
            row[name] = round(float(_score(lists[key], relevant, users, n)["ndcg"].mean()), 4) if users else 0.0
        rows.append(row)
    return rows


def choose_cold_start_k(rows: list[dict], default: int) -> int:
    """m nhỏ nhất mà ở m đó và mọi m lớn hơn, Hybrid ≥ CBF; không có thì giữ `default`."""
    for i, row in enumerate(rows):
        if all(r["hybrid"] >= r["cbf"] for r in rows[i:]):
            return int(row["events"])
    return default


CROSS_SELL_MAIN = "Mua kèm (đang dùng: cùng đơn + khách mua A cũng mua B + phụ kiện)"
CROSS_SELL_BASELINE = "Bán chạy nhất (baseline)"


def _variant(session: Session, **changes) -> engine.Recommender:
    """Bản sao Recommender của session với vài thành phần mô hình được thay (so sánh từng nguồn luật mua kèm)."""
    rec = engine.Recommender(load_interactions=session.rec._load_interactions, now=session.now)
    rec._model = dataclasses.replace(session.model, **changes)
    return rec


def _cross_sell(session: Session, train_baskets: pd.DataFrame, test_baskets: pd.DataFrame,
                learn_events: pd.DataFrame | None = None, bootstrap: int = 1000, seed: int = 42) -> dict:
    """
    Mỗi (đơn kiểm thử có ≥ 2 sản phẩm, sản phẩm i trong đơn): danh sách mua kèm của i có chứa sản phẩm khác của đơn?
    Luật "khách mua A cũng mua B" chỉ học từ hành vi trước ngày cắt của đơn (không nhìn thấy đơn kiểm thử).
    """
    co_buyers = {}
    if learn_events is not None and not train_baskets.empty and "order_date" in train_baskets:
        cutoff = pd.Timestamp(train_baskets["order_date"].max())
        before = learn_events[pd.to_datetime(learn_events["timestamp"]) <= cutoff]
        co_buyers = engine._co_buyers(aggregate(before), session.model.items, session.model.index)
    recommenders = {
        CROSS_SELL_MAIN: _variant(session, co_buyers=co_buyers),
        "Chỉ luật cùng đơn (cách cũ)": _variant(session, co_buyers={}),
        "Chỉ khách mua A cũng mua B": _variant(session, co_purchase={}, co_buyers=co_buyers),
    }
    popular = [p for p, _ in Counter(train_baskets["product_id"]).most_common()]
    counts = {name: [0, 0, 0] for name in [*recommenders, CROSS_SELL_BASELINE]}   # có gợi ý, trúng, số món trúng
    hit_lists = {name: [] for name in counts}
    pairs = 0
    for _, order in test_baskets.groupby("order_id"):
        products = set(order["product_id"])
        if len(products) < 2:
            continue
        for product in products:
            if product not in session.model.index:
                continue
            others = products - {product}
            pairs += 1
            lists = {name: [r.product_id for r in rec.cross_sell(product, None, CROSS_SELL_N)]
                     for name, rec in recommenders.items()}
            lists[CROSS_SELL_BASELINE] = [p for p in popular if p != product][:CROSS_SELL_N]
            for name, suggested in lists.items():
                found = len(others & set(suggested))
                counts[name][0] += bool(suggested)
                counts[name][1] += found > 0
                counts[name][2] += found
                hit_lists[name].append(float(found > 0))
    share = (lambda x: round(x / pairs, 4) if pairs else 0.0)
    samples = _samples(pairs, bootstrap, seed)
    comparisons = []
    for other in (CROSS_SELL_BASELINE, "Chỉ luật cùng đơn (cách cũ)"):
        diff = np.array(hit_lists[CROSS_SELL_MAIN]) - np.array(hit_lists[other])
        low, high = bootstrap_mean_ci(diff, samples) if samples is not None else (0.0, 0.0)
        comparisons.append({"best": CROSS_SELL_MAIN, "other": other, "metric": "hitRate",
                            "diff": round(float(diff.mean()) if pairs else 0.0, 4), "low": round(low, 4),
                            "high": round(high, 4), "significant": bool(low > 0 or high < 0)})
    return {"n": CROSS_SELL_N, "pairs": pairs, "trainOrders": int(train_baskets["order_id"].nunique()),
            "testOrders": int(test_baskets["order_id"].nunique()),
            "itemsWithRules": len(session.model.co_purchase), "itemsWithCoBuyers": len(co_buyers),
            "withSuggestions": share(counts[CROSS_SELL_MAIN][0]),
            "rows": [{"model": name, "hitRate": share(c[1]), "precision": share(c[2] / CROSS_SELL_N),
                      "withSuggestions": share(c[0])} for name, c in counts.items()],
            "comparisons": comparisons}


def _upsell(session: Session) -> dict:
    """Bao nhiêu sản phẩm đang bán có gợi ý nâng cấp, và gợi ý đầu tiên đắt hơn bao nhiêu %."""
    model = session.model
    prices = model.items["price"].to_numpy(float)
    valid = np.flatnonzero(model.valid)
    increases = []
    for row in valid:
        suggested = session.rec.upsell(int(model.items.index[row]), None, 1)
        if suggested:
            increases.append(prices[model.index[suggested[0].product_id]] / prices[row] - 1)
    return {"products": len(valid), "withSuggestions": round(len(increases) / len(valid), 4) if len(valid) else 0.0,
            "meanPriceIncrease": round(float(np.mean(increases)), 4) if increases else 0.0}


def evaluate(n: int = 10, train_ratio: float = 0.7, validation_ratio: float = 0.1, bootstrap: int = 1000,
             seed: int = 42) -> dict:
    """Tinh chỉnh trên validation, chấm trên kiểm thử; trả về mọi bảng (dạng dict, ghi được ra JSON)."""
    started = datetime.now()
    train, validation, test = split3(load_events(), train_ratio, validation_ratio)
    learn = pd.concat([train, validation])

    # 1a. tham số ALS (chấm ALS riêng, không giảm trọng số theo thời gian)
    als_rows = []
    for factors, regularization, confidence in ALS_GRID:
        params = {"factors": factors, "regularization": regularization, "confidence": confidence,
                  "half_life_days": 0.0}
        ndcg, _ = _mean_ndcg(Session(train, params), validation, [0.0], n)
        als_rows.append({"factors": factors, "regularization": regularization, "confidence": confidence,
                         "ndcg": round(ndcg[0.0], 4)})
    best_als = max(als_rows, key=lambda row: row["ndcg"])
    params = {"factors": best_als["factors"], "regularization": best_als["regularization"],
              "confidence": best_als["confidence"], "half_life_days": 0.0}

    # 1b. α (bảng validation đầy đủ: phổ biến + mọi α)
    session = Session(train, params)
    relevant = session.relevant(validation)
    validation_users = session.users(relevant)
    lists = session.lists(validation_users, [POPULAR, *ALPHAS], n)
    validation_rows = []
    for key in (POPULAR, *ALPHAS):
        shown = {p for ids in lists[key].values() for p in ids}
        kind = _kind(key)
        validation_rows.append(_row(model_name(kind, None if key == POPULAR else key), kind,
                                    None if key == POPULAR else key, _score(lists[key], relevant, validation_users, n),
                                    shown, session.valid_count))
    best_alpha = max((row for row in validation_rows if row["alpha"] is not None), key=lambda row: row["ndcg"])["alpha"]

    # 1c. giảm dần trọng số hành vi cũ, với α đã chọn
    sessions = {0.0: session}
    decay_rows = [{"halfLifeDays": 0.0, "ndcg": next(r["ndcg"] for r in validation_rows if r["alpha"] == best_alpha)}]
    for half_life in HALF_LIVES[1:]:
        sessions[half_life] = Session(train, {**params, "half_life_days": half_life})
        ndcg, _ = _mean_ndcg(sessions[half_life], validation, [best_alpha], n)
        decay_rows.append({"halfLifeDays": half_life, "ndcg": round(ndcg[best_alpha], 4)})
    params["half_life_days"] = max(decay_rows, key=lambda row: row["ndcg"])["halfLifeDays"]

    # 1d. λ của MMR, với α và nửa đời đã chọn: NDCG và độ đa dạng của từng λ
    session = sessions[params["half_life_days"]]
    mmr_keys = [(best_alpha, lam) for lam in MMR_LAMBDAS]
    lists = session.lists(validation_users, mmr_keys, n)
    mmr_rows = []
    for key in mmr_keys:
        table = _score(lists[key], relevant, validation_users, n)
        mmr_rows.append({"lambda": key[1], "ndcg": round(float(table["ndcg"].mean()), 4) if validation_users else 0.0,
                         "diversity": round(float(np.mean([session.diversity(lists[key][u])
                                                           for u in validation_users])), 4) if validation_users else 0.0})
    best_lambda = choose_mmr_lambda(mmr_rows)

    # 1e. ngưỡng cold-start k
    cold_curve_validation = cold_start_curve(train, validation, params, best_alpha, best_lambda, n, seed)
    best_k = choose_cold_start_k(cold_curve_validation, config.COLD_START_K)

    # 2. kiểm thử: học lại trên huấn luyện + validation (đơn hàng: 80% đầu theo ngày đặt)
    train_baskets, test_baskets = split_baskets(load_baskets(), train_ratio + validation_ratio)
    session = Session(learn, params, train_baskets[["order_id", "user_id", "product_id"]])
    keys = [POPULAR, KNN, 1.0, 0.0, best_alpha, config.ALPHA]
    keys += [(alpha, lam) for alpha, lam in ((best_alpha, best_lambda), (config.ALPHA, config.MMR_LAMBDA)) if lam < 1]
    keys = list(dict.fromkeys(keys))
    relevant = session.relevant(test)
    strong = session.relevant(test, strong_only=True)
    users = session.users(relevant)
    strong_users = session.users(strong)
    lists = session.lists(users, keys, n)

    def score_all(relevance: dict, group: list[int], seed_offset: int, with_extras: bool):
        samples = _samples(len(group), bootstrap, seed + seed_offset)
        rows, tables = [], {}
        for key in keys:
            kind = _kind(key)
            alpha, lam = _alpha_lambda(key)
            name = model_name(kind, alpha, lam)
            table = _score(lists[key], relevance, group, n)
            tables[name] = table
            extras = {}
            if with_extras:
                table["diversity"] = [session.diversity(lists[key][u]) for u in group]
                extras = {"shown": {p for u in group for p in lists[key][u]}, "valid_count": session.valid_count,
                          "diversity": float(table["diversity"].mean()) if group else 0.0}
            row = _with_ci(_row(name, kind, alpha, table, **extras), table, samples)
            row["mmrLambda"] = lam
            rows.append(row)
        best = model_name(_kind(best_alpha), best_alpha)
        others = [model_name(k, a) for k, a in (("CF", 0.0), ("CBF", 1.0), ("KNN", None), ("POPULAR", None))]
        mmr = model_name("HYBRID_MMR", best_alpha, best_lambda)
        mmr_comparisons = _compare(tables, mmr, [best], samples) if best_lambda < 1 else []
        if best_lambda < 1 and with_extras:
            mmr_comparisons += _compare(tables, mmr, [best], samples, "diversity")
        return rows, _compare(tables, best, others, samples), mmr_comparisons

    rows, comparisons, mmr_comparisons = score_all(relevant, users, 0, True)
    strong_rows, strong_comparisons, _ = score_all(strong, strong_users, 1, False)

    # khách mới: CBF (hệ thống tự dùng khi dưới k lượt) so với danh sách phổ biến
    cold_users = session.users(relevant, cold=True)
    cold_lists = session.lists(cold_users, [POPULAR, config.ALPHA], n)
    cold_samples = _samples(len(cold_users), bootstrap, seed + 2)
    cold_tables = {model_name("POPULAR", None): _score(cold_lists[POPULAR], relevant, cold_users, n),
                   model_name("COLD_CBF", None): _score(cold_lists[config.ALPHA], relevant, cold_users, n)}
    cold_rows = [_with_ci(_row(name, kind, None, table), table, cold_samples)
                 for (name, table), kind in zip(cold_tables.items(), ("POPULAR", "CBF"))]
    cold_curve_test = cold_start_curve(learn, test, params, best_alpha, best_lambda, n, seed + 3)

    # có / không có LIKE (danh sách yêu thích), cùng tham số, cùng người dùng kiểm thử
    like_rows, like_comparisons = [], []
    likes = int((learn["interaction_type"] == "LIKE").sum())
    if likes:
        no_like = Session(learn[learn["interaction_type"] != "LIKE"], params, train_baskets[["order_id", "user_id", "product_id"]])
        key = _key(best_alpha, best_lambda)
        like_users = [u for u in users if strong.get(u)]
        like_tables = {}
        for label, sess in (("Có LIKE", session), ("Bỏ LIKE", no_like)):
            for target, rel in (("mọi tương tác", relevant), ("thêm giỏ / mua", strong)):
                group = users if target == "mọi tương tác" else like_users
                like_tables[(label, target)] = _score(sess.lists(group, [key], n)[key], rel, group, n)
        for target in ("mọi tương tác", "thêm giỏ / mua"):
            group = users if target == "mọi tương tác" else like_users
            samples = _samples(len(group), bootstrap, seed + (4 if target == "mọi tương tác" else 5))
            pair = {f"{label} ({target})": like_tables[(label, target)] for label in ("Có LIKE", "Bỏ LIKE")}
            like_rows += [_with_ci(_row(name, "HYBRID", best_alpha, table), table, samples) for name, table in pair.items()]
            like_comparisons += _compare(pair, f"Có LIKE ({target})", [f"Bỏ LIKE ({target})"], samples)

    return {
        "evaluatedAt": datetime.now().isoformat(timespec="seconds"),
        "n": n, "trainRatio": train_ratio, "validationRatio": validation_ratio,
        "testRatio": round(1 - train_ratio - validation_ratio, 2), "coldStartK": config.COLD_START_K,
        "likeEvents": likes,
        "users": len(users), "strongUsers": len(strong_users), "coldUsers": len(cold_users),
        "validationUsers": len(validation_users),
        "trainEvents": len(train), "validationEvents": len(validation), "testEvents": len(test),
        "products": session.valid_count, "configuredAlpha": config.ALPHA, "bestAlpha": best_alpha,
        "configured": {"alpha": config.ALPHA, "factors": config.ALS_FACTORS,
                       "regularization": config.ALS_REGULARIZATION, "confidence": config.ALS_CONFIDENCE,
                       "halfLifeDays": config.HALF_LIFE_DAYS, "mmrLambda": config.MMR_LAMBDA,
                       "coldStartK": config.COLD_START_K},
        "chosen": {"alpha": best_alpha, "factors": params["factors"], "regularization": params["regularization"],
                   "confidence": params["confidence"], "halfLifeDays": params["half_life_days"],
                   "mmrLambda": best_lambda, "coldStartK": best_k},
        "bootstrapSamples": bootstrap if users else 0,
        "seconds": round((datetime.now() - started).total_seconds(), 1),
        "tuning": {"als": als_rows, "decay": decay_rows, "mmr": mmr_rows, "mmrMaxLoss": MMR_MAX_LOSS,
                   "coldStart": cold_curve_validation},
        "coldStartCurve": cold_curve_test,
        "likeRows": like_rows, "likeComparisons": like_comparisons,
        "validationRows": validation_rows, "rows": rows, "comparisons": comparisons,
        "mmrComparisons": mmr_comparisons,
        "strongRows": strong_rows, "strongComparisons": strong_comparisons,
        "coldStartRows": cold_rows,
        "coldStartComparisons": _compare(cold_tables, model_name("COLD_CBF", None), [model_name("POPULAR", None)],
                                         cold_samples),
        "crossSell": _cross_sell(session, train_baskets, test_baskets, learn, bootstrap, seed + 6),
        "upsell": _upsell(session),
    }


# ---------- ghi kết quả: JSON (trang admin), CSV + Markdown + biểu đồ SVG (dán vào báo cáo) ----------

def save(result: dict, directory: Path = config.EVAL_RESULTS_DIR) -> None:
    directory.mkdir(parents=True, exist_ok=True)
    (directory / f"{LATEST}.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    n = result["n"]
    with (directory / f"{LATEST}.csv").open("w", newline="", encoding="utf-8-sig") as f:
        writer = csv.writer(f)
        writer.writerow(["Phần", "Mô hình", "alpha", f"Precision@{n}", f"Recall@{n}", f"NDCG@{n}", f"NDCG@{n} thấp",
                         f"NDCG@{n} cao", f"HitRate@{n}", "Coverage", "Diversity"])
        for part, rows in (("Kiểm thử", result["rows"]), ("Kiểm thử (thêm giỏ / mua)", result["strongRows"]),
                           ("Khách mới", result["coldStartRows"]), ("LIKE: có / bỏ", result.get("likeRows", [])),
                           ("Validation", result["validationRows"])):
            for row in rows:
                ci = (row.get("ci") or {}).get("ndcg") or ["", ""]
                writer.writerow([part, row["model"], "" if row["alpha"] is None else row["alpha"], row["precision"],
                                 row["recall"], row["ndcg"], ci[0], ci[1], row["hitRate"], row.get("coverage", ""),
                                 row.get("diversity", "")])
    (directory / f"{LATEST}.md").write_text(markdown(result), encoding="utf-8")
    (directory / "recommender_alpha.svg").write_text(alpha_chart_svg(result), encoding="utf-8")


def load_latest(directory: Path = config.EVAL_RESULTS_DIR) -> dict | None:
    path = directory / f"{LATEST}.json"
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None


def _find(rows: list[dict], kind: str) -> dict | None:
    return next((row for row in rows if row["kind"] == kind), None)


def _pct(value: float) -> str:
    return f"{round(value * 100)}%"


def _table_lines(rows: list[dict], n: int, extras: bool) -> list[str]:
    head = f"| Mô hình | Precision@{n} | Recall@{n} | NDCG@{n} (KTC 95%) | HitRate@{n}"
    lines = [head + (" | Coverage | Diversity |" if extras else " |"),
             "|---|---|---|---|---|" + ("---|---|" if extras else "")]
    for row in rows:
        ci = (row.get("ci") or {}).get("ndcg")
        ndcg = f"{row['ndcg']:.4f}" + (f" ({ci[0]:.4f}–{ci[1]:.4f})" if ci else "")
        line = f"| {row['model']} | {row['precision']:.4f} | {row['recall']:.4f} | {ndcg} | {row['hitRate']:.4f}"
        lines.append(line + (f" | {row['coverage']:.4f} | {row['diversity']:.4f} |" if extras else " |"))
    return lines


def _comparison_lines(comparisons: list[dict], n: int) -> list[str]:
    if not comparisons:
        return []
    lines = ["", "Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):", ""]
    for c in comparisons:
        verdict = "có ý nghĩa" if c["low"] > 0 or c["high"] < 0 else "chưa đủ chắc chắn"
        metric = f"NDCG@{n}" if c["metric"] == "ndcg" else "Diversity"
        lines.append(f"- {metric}: {c['model']} − {c['versus']}: {c['diff']:+.4f} ({c['low']:+.4f} đến "
                     f"{c['high']:+.4f}), {verdict}")
    return lines


def markdown(result: dict) -> str:
    n = result["n"]
    chosen = result["chosen"]
    lines = [
        f"# Đánh giá offline hệ thống khuyến nghị ({result['evaluatedAt'].replace('T', ' ')})",
        "",
        f"- Chia theo thời gian từng người dùng: {_pct(result['trainRatio'])} hành vi đầu để huấn luyện, "
        f"{_pct(result['validationRatio'])} tiếp theo để tinh chỉnh (validation), {_pct(result['testRatio'])} cuối để "
        f"kiểm thử ({result['trainEvents']} / {result['validationEvents']} / {result['testEvents']} lượt).",
        f"- Tham số chọn trên validation ({result['validationUsers']} người dùng): ALS {chosen['factors']} chiều, "
        f"λ = {chosen['regularization']}, a = {chosen['confidence']}; α = {chosen['alpha']}; nửa đời giảm trọng số "
        f"{'không giảm' if not chosen['halfLifeDays'] else str(int(chosen['halfLifeDays'])) + ' ngày'}; MMR "
        f"{_mmr_text(chosen.get('mmrLambda'))}; ngưỡng cold-start k = {chosen.get('coldStartK')}.",
        f"- Kiểm thử: học lại trên huấn luyện + validation, chấm {result['users']} người dùng có ít nhất "
        f"{result['coldStartK']} lượt tương tác và có sản phẩm mới trong phần kiểm thử; {result['products']} sản phẩm "
        f"đang bán. Khoảng tin cậy 95%: bootstrap {result['bootstrapSamples']} lần theo người dùng.",
        "",
        "## Kiểm thử (đúng = mọi tương tác)",
        "",
        *_table_lines(result["rows"], n, True),
        *_comparison_lines(result["comparisons"], n),
        *_comparison_lines(result.get("mmrComparisons") or [], n),
        "",
        f"## Kiểm thử (đúng = chỉ thêm giỏ / mua, {result['strongUsers']} người dùng)",
        "",
        *_table_lines(result["strongRows"], n, False),
        *_comparison_lines(result["strongComparisons"], n),
        "",
        f"## Khách mới (1–{result['coldStartK'] - 1} lượt tương tác, {result['coldUsers']} người dùng)",
        "",
        *_table_lines(result["coldStartRows"], n, False),
        *_comparison_lines(result["coldStartComparisons"], n),
    ]
    if result.get("coldStartCurve"):
        lines += ["", f"## Cold-start: NDCG@{n} theo số lượt của khách mới (kiểm thử)", "",
                  f"Cắt lịch sử của {_pct(COLD_START_SHARE)} khách còn m lượt đầu rồi học lại; Hybrid được dùng ALS ngay "
                  f"từ lượt đầu để so với chỉ CBF.", "", *_cold_lines(result["coldStartCurve"])]
    if result.get("likeRows"):
        lines += ["", f"## Danh sách yêu thích (LIKE): có so với bỏ ({result['likeEvents']} lượt LIKE trong phần học)", "",
                  *_table_lines(result["likeRows"], n, False), *_comparison_lines(result["likeComparisons"], n)]
    cross = result["crossSell"]
    lines += ["", f"## Mua kèm ({cross['testOrders']} đơn kiểm thử, {cross['pairs']} cặp đơn – sản phẩm)", "",
              f"Luật cùng đơn học trên {cross['trainOrders']} đơn đặt trước; luật \"khách mua A cũng mua B\" học từ "
              f"hành vi trước ngày cắt ({cross.get('itemsWithCoBuyers', 0)} sản phẩm có luật); "
              f"{_pct(cross['withSuggestions'])} sản phẩm trong đơn kiểm thử có danh sách mua kèm.", "",
              f"| Cách gợi ý | HitRate@{cross['n']} | Precision@{cross['n']} | Có gợi ý |", "|---|---|---|---|"]
    lines += [f"| {row['model']} | {row['hitRate']:.4f} | {row['precision']:.4f} | "
              f"{_pct(row.get('withSuggestions', 0))} |" for row in cross["rows"]]
    lines += ["", *(f"- HitRate@{cross['n']}: {c['best']} − {c['other']}: {c['diff']:+.4f} "
                    f"(KTC 95%: {c['low']:+.4f} đến {c['high']:+.4f}), "
                    f"{'có ý nghĩa' if c['significant'] else 'chưa đủ chắc chắn'}"
                    for c in cross.get("comparisons", []))]
    upsell = result["upsell"]
    lines += ["", "## Nâng cấp", "",
              f"{_pct(upsell['withSuggestions'])} trong {upsell['products']} sản phẩm đang bán có gợi ý nâng cấp; gợi ý "
              f"đầu tiên đắt hơn trung bình {_pct(upsell['meanPriceIncrease'])}."]
    lines += ["", "## Tinh chỉnh trên validation", "", "Tham số ALS (chỉ ALS, NDCG@" + str(n) + "):", "",
              "| Số chiều | λ | a | NDCG |", "|---|---|---|---|"]
    lines += [f"| {r['factors']} | {r['regularization']} | {r['confidence']} | {r['ndcg']:.4f} |"
              for r in sorted(result["tuning"]["als"], key=lambda r: -r["ndcg"])]
    lines += ["", f"| Mô hình | Precision@{n} | Recall@{n} | NDCG@{n} | HitRate@{n} | Coverage |",
              "|---|---|---|---|---|---|"]
    lines += [f"| {r['model']} | {r['precision']:.4f} | {r['recall']:.4f} | {r['ndcg']:.4f} | {r['hitRate']:.4f} | "
              f"{r['coverage']:.4f} |" for r in result["validationRows"]]
    lines += ["", "Nửa đời giảm trọng số hành vi cũ (α đã chọn):", "", "| Nửa đời | NDCG |", "|---|---|"]
    lines += [f"| {'không giảm' if not r['halfLifeDays'] else str(int(r['halfLifeDays'])) + ' ngày'} | {r['ndcg']:.4f} |"
              for r in result["tuning"]["decay"]]
    if result["tuning"].get("mmr"):
        keep = round((1 - result["tuning"]["mmrMaxLoss"]) * 100)
        lines += ["", f"MMR (α và nửa đời đã chọn; chọn λ đa dạng nhất mà NDCG ≥ {keep}% của λ = 1):", "",
                  f"| λ | NDCG@{n} | Diversity |", "|---|---|---|"]
        lines += [f"| {r['lambda']:.1f} | {r['ndcg']:.4f} | {r['diversity']:.4f} |" for r in result["tuning"]["mmr"]]
    if result["tuning"].get("coldStart"):
        lines += ["", f"Ngưỡng cold-start (chọn k = m nhỏ nhất mà từ đó Hybrid ≥ CBF; được k = "
                      f"{chosen.get('coldStartK')}):", "", *_cold_lines(result["tuning"]["coldStart"])]
    configured = result["configured"]
    lines += ["", f"Hệ thống đang dùng: α = {configured['alpha']}, ALS {configured['factors']} chiều, "
                  f"λ = {configured['regularization']}, a = {configured['confidence']}, nửa đời "
                  f"{configured['halfLifeDays'] or 'không giảm'}, MMR {_mmr_text(configured.get('mmrLambda'))}, "
                  f"k = {configured.get('coldStartK')}."]
    return "\n".join(lines) + "\n"


def _cold_lines(rows: list[dict]) -> list[str]:
    return (["| Số lượt m | Người dùng | Phổ biến | Chỉ CBF | Hybrid |", "|---|---|---|---|---|"]
            + [f"| {r['events']} | {r['users']} | {r['popular']:.4f} | {r['cbf']:.4f} | {r['hybrid']:.4f} |" for r in rows])


def _mmr_text(lam: float | None) -> str:
    return "không xếp lại" if lam is None or lam >= 1 else f"λ = {lam}"


def alpha_chart_svg(result: dict, width: int = 640, height: int = 360) -> str:
    """NDCG@N và Recall@N theo α trên validation (α = 0: chỉ ALS, α = 1: chỉ CBF); đường ngang: danh sách phổ biến."""
    rows = result.get("validationRows") or result["rows"]
    points = sorted((row["alpha"], row) for row in rows if row["alpha"] is not None)
    if not points:
        return '<svg xmlns="http://www.w3.org/2000/svg" width="1" height="1"/>'
    n = result["n"]
    left, right, top, bottom = 56, 170, 24, 48
    plot_w, plot_h = width - left - right, height - top - bottom
    popular = _find(rows, "POPULAR")
    peak = max(max(row["ndcg"], row["recall"]) for _, row in points)
    y_max = max(0.05, math.ceil(peak * 20) / 20)

    def x(alpha):
        return left + alpha * plot_w

    def y(value):
        return top + plot_h - value / y_max * plot_h

    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" width="{width}" '
             f'height="{height}" font-family="Arial, sans-serif" font-size="12">',
             f'<rect width="{width}" height="{height}" fill="#ffffff"/>']
    steps = 5
    for i in range(steps + 1):
        value = y_max * i / steps
        parts.append(f'<line x1="{left}" y1="{y(value):.1f}" x2="{left + plot_w}" y2="{y(value):.1f}" '
                     'stroke="#e5e7eb"/>')
        parts.append(f'<text x="{left - 8}" y="{y(value) + 4:.1f}" text-anchor="end" fill="#4b5563">{value:.2f}</text>')
    for alpha, _ in points:
        parts.append(f'<text x="{x(alpha):.1f}" y="{top + plot_h + 18}" text-anchor="middle" fill="#4b5563">'
                     f'{alpha:.1f}</text>')
    parts.append(f'<text x="{left + plot_w / 2:.1f}" y="{height - 8}" text-anchor="middle" fill="#111827">'
                 'α trên validation (0 = chỉ ALS, 1 = chỉ CBF)</text>')
    series = [("ndcg", f"NDCG@{n}", "#2563eb"), ("recall", f"Recall@{n}", "#d97706")]
    for key, label, color in series:
        path = " ".join(f"{'M' if i == 0 else 'L'}{x(a):.1f},{y(row[key]):.1f}" for i, (a, row) in enumerate(points))
        parts.append(f'<path d="{path}" fill="none" stroke="{color}" stroke-width="2.5"/>')
        parts += [f'<circle cx="{x(a):.1f}" cy="{y(row[key]):.1f}" r="3.5" fill="{color}"/>' for a, row in points]
    if popular:
        parts.append(f'<line x1="{left}" y1="{y(popular["ndcg"]):.1f}" x2="{left + plot_w}" '
                     f'y2="{y(popular["ndcg"]):.1f}" stroke="#6b7280" stroke-dasharray="5 4"/>')
    if result.get("bestAlpha") is not None:
        bx = x(result["bestAlpha"])
        parts.append(f'<line x1="{bx:.1f}" y1="{top}" x2="{bx:.1f}" y2="{top + plot_h}" stroke="#16a34a" '
                     'stroke-dasharray="3 3"/>')
    legend = series + [(None, "Phổ biến (NDCG)", "#6b7280"), (None, "α chọn", "#16a34a")]
    for i, (key, label, color) in enumerate(legend):
        ly = top + 10 + i * 20
        dash = '' if key else ' stroke-dasharray="5 4"'
        parts.append(f'<line x1="{left + plot_w + 14}" y1="{ly}" x2="{left + plot_w + 36}" y2="{ly}" '
                     f'stroke="{color}" stroke-width="2.5"{dash}/>')
        parts.append(f'<text x="{left + plot_w + 42}" y="{ly + 4}" fill="#111827">{label}</text>')
    parts.append("</svg>")
    return "\n".join(parts) + "\n"


# ---------- lịch sử trong database ----------

def save_history(result: dict) -> bool:
    """Ghi một lần đánh giá + chỉ số từng mô hình; lần chạy cùng thời điểm đã có thì bỏ qua (trả False)."""
    chosen, cross, upsell = result["chosen"], result["crossSell"], result["upsell"]
    cross_rows = {row["model"]: row for row in cross["rows"]}
    with connect(readonly=False) as conn:
        created = conn.execute("""
            insert into recommendation_evaluations (evaluated_at, top_n, train_ratio, validation_ratio, cold_start_k,
                                                    users, validation_users, strong_users, cold_users, train_events,
                                                    validation_events, test_events, products, configured_alpha,
                                                    best_alpha, als_factors, als_regularization, als_confidence,
                                                    half_life_days, mmr_lambda, cross_sell_hit_rate,
                                                    cross_sell_baseline_hit_rate, upsell_share, bootstrap_samples,
                                                    seconds)
            values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s,
                    %s)
            on conflict (evaluated_at) do nothing
            returning evaluation_id
        """, (result["evaluatedAt"], result["n"], result["trainRatio"], result["validationRatio"],
              result["coldStartK"], result["users"], result["validationUsers"], result["strongUsers"],
              result["coldUsers"], result["trainEvents"], result["validationEvents"], result["testEvents"],
              result["products"], result["configuredAlpha"], result["bestAlpha"], chosen["factors"],
              chosen["regularization"], chosen["confidence"], chosen["halfLifeDays"], chosen.get("mmrLambda", 1.0),
              cross_rows.get(CROSS_SELL_MAIN, cross["rows"][0])["hitRate"],
              cross_rows[CROSS_SELL_BASELINE]["hitRate"], upsell["withSuggestions"],
              result["bootstrapSamples"], result["seconds"])).fetchone()
        if created is None:
            return False
        parts = ([("TEST", row) for row in result["rows"]] + [("TEST_STRONG", row) for row in result["strongRows"]]
                 + [("COLD_START", row) for row in result["coldStartRows"]]
                 + [("VALIDATION", row) for row in result["validationRows"]])
        conn.cursor().executemany("""
            insert into recommendation_evaluation_rows (evaluation_id, position, split, model, kind, alpha,
                                                        mmr_lambda, precision_at_n, recall_at_n, ndcg_at_n, ndcg_low,
                                                        ndcg_high, hit_rate_at_n, coverage, diversity)
            values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """, [(created["evaluation_id"], position, part, row["model"], row["kind"], row["alpha"],
               row.get("mmrLambda"), row["precision"],
               row["recall"], row["ndcg"], *((row.get("ci") or {}).get("ndcg") or (None, None)), row["hitRate"],
               row.get("coverage"), row.get("diversity"))
              for position, (part, row) in enumerate(parts)])
    return True


def load_history(limit: int = 20) -> list[dict]:
    """Các lần đánh giá gần nhất, NDCG trên phần kiểm thử: Hybrid (α chọn, α đang dùng), CBF, ALS, kNN, phổ biến."""
    try:
        rows = fetch_all("""
            select e.evaluated_at, e.top_n, e.users, e.train_events, e.validation_events, e.test_events,
                   e.configured_alpha, e.best_alpha, e.als_factors, e.als_regularization, e.als_confidence,
                   e.half_life_days, e.mmr_lambda, e.cross_sell_hit_rate, e.upsell_share,
                   max(r.ndcg_at_n) filter (where r.alpha = e.best_alpha and r.mmr_lambda is null) as best_ndcg,
                   max(r.ndcg_low) filter (where r.alpha = e.best_alpha and r.mmr_lambda is null) as best_ndcg_low,
                   max(r.ndcg_high) filter (where r.alpha = e.best_alpha and r.mmr_lambda is null) as best_ndcg_high,
                   max(r.diversity) filter (where r.alpha = e.best_alpha and r.mmr_lambda is null) as best_diversity,
                   max(r.ndcg_at_n) filter (where r.alpha = e.configured_alpha and r.mmr_lambda is null)
                       as configured_ndcg,
                   max(r.ndcg_at_n) filter (where r.alpha = e.best_alpha and r.mmr_lambda = e.mmr_lambda) as mmr_ndcg,
                   max(r.diversity) filter (where r.alpha = e.best_alpha and r.mmr_lambda = e.mmr_lambda)
                       as mmr_diversity,
                   max(r.ndcg_at_n) filter (where r.kind = 'CBF') as cbf_ndcg,
                   max(r.ndcg_at_n) filter (where r.kind = 'CF') as cf_ndcg,
                   max(r.ndcg_at_n) filter (where r.kind = 'KNN') as knn_ndcg,
                   max(r.ndcg_at_n) filter (where r.kind = 'POPULAR') as popular_ndcg
            from recommendation_evaluations e
            join recommendation_evaluation_rows r on r.evaluation_id = e.evaluation_id and r.split = 'TEST'
            group by e.evaluation_id
            order by e.evaluated_at desc
            limit %s
        """, (limit,))
    except psycopg.Error as exc:
        log.warning("Evaluation history unavailable (run database/migrations/recommendation_evaluations.sql): %s", exc)
        return []
    keys = {"evaluated_at": "evaluatedAt", "top_n": "n", "train_events": "trainEvents",
            "validation_events": "validationEvents", "test_events": "testEvents",
            "configured_alpha": "configuredAlpha", "best_alpha": "bestAlpha", "als_factors": "alsFactors",
            "als_regularization": "alsRegularization", "als_confidence": "alsConfidence",
            "half_life_days": "halfLifeDays", "mmr_lambda": "mmrLambda", "best_diversity": "bestDiversity",
            "mmr_ndcg": "mmrNdcg", "mmr_diversity": "mmrDiversity", "cross_sell_hit_rate": "crossSellHitRate", "upsell_share": "upsellShare",
            "best_ndcg": "bestNdcg", "best_ndcg_low": "bestNdcgLow", "best_ndcg_high": "bestNdcgHigh",
            "configured_ndcg": "configuredNdcg", "cbf_ndcg": "cbfNdcg", "cf_ndcg": "cfNdcg", "knn_ndcg": "knnNdcg",
            "popular_ndcg": "popularNdcg"}
    return [{keys.get(key, key): _plain(value) for key, value in row.items()} for row in rows]


def _plain(value):
    if isinstance(value, datetime):
        return value.isoformat(timespec="seconds")
    return float(value) if hasattr(value, "is_finite") else value  # Decimal → số


# ---------- chạy nền trong AI Service (trang admin bấm "Đánh giá lại") ----------

class EvaluationJob:
    """Một lần đánh giá chạy nền (vài phút); trạng thái + kết quả gần nhất để trang admin đọc."""

    def __init__(self):
        self._lock = threading.Lock()
        self._running = False
        self._started_at: str | None = None
        self._error: str | None = None

    def status(self) -> dict:
        with self._lock:
            state = {"running": self._running, "startedAt": self._started_at, "error": self._error}
        return {**state, "result": load_latest(), "history": load_history()}

    def start(self) -> dict:
        with self._lock:
            if not self._running:
                self._running = True
                self._started_at = datetime.now().isoformat(timespec="seconds")
                self._error = None
                threading.Thread(target=self._run, daemon=True).start()
        return self.status()

    def _run(self) -> None:
        error = None
        try:
            result = evaluate()
            save(result)
            save_history(result)
        except Exception as exc:  # trang admin hiện lỗi, mô hình đang phục vụ không bị ảnh hưởng
            log.exception("Offline evaluation failed")
            error = str(exc) or exc.__class__.__name__
        with self._lock:
            self._running = False
            self._error = error


evaluation_job = EvaluationJob()
