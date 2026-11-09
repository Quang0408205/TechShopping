"""
Recommendation Engine (báo cáo mục 3.5.1–3.5.6): CBF + CF (ALS) → Hybrid → lọc → xếp hạng → Top-N,
cùng Upselling / Cross-selling. Huấn luyện offline (train) → lưu models/; khi có request chỉ tính điểm.
"""

import logging
import pickle
import threading
from collections import Counter
from dataclasses import dataclass, field
from datetime import datetime
from itertools import combinations

import numpy as np
import pandas as pd
from scipy.sparse import csr_matrix

from app import config
from app.recommender import als, data
from app.recommender.compat import compatible
from app.recommender.features import NUMERIC_FEATURES, build_item_features, extract_numeric_specs, interaction_scores

log = logging.getLogger(__name__)

MODEL_FILE = "recommender.pkl"
ACCESSORY_REASON = "Phụ kiện phù hợp với sản phẩm này"


@dataclass
class Recommendation:
    product_id: int
    score: float
    reason: str
    cbf_score: float | None = None
    cf_score: float | None = None

    def to_dict(self) -> dict:
        return {
            "productId": int(self.product_id),
            "score": round(float(self.score), 3),
            "reason": self.reason,
            "cbfScore": None if self.cbf_score is None else round(float(self.cbf_score), 3),
            "cfScore": None if self.cf_score is None else round(float(self.cf_score), 3),
        }


@dataclass
class Model:
    items: pd.DataFrame                 # thuộc tính sản phẩm đã làm giàu, index = product_id
    vectors: np.ndarray                 # v_i đã chuẩn hoá độ dài (để cosine = tích vô hướng)
    index: dict[int, int]               # product_id → dòng trong vectors / item_factors
    item_factors: np.ndarray            # Y của ALS
    popularity: np.ndarray              # Σ_u r(u,i)
    co_purchase: dict[int, list[tuple[int, float, float]]]  # i → [(j, confidence, lift)]: cùng một đơn
    relations: pd.DataFrame
    trained_at: datetime
    stats: dict = field(default_factory=dict)
    params: dict = field(default_factory=dict)  # tham số ALS + half_life_days đã dùng khi học (fold-in dùng lại)
    co_buyers: dict = field(default_factory=dict)  # i → [(j, confidence, lift)]: khách mua i cũng mua j (khác danh mục)

    def param(self, key: str):
        """Tham số lúc học; mô hình lưu trước khi có `params` → giá trị trong config."""
        defaults = {"factors": config.ALS_FACTORS, "regularization": config.ALS_REGULARIZATION,
                    "confidence": config.ALS_CONFIDENCE, "iterations": config.ALS_ITERATIONS,
                    "half_life_days": config.HALF_LIFE_DAYS}
        return getattr(self, "params", {}).get(key, defaults[key])

    @property
    def valid(self) -> np.ndarray:
        """3.5.5 c: chỉ sản phẩm đang bán và có giá (giống CartMapper.isPurchasable ở backend)."""
        return (self.items["is_active"].to_numpy(bool)) & (self.items["price"].to_numpy(float) > 0)


def _unit_rows(matrix: np.ndarray) -> np.ndarray:
    norms = np.linalg.norm(matrix, axis=1, keepdims=True)
    return matrix / np.where(norms == 0, 1, norms)


def _min_max(values: np.ndarray) -> np.ndarray:
    low, high = values.min(), values.max()
    return np.zeros_like(values) if high == low else (values - low) / (high - low)


def _co_purchase(baskets: pd.DataFrame, min_pairs: int = 2, min_confidence: float = 0.05):
    """3.5.6 b: confidence(i→j) = count(i,j)/count(i); lift = confidence / (count(j)/|O|); giữ lift > 1."""
    if baskets.empty:
        return {}
    orders = baskets.groupby("order_id")["product_id"].apply(lambda s: sorted(set(s)))
    total = len(orders)
    single, pair = Counter(), Counter()
    for products in orders:
        single.update(products)
        pair.update(combinations(products, 2))
    result: dict[int, list] = {}
    for (i, j), together in pair.items():
        if together < min_pairs:
            continue
        for a, b in ((i, j), (j, i)):
            confidence = together / single[a]
            lift = confidence / (single[b] / total)
            if lift > 1 and confidence >= min_confidence:
                result.setdefault(a, []).append((b, confidence, lift))
    return result


BUYER_TYPES = ("ADD_TO_CART", "PURCHASE")


