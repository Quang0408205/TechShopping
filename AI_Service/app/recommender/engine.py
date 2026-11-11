"""
Recommendation Engine: CBF + CF (ALS) → Hybrid → lọc → xếp hạng → Top-N,
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
from app.recommender.compat import ACCESSORY_CATEGORY, MODEL_SPECIFIC, accessory_kind, compatible, phone_model
from app.recommender.features import NUMERIC_FEATURES, build_item_features, extract_numeric_specs, interaction_scores

log = logging.getLogger(__name__)

MODEL_FILE = "recommender.pkl"
ACCESSORY_REASON = "Phụ kiện phù hợp với sản phẩm này"

# Căn cứ của danh sách "Dành cho bạn" (giao diện đổi tiêu đề theo căn cứ; POPULAR → hiện bảng hỏi)
BASIS_BEHAVIOR, BASIS_PREFERENCES, BASIS_POPULAR = "BEHAVIOR", "PREFERENCES", "POPULAR"
# Câu "Tầm giá" của bảng hỏi ↔ phân khúc giá (phân vị 1/3, 2/3 trong từng danh mục, features.build_item_features)
PRICE_LEVELS = {"LOW": "Thấp", "MID": "Trung", "HIGH": "Cao"}
PRICE_LABELS = {"Thấp": "phổ thông", "Trung": "tầm trung", "Cao": "cao cấp"}


@dataclass(frozen=True)
class Preferences:
    """Câu trả lời bảng hỏi của khách: danh mục quan tâm (bắt buộc), tầm giá, hãng, nhu cầu (có thể bỏ trống)."""
    category_ids: tuple[int, ...]
    brand_ids: tuple[int, ...] = ()
    price_levels: tuple[str, ...] = ()
    need_ids: tuple[int, ...] = ()


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
    needs: dict = field(default_factory=dict)          # need_id → (category_id, tên nhu cầu): bảng hỏi của khách mới
    product_needs: dict = field(default_factory=dict)  # dòng sản phẩm → frozenset(need_id) sản phẩm đáp ứng

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


def _needs(frame: pd.DataFrame, index: dict[int, int]) -> tuple[dict, dict]:
    """(need_id → (category_id, tên), dòng sản phẩm → frozenset(need_id)) từ bảng của data.load_needs."""
    needs = {int(row.need_id): (int(row.category_id), str(row.name))
             for row in frame.drop_duplicates("need_id").itertuples()}
    pairs = frame.dropna(subset=["product_id"])
    product_needs: dict[int, set] = {}
    for pid, need in zip(pairs["product_id"].astype(int), pairs["need_id"].astype(int)):
        if pid in index:
            product_needs.setdefault(index[pid], set()).add(need)
    return needs, {row: frozenset(values) for row, values in product_needs.items()}


def train_model(load_interactions=data.load_interactions, load_baskets=data.load_order_baskets,
                params: dict | None = None, now=None, load_needs=None) -> Model:
    """
    load_interactions / load_baskets: nguồn hành vi và đơn hàng (mặc định cả database; đánh giá offline truyền phần
    huấn luyện). params: factors, regularization, confidence, iterations, half_life_days (thiếu → config).
    now: mốc tính tuổi hành vi khi giảm dần trọng số (mặc định lúc học).
    load_needs: nhu cầu của sản phẩm cho bảng hỏi (mặc định data.load_needs).
    """
    started = datetime.now()
    params = {"factors": config.ALS_FACTORS, "regularization": config.ALS_REGULARIZATION,
              "confidence": config.ALS_CONFIDENCE, "iterations": config.ALS_ITERATIONS,
              "half_life_days": config.HALF_LIFE_DAYS, **(params or {})}
    products = data.load_products()
    if "brand_id" not in products:          # nguồn ngoài (scripts/eval_amazon.py) không có mã hãng
        products["brand_id"] = 0
    specs = data.load_specs()
    items, vectors = build_item_features(products, specs)
    raw = extract_numeric_specs(products, specs)
    for feature in NUMERIC_FEATURES:
        items[feature + "_raw"] = raw[feature]
    index = {int(pid): row for row, pid in enumerate(items.index)}
    needs, product_needs = _needs((load_needs or data.load_needs)(), index)

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
        needs=needs,
        product_needs=product_needs,
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
        self._model_columns = None      # (mô hình, đời máy của phụ kiện theo đời máy, đời máy của thiết bị)

    # ---------- huấn luyện / nạp ----------

    def load_or_train(self) -> None:
        path = config.MODEL_DIR / MODEL_FILE
        if path.exists():
            with path.open("rb") as f:
                self._model = pickle.load(f)
            log.info("Recommender loaded from %s (trained %s)", path, self._model.trained_at)
            if not hasattr(self._model, "product_needs") or "brand_id" not in self._model.items:
                log.info("Model file has no questionnaire data (brand ids, product needs): retraining")
                self.train()
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

    def _rank(self, candidates: np.ndarray, score: np.ndarray, prior: tuple | None = None) -> np.ndarray:
        """
        3.5.5 d: điểm giảm dần; bằng điểm thì ưu tiên phổ biến hơn, rồi cập nhật gần đây hơn.
        prior: các cột (theo dòng sản phẩm, cột đầu quan trọng nhất) thay cho độ phổ biến khi bằng điểm.
        """
        m = self.model
        updated = m.items["updated_at"].to_numpy("datetime64[s]")[candidates].astype(float)
        prior = (m.popularity,) if prior is None else prior
        return np.lexsort((-updated, *[-np.asarray(p, float)[candidates] for p in reversed(prior)],
                           -np.round(score, 6)))

    def _mmr(self, candidates: np.ndarray, score: np.ndarray, n: int, lam: float,
             prior: tuple | None = None) -> np.ndarray:
        """
        Maximal Marginal Relevance: chọn lần lượt j = argmax [λ·Score(j) − (1 − λ)·max_{k đã chọn} cos(v_j, v_k)].
        Score (Hybrid, trong [0, 1]) và cosine (vector thuộc tính không âm, trong [0, 1]) cùng thang. λ = 1 → giống
        _rank; bằng nhau thì giữ thứ tự của _rank. Trả về vị trí trong `candidates` như _rank.
        """
        order = self._rank(candidates, score, prior)
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
                  alpha: float | None = None, preferences: Preferences | None = None) -> list[Recommendation]:
        """Danh sách hiển thị cho khách (xem personal)."""
        return self.personal(user_id, n, exclude, alpha, preferences)[0]

    def personal(self, user_id: int | None, n: int = 8, exclude: list[int] | None = None,
                 alpha: float | None = None, preferences: Preferences | None = None) -> tuple[list[Recommendation], str]:
        """
        Danh sách "Dành cho bạn" và căn cứ của nó:
          BEHAVIOR: khách đã có hành vi → như recommend_alphas, thêm bộ lọc phụ kiện theo đời máy;
          PREFERENCES: khách chưa có hành vi nhưng đã trả lời bảng hỏi → from_preferences (không dùng hành vi của
            khách khác);
          POPULAR: chưa có cả hai → danh sách phổ biến (giao diện hiện bảng hỏi thay cho danh sách này).
        """
        state = self._user_state(user_id)
        if state is not None:
            return self._behavior_lists(state, n, [config.ALPHA if alpha is None else alpha], self._allowed(exclude),
                                        own_models_only=True)[0], BASIS_BEHAVIOR
        if preferences is not None and preferences.category_ids:
            result = self.from_preferences(preferences, n, exclude)
            if result:
                return result, BASIS_PREFERENCES
        return self.popular(n, self._allowed(exclude)), BASIS_POPULAR

    def _allowed(self, exclude: list[int] | None) -> np.ndarray:
        """Sản phẩm được phép gợi ý: đang bán, có giá, không nằm trong exclude."""
        m = self.model
        allowed = m.valid.copy()
        for pid in exclude or []:
            if pid in m.index:
                allowed[m.index[pid]] = False
        return allowed

    def recommend_alphas(self, user_id: int | None, n: int, alphas: list[float],
                         exclude: list[int] | None = None, mmr_lambda: float | None = None,
                         cold_start_k: int | None = None, own_models_only: bool = False) -> list[list[Recommendation]]:
        """
        Một danh sách Top-N cho mỗi α: hồ sơ khách, điểm CBF / CF và tập ứng viên không phụ thuộc α nên chỉ tính
        một lần (đánh giá offline thử nhiều α trên cùng khách). mmr_lambda: λ của MMR (mặc định config.MMR_LAMBDA).
        own_models_only: bỏ ốp lưng, kính cường lực… của đời máy khách chưa quan tâm. Chỉ bật khi hiển thị
        (recommend); đánh giá offline không bật, nên số liệu đánh giá là của mô hình chưa có bộ lọc này.
        Khách chưa có hành vi → danh sách phổ biến (đánh giá offline không có câu trả lời bảng hỏi).
        """
        return self._behavior_lists(self._user_state(user_id), n, alphas, self._allowed(exclude), mmr_lambda,
                                    cold_start_k, own_models_only)

    def _behavior_lists(self, state, n: int, alphas: list[float], allowed: np.ndarray,
                        mmr_lambda: float | None = None, cold_start_k: int | None = None,
                        own_models_only: bool = False) -> list[list[Recommendation]]:
        mmr_lambda = config.MMR_LAMBDA if mmr_lambda is None else mmr_lambda
        m = self.model
        if state is None:
            popular = self.popular(n, allowed)
            return [popular for _ in alphas]

        rows, r, count = state
        # bỏ sản phẩm đã mua (3.5.5 a) và cả sản phẩm đã xem: danh sách "dành cho bạn" chỉ gồm sản phẩm mới với khách
        allowed[rows] = False
        if own_models_only:
            # đo 09/11: 144/2.302 khách được gợi ý ốp lưng iPhone 15 Pro dù chưa xem máy này (CF học từ độ phổ biến)
            allowed &= self._own_model_mask(rows)
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
            alpha = 1.0 if cf is None else alpha        # ít dữ liệu → chỉ CBF
            score, s_cbf, s_cf = self._hybrid(cbf, cf, candidates, alpha)
            result = []
            for pos in self._mmr(candidates, score, n, mmr_lambda):
                row = candidates[pos]
                from_cf = s_cf is not None and (1 - alpha) * s_cf[pos] > alpha * s_cbf[pos]
                result.append(Recommendation(
                    m.items.index[row], score[pos], self._reason_personal(row, rows, r, from_cf),
                    s_cbf[pos], None if s_cf is None else s_cf[pos]))
            lists.append(result)
        log.debug("%d interactions, alphas=%s", count, alphas)
        return lists

    def _own_model_mask(self, rows: np.ndarray) -> np.ndarray:
        """False ở phụ kiện theo đời máy (ốp lưng, kính cường lực…) của đời máy khách chưa xem / mua; đời máy của khách
        lấy từ cả thiết bị lẫn phụ kiện đã tương tác ('iPhone 16 Plus 128GB', 'Ốp lưng iPhone 16 Plus' → iphone 16 plus)."""
        m = self.model
        if self._model_columns is None or self._model_columns[0] is not m:
            names = m.items["name"].astype(str).tolist()
            accessory = (m.items["category"].astype(str) == ACCESSORY_CATEGORY).tolist()
            fitted = [(phone_model(x) or "") if a and MODEL_SPECIFIC.search(x) else "" for x, a in zip(names, accessory)]
            device = ["" if a else (phone_model(x) or "") for x, a in zip(names, accessory)]
            self._model_columns = (m, np.array(fitted, dtype=object), np.array(device, dtype=object))
        _, fitted, device = self._model_columns
        owned = (set(device[rows]) | set(fitted[rows])) - {""}
        return (fitted == "") | np.isin(fitted, list(owned))

    def from_preferences(self, preferences: Preferences, n: int = 8,
                         exclude: list[int] | None = None) -> list[Recommendation]:
        """
        Gợi ý cho khách chưa có hành vi theo câu trả lời bảng hỏi; không dùng hành vi của khách khác.
          1. Ứng viên: sản phẩm đang bán thuộc danh mục khách chọn (bỏ ốp lưng, kính cường lực... theo đời máy, vì
             chưa biết khách dùng máy nào).
          2. Score_Q(i) = (1 + Σ_q khớp_q(i)) / (1 + |Q_i|): số 1 là câu "danh mục" (ứng viên luôn khớp), Q_i là các
             câu khách đã trả lời và áp dụng được cho danh mục của i:
               hãng: 1 nếu hãng của i nằm trong các hãng khách chọn (chỉ tính khi khách chọn hãng có bán ở danh mục này);
               tầm giá: 1 nếu phân khúc giá của i (phân vị 1/3, 2/3 trong danh mục) là tầm giá khách chọn;
               nhu cầu: tỉ lệ nhu cầu khách chọn ở danh mục này mà i đáp ứng (product_needs).
          3. Mỗi danh mục xếp bằng MMR (λ như danh sách cá nhân; bằng điểm thì ưu tiên điểm đánh giá, rồi số lượt đánh
             giá của sản phẩm), sau đó lấy lần lượt từng vị trí của các danh mục để danh mục nào khách chọn cũng có mặt.
        """
        m = self.model
        items = m.items
        allowed = self._allowed(exclude) & self._own_model_mask(np.array([], dtype=int))
        category = items["category_id"].to_numpy()
        brand = items["brand_id"].to_numpy()
        segment = items["price_segment"].to_numpy()
        prior = (items["rating"].to_numpy(float) if "rating" in items else np.zeros(len(items)),
                 items["review_count"].to_numpy(float) if "review_count" in items else np.zeros(len(items)))
        brands = set(preferences.brand_ids)
        levels = {PRICE_LEVELS[level] for level in preferences.price_levels if level in PRICE_LEVELS}
        needs = {need for need in preferences.need_ids if need in m.needs}

        ranked = []                                        # mỗi danh mục: [(dòng, điểm, lý do)] đã xếp
        for category_id in dict.fromkeys(preferences.category_ids):
            rows = np.flatnonzero(allowed & (category == category_id))
            if rows.size == 0:
                continue
            brands_here = brands & set(brand[rows].tolist())
            needs_here = {need for need in needs if m.needs[need][0] == category_id}
            matched = np.ones(rows.size)
            asked = 1
            if brands_here:
                matched += np.isin(brand[rows], list(brands_here))
                asked += 1
            if levels:
                matched += np.isin(segment[rows], list(levels))
                asked += 1
            if needs_here:
                matched += [len(m.product_needs.get(row, frozenset()) & needs_here) / len(needs_here) for row in rows]
                asked += 1
            score = matched / asked
            order = self._mmr(rows, score, n, config.MMR_LAMBDA, prior)
            ranked.append([(rows[p], score[p], self._reason_preferences(rows[p], brands_here, levels, needs_here))
                           for p in order])

        result = []
        for position in range(n):
            # vị trí thứ `position` của mọi danh mục, điểm cao trước
            for row, score, reason in sorted((lst[position] for lst in ranked if position < len(lst)),
                                             key=lambda entry: -entry[1]):
                if len(result) < n:
                    result.append(Recommendation(items.index[row], score, reason))
        return result

    def _reason_preferences(self, row: int, brands: set, levels: set, needs: set) -> str:
        m = self.model
        items = m.items
        matched = []
        if items["brand_id"].iloc[row] in brands:
            matched.append(str(items["brand"].iloc[row]))
        segment = items["price_segment"].iloc[row]
        if segment in levels:
            matched.append(PRICE_LABELS[segment])
        for need in sorted(m.product_needs.get(row, frozenset()) & needs):
            matched.append(m.needs[need][1].lower())
        if matched:
            return "Hợp với lựa chọn của bạn: " + " · ".join(matched)
        return "Thuộc danh mục " + str(items["category"].iloc[row]) + " bạn quan tâm"

    def popular(self, n: int, allowed: np.ndarray | None = None) -> list[Recommendation]:
        """người dùng mới / chưa đăng nhập → sản phẩm phổ biến (Σ r cao nhất) + sản phẩm mới."""
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
        candidates = self._same_accessory_kind(i, candidates[candidates != i])
        order = self._rank(candidates, sims[candidates])[:n]
        return [Recommendation(m.items.index[candidates[p]], sims[candidates[p]],
                               self._reason_similar(candidates[p], i)) for p in order]

    # ---------- Upselling (3.5.6 a) ----------

    def upsell(self, product_id: int, user_id: int | None = None, n: int = 4) -> list[Recommendation]:
        """
        Bản cao hơn của sản phẩm đang xem. Ứng viên phải cùng danh mục, đang bán, đắt hơn tối đa δ = 30%, và:
          - thiết bị có thông số (RAM, bộ nhớ, camera): không kém hơn ở thông số nào mà cả hai cùng ghi, đồng thời
            tốt hơn ít nhất một thông số, hoặc cùng hãng (đời mới hơn / bản cao hơn cùng dòng, như iPhone 17 Pro Max
            → iPhone 18 Pro Max). Trước 11/11/2026 chỉ cần một thông số tốt hơn, nên iPad mini 7 (RAM 8 GB) được
            gợi ý iPad 10 (RAM 4 GB) vì bộ nhớ lớn hơn;
          - phụ kiện: cùng loại (sạc → sạc, tai nghe → tai nghe), ốp lưng / kính cường lực cùng đời máy. Trước đó
            danh mục Phụ kiện chỉ xét giá, nên ốp lưng được gợi ý "nâng cấp" lên camera IP, loa Bluetooth;
          - danh mục không có thông số (đồng hồ thời trang) chỉ xét giá.
        """
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
            diff = np.nan_to_num(raw - raw[i], nan=0.0)    # một bên không ghi thông số đó → không so
            brand = items["brand"].to_numpy()
            same_brand = (brand == brand[i]) & (brand[i] != "Unknown")
            mask &= ~(diff < 0).any(axis=1) & ((diff > 0).any(axis=1) | same_brand)

        candidates = self._same_accessory_kind(i, np.flatnonzero(mask))
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

    def _same_accessory_kind(self, anchor_row: int, candidates: np.ndarray) -> np.ndarray:
        """Phụ kiện chỉ so với phụ kiện cùng loại (sạc với sạc, tai nghe với tai nghe; compat.accessory_kind), ốp lưng
        và kính cường lực còn phải cùng đời máy. Danh mục Phụ kiện gồm nhiều loại hàng khác hẳn nhau, nên trước
        11/11/2026 "Tương tự" của camera IP là chuột, tai nghe, balo (77% gợi ý khác loại). Sản phẩm khác: giữ nguyên."""
        items = self.model.items
        if items["category"].iloc[anchor_row] != ACCESSORY_CATEGORY:
            return candidates
        names = items["name"]
        anchor = names.iloc[anchor_row]
        kind = accessory_kind(anchor)
        model = phone_model(anchor) if MODEL_SPECIFIC.search(anchor) else None
        return np.array([j for j in candidates if accessory_kind(names.iloc[j]) == kind
                         and (model is None or phone_model(names.iloc[j]) == model)], dtype=int)

    def _fits(self, anchor_row: int, other_row: int) -> bool:
        """Gợi ý từ luật mua chung cũng phải vừa máy (luật Apple, đúng đời máy, cổng sạc...) như quan hệ khai báo;
        thiết bị gợi ý thiết bị khác chỉ khi cùng hãng (compat.compatible). Đang xem phụ kiện mà gợi ý máy thì xét
        theo chiều máy → phụ kiện: tai nghe → điện thoại được, camera IP / máy chiếu → điện thoại thì không (trước
        11/11/2026 chiều này không được lọc, dữ liệu mô phỏng cho ra "camera IP mua kèm iPhone 18 Pro")."""
        items = self.model.items
        device, accessory = items.iloc[anchor_row], items.iloc[other_row]
        if device.get("category") == ACCESSORY_CATEGORY and accessory.get("category") != ACCESSORY_CATEGORY:
            device, accessory = accessory, device
        screen = device.get("screen_inch_raw")          # thông số gốc như build_relations, không điền trung vị
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
        return "Tương tự " + str(m.items["name"].iloc[nearest]) + " bạn đã quan tâm"   # đã xem, thích, thêm giỏ hoặc mua

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
