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
from app.recommender.features import NUMERIC_FEATURES, build_item_features, extract_numeric_specs, interaction_scores

log = logging.getLogger(__name__)

MODEL_FILE = "recommender.pkl"


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
    co_purchase: dict[int, list[tuple[int, float, float]]]  # i → [(j, confidence, lift)]
    relations: pd.DataFrame
    trained_at: datetime
    stats: dict = field(default_factory=dict)

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


def train_model() -> Model:
    started = datetime.now()
    products = data.load_products()
    specs = data.load_specs()
    items, vectors = build_item_features(products, specs)
    raw = extract_numeric_specs(products, specs)
    for feature in NUMERIC_FEATURES:
        items[feature + "_raw"] = raw[feature]
    index = {int(pid): row for row, pid in enumerate(items.index)}

    scores = interaction_scores(data.load_interactions())
    scores = scores[scores["product_id"].isin(index.keys())]
    users = {int(u): row for row, u in enumerate(sorted(scores["user_id"].unique()))}
    matrix = csr_matrix(
        (scores["r"].to_numpy(float),
         (scores["user_id"].map(users).to_numpy(int), scores["product_id"].map(index).to_numpy(int))),
        shape=(len(users), len(index)))
    if matrix.nnz:
        _, item_factors = als.train(matrix, config.ALS_FACTORS, config.ALS_REGULARIZATION,
                                    config.ALS_CONFIDENCE, config.ALS_ITERATIONS)
    else:
        item_factors = np.zeros((len(index), config.ALS_FACTORS))

    model = Model(
        items=items,
        vectors=_unit_rows(vectors),
        index=index,
        item_factors=item_factors,
        popularity=np.asarray(matrix.sum(axis=0)).ravel(),
        co_purchase=_co_purchase(data.load_order_baskets()),
        relations=data.load_relations(),
        trained_at=datetime.now(),
    )
    model.stats = {
        "products": len(index), "users": len(users), "interactions": int(matrix.nnz),
        "features": int(vectors.shape[1]), "co_purchase_items": len(model.co_purchase),
        "relations": len(model.relations), "seconds": round((datetime.now() - started).total_seconds(), 1),
    }
    log.info("Recommender trained: %s", model.stats)
    return model


class Recommender:
    """Giữ mô hình đã huấn luyện trong bộ nhớ; train() thay mô hình mới một cách an toàn giữa các request."""

    def __init__(self):
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
        interactions = data.load_interactions(user_id)
        interactions = interactions[interactions["product_id"].isin(m.index.keys())]
        if interactions.empty:
            return None
        scores = interaction_scores(interactions)
        rows = scores["product_id"].map(m.index).to_numpy(int)
        return rows, scores["r"].to_numpy(float), int(interactions["n"].sum())

    def _cbf_cf(self, state):
        """Score_CBF = cos(p_u, v_i); Score_CF = x_uᵀ y_i (None khi chưa đủ k tương tác hoặc ALS chưa có dữ liệu)."""
        m = self.model
        rows, r, count = state
        profile = (r[:, None] * m.vectors[rows]).sum(axis=0) / r.sum()
        cbf = m.vectors @ (profile / (np.linalg.norm(profile) or 1))
        cf = None
        if count >= config.COLD_START_K and np.any(m.item_factors[rows]):
            x_u = als.fold_in(m.item_factors, rows, r, config.ALS_CONFIDENCE, config.ALS_REGULARIZATION)
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

    # ---------- khuyến nghị cá nhân hoá ----------

    def recommend(self, user_id: int | None, n: int = 8, exclude: list[int] | None = None,
                  alpha: float | None = None) -> list[Recommendation]:
        m = self.model
        alpha = config.ALPHA if alpha is None else alpha
        allowed = m.valid.copy()
        for pid in exclude or []:
            if pid in m.index:
                allowed[m.index[pid]] = False

        state = self._user_state(user_id)
        if state is None:
            return self.popular(n, allowed)

        rows, r, count = state
        # bỏ sản phẩm đã mua (3.5.5 a) và cả sản phẩm đã xem: danh sách "dành cho bạn" chỉ gồm sản phẩm mới với khách
        allowed[rows] = False
        cbf, cf = self._cbf_cf(state)
        if cf is None:
            alpha = 1.0                                     # Bảng 3.21: ít dữ liệu → chỉ CBF

        # 3.5.5 a: K ứng viên tốt nhất từ mỗi mô hình (đã bỏ sản phẩm không hợp lệ / đã mua)
        def top_k(scores):
            masked = np.where(allowed, scores, -np.inf)
            k = min(config.CANDIDATES_K, int(allowed.sum()))
            return np.argpartition(-masked, k - 1)[:k] if k else np.array([], dtype=int)

        candidates = np.union1d(top_k(cbf), top_k(cf) if cf is not None else [])
        candidates = candidates[allowed[candidates]].astype(int)
        if candidates.size == 0:
            return self.popular(n, allowed)

        score, s_cbf, s_cf = self._hybrid(cbf, cf, candidates, alpha)
        order = self._rank(candidates, score)[:n]
        result = []
        for pos in order:
            row = candidates[pos]
            from_cf = s_cf is not None and (1 - alpha) * s_cf[pos] > alpha * s_cbf[pos]
            result.append(Recommendation(
                m.items.index[row], score[pos], self._reason_personal(row, rows, r, from_cf),
                s_cbf[pos], None if s_cf is None else s_cf[pos]))
        log.debug("user %s: %d interactions, alpha=%.2f", user_id, count, alpha)
        return result

    def popular(self, n: int, allowed: np.ndarray | None = None) -> list[Recommendation]:
        """Bảng 3.21: người dùng mới / chưa đăng nhập → sản phẩm phổ biến (Σ r cao nhất) + sản phẩm mới."""
        m = self.model
        allowed = m.valid if allowed is None else allowed
        reviews = m.items["tgdd_review_count"].to_numpy(float)
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
            relevance[int(row.related_product_id)] = (float(row.strength), "Phụ kiện phù hợp với sản phẩm này")
        for j, confidence, lift in m.co_purchase.get(product_id, []):
            score = min(1.0, 0.5 + 0.5 * confidence)
            if score > relevance.get(j, (0, ""))[0]:
                relevance[j] = (score, "Thường được mua cùng sản phẩm này")

        pairs = [(m.index[j], s, why) for j, (s, why) in relevance.items() if j in m.index and m.valid[m.index[j]]]
        if not pairs:
            return []
        candidates = np.array([p[0] for p in pairs])
        related = np.array([p[1] for p in pairs])
        personal = self._personal_or_similar(user_id, None, candidates)
        score = related if personal is None else 0.6 * related + 0.4 * personal
        order = self._rank(candidates, score)[:n]
        return [Recommendation(m.items.index[candidates[p]], score[p], pairs[p][2]) for p in order]

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