def _co_buyers(interactions: pd.DataFrame, items: pd.DataFrame, index: dict[int, int], min_buyers: int = 2,
               top: int = 20) -> dict[int, list[tuple[int, float, float]]]:
    """
    3.5.6 b: "khách mua A cũng mua B" (item-to-item, Linden và cộng sự 2003). Đếm theo từng khách trên toàn bộ lịch sử
    thêm giỏ / mua, không cần cùng một đơn: confidence(i→j) = số khách có cả i và j / số khách có i;
    lift = confidence / (số khách có j / số khách). Chỉ giữ j khác danh mục với i (đồ đi kèm, không phải sản phẩm
    thay thế), ít nhất `min_buyers` khách chung và lift > 1; mỗi i giữ `top` cặp confidence cao nhất.
    """
    bought = interactions[interactions["interaction_type"].isin(BUYER_TYPES)]
    bought = bought[bought["product_id"].isin(index.keys())][["user_id", "product_id"]].drop_duplicates()
    if bought.empty:
        return {}
    users = {u: row for row, u in enumerate(bought["user_id"].unique())}
    matrix = csr_matrix((np.ones(len(bought)), (bought["user_id"].map(users).to_numpy(int),
                                                 bought["product_id"].map(index).to_numpy(int))),
                        shape=(len(users), len(index)))
    together = (matrix.T @ matrix).tocsr()
    together.setdiag(0)
    together.eliminate_zeros()
    buyers = np.asarray(matrix.sum(axis=0)).ravel()
    category = items["category_id"].to_numpy()
    ids = items.index.to_numpy()
    result: dict[int, list] = {}
    for i in np.flatnonzero(buyers):
        start, end = together.indptr[i], together.indptr[i + 1]
        partners, counts = together.indices[start:end], together.data[start:end]
        keep = (counts >= min_buyers) & (category[partners] != category[i])
        if not keep.any():
            continue
        partners, counts = partners[keep], counts[keep]
        confidence = counts / buyers[i]
        lift = confidence / (buyers[partners] / len(users))
        keep = lift > 1
        order = np.argsort(-confidence[keep], kind="stable")[:top]
        result[int(ids[i])] = [(int(ids[j]), float(c), float(l))
                               for j, c, l in zip(partners[keep][order], confidence[keep][order], lift[keep][order])]
    return result


def train_model(load_interactions=data.load_interactions, load_baskets=data.load_order_baskets,
                params: dict | None = None, now=None) -> Model:
    """
    load_interactions / load_baskets: nguồn hành vi và đơn hàng (mặc định cả database; đánh giá offline truyền phần
    huấn luyện). params: factors, regularization, confidence, iterations, half_life_days (thiếu → config).
    now: mốc tính tuổi hành vi khi giảm dần trọng số (mặc định lúc học).
    """
    started = datetime.now()
    params = {"factors": config.ALS_FACTORS, "regularization": config.ALS_REGULARIZATION,
              "confidence": config.ALS_CONFIDENCE, "iterations": config.ALS_ITERATIONS,
              "half_life_days": config.HALF_LIFE_DAYS, **(params or {})}
    products = data.load_products()
    specs = data.load_specs()
    items, vectors = build_item_features(products, specs)
    raw = extract_numeric_specs(products, specs)
    for feature in NUMERIC_FEATURES:
        items[feature + "_raw"] = raw[feature]
    index = {int(pid): row for row, pid in enumerate(items.index)}

    interactions = load_interactions()
    scores = interaction_scores(interactions, params["half_life_days"], now)
    scores = scores[scores["product_id"].isin(index.keys())]
    users = {int(u): row for row, u in enumerate(sorted(scores["user_id"].unique()))}
    matrix = csr_matrix(
        (scores["r"].to_numpy(float),
         (scores["user_id"].map(users).to_numpy(int), scores["product_id"].map(index).to_numpy(int))),
        shape=(len(users), len(index)))
    if matrix.nnz:
        _, item_factors = als.train(matrix, params["factors"], params["regularization"], params["confidence"],
                                    params["iterations"])
    else:
        item_factors = np.zeros((len(index), params["factors"]))

    model = Model(
        items=items,
        vectors=_unit_rows(vectors),
        index=index,
        item_factors=item_factors,
        popularity=np.asarray(matrix.sum(axis=0)).ravel(),
        co_purchase=_co_purchase(load_baskets()),
        co_buyers=_co_buyers(interactions, items, index),
        relations=data.load_relations(),
        trained_at=datetime.now(),
        params=params,
    )
    model.stats = {
        "products": len(index), "users": len(users), "interactions": int(matrix.nnz),
        "features": int(vectors.shape[1]), "co_purchase_items": len(model.co_purchase),
        "co_buyer_items": len(model.co_buyers),
        "relations": len(model.relations), "seconds": round((datetime.now() - started).total_seconds(), 1),
    }
    log.info("Recommender trained: %s", model.stats)
    return model


class Recommender:
    """Giữ mô hình đã huấn luyện trong bộ nhớ; train() thay mô hình mới một cách an toàn giữa các request."""

    def __init__(self, load_interactions=data.load_interactions, now=None):
        """now: mốc tính tuổi hành vi của khách (đánh giá offline: cuối phần học; mặc định: lúc gợi ý)."""
        self._load_interactions = load_interactions
        self._now = now
        self._model: Model | None = None
        self._lock = threading.Lock()

    # ---------- huấn luyện / nạp ----------

    def load_or_train(self) -> None:
        path = config.MODEL_DIR / MODEL_FILE
        if path.exists():
            with path.open("rb") as f:
                self._model = pickle.load(f)
            log.info("Recommender loaded from %s (trained %s)", path, self._model.trained_at)
        else:
            self.train()

    def train(self) -> dict:
        model = train_model()
        config.MODEL_DIR.mkdir(parents=True, exist_ok=True)
        with (config.MODEL_DIR / MODEL_FILE).open("wb") as f:
            pickle.dump(model, f)
        with self._lock:
            self._model = model
        return {"trainedAt": model.trained_at.isoformat(timespec="seconds"), **model.stats}

    @property
    def model(self) -> Model:
        if self._model is None:
            raise RuntimeError("Recommender is not trained yet")
        return self._model

    # ---------- điểm của một người dùng ----------

    def _user_state(self, user_id: int | None):
        """(dòng sản phẩm đã tương tác, r(u,i) tương ứng, tổng số lượt tương tác); None nếu chưa có."""
        if user_id is None:
            return None
        m = self.model
        interactions = self._load_interactions(user_id)
        interactions = interactions[interactions["product_id"].isin(m.index.keys())]
        if interactions.empty:
            return None
        scores = interaction_scores(interactions, m.param("half_life_days"), self._now)
        rows = scores["product_id"].map(m.index).to_numpy(int)
        return rows, scores["r"].to_numpy(float), int(interactions["n"].sum())

    def _cbf_cf(self, state, cold_start_k: int | None = None):
        """
        Score_CBF = cos(p_u, v_i); Score_CF = x_uᵀ y_i (None khi chưa đủ k tương tác hoặc ALS chưa có dữ liệu).
        cold_start_k: ngưỡng k (mặc định config.COLD_START_K); đánh giá offline truyền giá trị khác để thử nhiều ngưỡng
        mà không đổi cấu hình chung đang phục vụ khách.
        """
        m = self.model
        rows, r, count = state
        k = config.COLD_START_K if cold_start_k is None else cold_start_k
        profile = (r[:, None] * m.vectors[rows]).sum(axis=0) / r.sum()
        cbf = m.vectors @ (profile / (np.linalg.norm(profile) or 1))
        cf = None
        if count >= k and np.any(m.item_factors[rows]):
            x_u = als.fold_in(m.item_factors, rows, r, m.param("confidence"), m.param("regularization"))
            cf = m.item_factors @ x_u
        return cbf, cf

    def _hybrid(self, cbf, cf, candidates: np.ndarray, alpha: float):
        """3.5.5 b: chuẩn hoá Min-Max trên tập ứng viên rồi Score = α·S'_CBF + (1 − α)·S'_CF."""
        s_cbf = _min_max(cbf[candidates])
        if cf is None:
            return s_cbf, s_cbf, None
        s_cf = _min_max(cf[candidates])
        return alpha * s_cbf + (1 - alpha) * s_cf, s_cbf, s_cf

    def _rank(self, candidates: np.ndarray, score: np.ndarray) -> np.ndarray:
        """3.5.5 d: điểm giảm dần; bằng điểm thì ưu tiên phổ biến hơn, rồi cập nhật gần đây hơn."""
        m = self.model
        updated = m.items["updated_at"].to_numpy("datetime64[s]")[candidates].astype(float)
        return np.lexsort((-updated, -m.popularity[candidates], -np.round(score, 6)))

    def _mmr(self, candidates: np.ndarray, score: np.ndarray, n: int, lam: float) -> np.ndarray:
        """
        Maximal Marginal Relevance: chọn lần lượt j = argmax [λ·Score(j) − (1 − λ)·max_{k đã chọn} cos(v_j, v_k)].
        Score (Hybrid, trong [0, 1]) và cosine (vector thuộc tính không âm, trong [0, 1]) cùng thang. λ = 1 → giống
        _rank; bằng nhau thì giữ thứ tự của _rank. Trả về vị trí trong `candidates` như _rank.
        """
        order = self._rank(candidates, score)
        if lam >= 1 or len(order) <= 1:
            return order[:n]
        vectors = self.model.vectors[candidates[order]]
        relevance = score[order]
        closest = np.zeros(len(order))              # max cosine tới các sản phẩm đã chọn
        available = np.ones(len(order), bool)
        picked = []
        for _ in range(min(n, len(order))):
            best = int(np.argmax(np.where(available, lam * relevance - (1 - lam) * closest, -np.inf)))
            picked.append(best)
            available[best] = False
            closest = np.maximum(closest, vectors @ vectors[best])
        return order[picked]

    # ---------- khuyến nghị cá nhân hoá ----------

    def recommend(self, user_id: int | None, n: int = 8, exclude: list[int] | None = None,
                  alpha: float | None = None) -> list[Recommendation]:
        return self.recommend_alphas(user_id, n, [config.ALPHA if alpha is None else alpha], exclude)[0]

    def recommend_alphas(self, user_id: int | None, n: int, alphas: list[float],
                         exclude: list[int] | None = None, mmr_lambda: float | None = None,
                         cold_start_k: int | None = None) -> list[list[Recommendation]]:
        """
        Một danh sách Top-N cho mỗi α: hồ sơ khách, điểm CBF / CF và tập ứng viên không phụ thuộc α nên chỉ tính
        một lần (đánh giá offline thử nhiều α trên cùng khách). mmr_lambda: λ của MMR (mặc định config.MMR_LAMBDA).
        """
        mmr_lambda = config.MMR_LAMBDA if mmr_lambda is None else mmr_lambda
        m = self.model
        allowed = m.valid.copy()
        for pid in exclude or []:
            if pid in m.index:
                allowed[m.index[pid]] = False

        state = self._user_state(user_id)
        if state is None:
            popular = self.popular(n, allowed)
            return [popular for _ in alphas]

        rows, r, count = state
        # bỏ sản phẩm đã mua (3.5.5 a) và cả sản phẩm đã xem: danh sách "dành cho bạn" chỉ gồm sản phẩm mới với khách
        allowed[rows] = False
        cbf, cf = self._cbf_cf(state, cold_start_k)

        # 3.5.5 a: K ứng viên tốt nhất từ mỗi mô hình (đã bỏ sản phẩm không hợp lệ / đã mua)
        def top_k(scores):
            masked = np.where(allowed, scores, -np.inf)
            k = min(config.CANDIDATES_K, int(allowed.sum()))
            return np.argpartition(-masked, k - 1)[:k] if k else np.array([], dtype=int)

        # mảng rỗng phải là số nguyên: union1d với [] (số thực) làm chỉ số thành float → IndexError khi chỉ có CBF
        candidates = np.union1d(top_k(cbf), top_k(cf) if cf is not None else np.array([], dtype=int))
        candidates = candidates[allowed[candidates]].astype(int)
        if candidates.size == 0:
            popular = self.popular(n, allowed)
            return [popular for _ in alphas]

        lists = []
        for alpha in alphas:
            alpha = 1.0 if cf is None else alpha        # Bảng 3.21: ít dữ liệu → chỉ CBF
            score, s_cbf, s_cf = self._hybrid(cbf, cf, candidates, alpha)
            result = []
            for pos in self._mmr(candidates, score, n, mmr_lambda):
                row = candidates[pos]
                from_cf = s_cf is not None and (1 - alpha) * s_cf[pos] > alpha * s_cbf[pos]
                result.append(Recommendation(
                    m.items.index[row], score[pos], self._reason_personal(row, rows, r, from_cf),
                    s_cbf[pos], None if s_cf is None else s_cf[pos]))
            lists.append(result)
        log.debug("user %s: %d interactions, alphas=%s", user_id, count, alphas)
        return lists

    def popular(self, n: int, allowed: np.ndarray | None = None) -> list[Recommendation]:
        """Bảng 3.21: người dùng mới / chưa đăng nhập → sản phẩm phổ biến (Σ r cao nhất) + sản phẩm mới."""
        m = self.model
        allowed = m.valid if allowed is None else allowed
        # model files trained before 2026-10-17 still have the old TGDĐ column name
        column = "review_count" if "review_count" in m.items else "tgdd_review_count"
        reviews = m.items[column].to_numpy(float)
        rows = np.flatnonzero(allowed)
        by_popularity = rows[np.lexsort((-reviews[rows], -m.popularity[rows]))]
        created = m.items["created_at"].to_numpy("datetime64[s]").astype(float)
        by_new = rows[np.argsort(-created[rows], kind="stable")]

        n_new = max(1, n // 4)
        score = _min_max(np.log1p(m.popularity))
        picked, result = set(), []
        for row in by_popularity:
            if len(result) >= n - n_new:
                break
            picked.add(row)
            reason = "Được nhiều khách hàng quan tâm" if m.popularity[row] > 0 else "Sản phẩm nổi bật"
            result.append(Recommendation(m.items.index[row], score[row], reason))
        for row in by_new:
            if len(result) >= n:
                break
            if row not in picked:
                result.append(Recommendation(m.items.index[row], score[row], "Sản phẩm mới"))
        return result

    # ---------- sản phẩm tương tự (CBF giữa 2 sản phẩm) ----------

    def similar(self, product_id: int, n: int = 4) -> list[Recommendation]:
        m = self.model
        if product_id not in m.index:
            return []
        i = m.index[product_id]
        sims = m.vectors @ m.vectors[i]
        same_category = m.items["category_id"].to_numpy() == m.items["category_id"].iloc[i]
        candidates = np.flatnonzero(m.valid & same_category)
        candidates = candidates[candidates != i]
        order = self._rank(candidates, sims[candidates])[:n]
        return [Recommendation(m.items.index[candidates[p]], sims[candidates[p]],
                               self._reason_similar(candidates[p], i)) for p in order]

    # ---------- Upselling (3.5.6 a) ----------

    def upsell(self, product_id: int, user_id: int | None = None, n: int = 4) -> list[Recommendation]:
        m = self.model
        if product_id not in m.index:
            return []
        i = m.index[product_id]
        items = m.items
        price = items["price"].to_numpy(float)
        same_category = items["category_id"].to_numpy() == items["category_id"].iloc[i]
        in_range = (price > price[i]) & (price <= price[i] * (1 + config.UPSELL_DELTA))
        mask = m.valid & same_category & in_range
        mask[i] = False

        raw = items[[f + "_raw" for f in ("ram_gb", "storage_gb", "camera_mp")]].to_numpy(float)
        if not np.all(np.isnan(raw[i])):
            # có ít nhất một thông số chính tốt hơn; danh mục không có thông số (đồng hồ thời trang) chỉ xét giá
            better = np.nan_to_num(raw - raw[i], nan=0.0) > 0
            mask &= better.any(axis=1)

        candidates = np.flatnonzero(mask)
        if candidates.size == 0:
            return []
        score = self._personal_or_similar(user_id, i, candidates)
        order = self._rank(candidates, score)[:n]
        return [Recommendation(items.index[candidates[p]], score[p], self._reason_upsell(candidates[p], i))
                for p in order]

    # ---------- Cross-selling (3.5.6 b) ----------

    def cross_sell(self, product_id: int, user_id: int | None = None, n: int = 4) -> list[Recommendation]:
        m = self.model
        if product_id not in m.index:
            return []
        relevance: dict[int, tuple[float, str]] = {}
        rel = m.relations
        declared = rel[(rel["product_id"] == product_id) & rel["relation_type"].isin(data.CROSS_SELL_RELATIONS)]
        for row in declared.itertuples():
            relevance[int(row.related_product_id)] = (float(row.strength), ACCESSORY_REASON)
        for j, confidence, lift in m.co_purchase.get(product_id, []):
            score = min(1.0, 0.5 + 0.5 * confidence)
            if score > relevance.get(j, (0, ""))[0]:
                relevance[j] = (score, "Thường được mua cùng sản phẩm này")
        for j, confidence, lift in getattr(m, "co_buyers", {}).get(product_id, []):
            score = min(1.0, 0.4 + 0.5 * confidence)    # cùng khách, không cùng đơn: bằng chứng yếu hơn một chút
            if score > relevance.get(j, (0, ""))[0]:
                relevance[j] = (score, "Khách mua sản phẩm này cũng mua")

        pairs = [(m.index[j], s, why) for j, (s, why) in relevance.items() if j in m.index and m.valid[m.index[j]]
                 and (why == ACCESSORY_REASON or self._fits(m.index[product_id], m.index[j]))]
        if not pairs:
            return []
        candidates = np.array([p[0] for p in pairs])
        related = np.array([p[1] for p in pairs])
        personal = self._personal_or_similar(user_id, None, candidates)
        score = related if personal is None else 0.6 * related + 0.4 * personal
        order = self._rank(candidates, score)[:n]
        return [Recommendation(m.items.index[candidates[p]], score[p], pairs[p][2]) for p in order]

    def _fits(self, device_row: int, accessory_row: int) -> bool:
        """Gợi ý từ luật mua chung cũng phải vừa máy (luật Apple, đúng đời máy, cổng sạc...) như quan hệ khai báo;
        thiết bị gợi ý thiết bị khác chỉ khi cùng hãng (compat.compatible)."""
        items = self.model.items
        device, accessory = items.iloc[device_row], items.iloc[accessory_row]
        screen = device.get("screen_inch")
        return compatible(str(device.get("name", "")), str(device.get("category", "")), str(device.get("brand", "")),
                          str(accessory.get("name", "")), str(accessory.get("category", "")),
                          f"{screen} inch" if pd.notna(screen) else None, str(accessory.get("brand", "")))

    def _personal_or_similar(self, user_id, anchor_row, candidates):
        """Score_Hybrid(u, j) khi người dùng có dữ liệu, ngược lại độ tương đồng CBF với sản phẩm đang xem."""
        m = self.model
        state = self._user_state(user_id)
        if state is not None:
            cbf, cf = self._cbf_cf(state)
            return self._hybrid(cbf, cf, candidates, config.ALPHA if cf is not None else 1.0)[0]
        if anchor_row is None:
            return None
        return m.vectors[candidates] @ m.vectors[anchor_row]

    # ---------- lý do đề xuất (lưu vào recommendations.reason) ----------

    def _reason_personal(self, row, rows, r, from_cf) -> str:
        m = self.model
        if from_cf:
            # sản phẩm đã tương tác có vector ALS gần nhất: "khách quan tâm X cũng quan tâm sản phẩm này"
            factors = m.item_factors[rows]
            norms = np.linalg.norm(factors, axis=1) * (np.linalg.norm(m.item_factors[row]) or 1)
            if np.any(norms > 0):
                closeness = (factors @ m.item_factors[row]) / np.where(norms > 0, norms, 1)
                nearest = rows[np.lexsort((-r, -np.round(closeness, 6)))[0]]
                return "Khách quan tâm " + str(m.items["name"].iloc[nearest]) + " cũng quan tâm sản phẩm này"
            return "Khách hàng có sở thích giống bạn cũng quan tâm"
        # sản phẩm đã tương tác giống nhất (bằng nhau thì lấy cái được quan tâm nhiều hơn)
        nearest = rows[np.lexsort((-r, -np.round(m.vectors[rows] @ m.vectors[row], 6)))[0]]
        return "Tương tự " + str(m.items["name"].iloc[nearest]) + " bạn đã xem"

    def _reason_similar(self, row, anchor) -> str:
        items = self.model.items
        parts = []
        if items["brand"].iloc[row] == items["brand"].iloc[anchor]:
            parts.append("Cùng hãng " + str(items["brand"].iloc[row]))
        if items["price_segment"].iloc[row] == items["price_segment"].iloc[anchor]:
            parts.append("cùng tầm giá")
        if not parts:
            return "Cấu hình tương tự"
        text = ", ".join(parts)
        return text[0].upper() + text[1:]

    def _reason_upsell(self, row, anchor) -> str:
        items = self.model.items
        better = []
        for feature, label in (("ram_gb", "RAM"), ("storage_gb", "bộ nhớ"), ("camera_mp", "camera")):
            a, b = items[feature + "_raw"].iloc[anchor], items[feature + "_raw"].iloc[row]
            if pd.notna(a) and pd.notna(b) and b > a:
                better.append(f"{label} {b:g}{'MP' if feature == 'camera_mp' else 'GB'}")
        extra = f"{items['price'].iloc[row] - items['price'].iloc[anchor]:,.0f}".replace(",", ".")
        text = "Nâng cấp " + ", ".join(better) if better else "Phiên bản cao cấp hơn"
        return f"{text}, chỉ thêm {extra}đ"


recommender = Recommender()
