"""
Đánh giá offline các mô hình khuyến nghị (báo cáo mục 3.7 / 4.8.3) và chọn α cho Hybrid.

Chia theo thời gian cho từng người dùng: 80% hành vi đầu → huấn luyện, 20% cuối → kiểm thử.
Sản phẩm liên quan (relevant) = sản phẩm người dùng tương tác trong phần kiểm thử mà CHƯA từng gặp trong phần
huấn luyện (hệ thống không gợi ý lại sản phẩm đã xem). Min-Max, x_min/x_max và ALS chỉ học trên phần huấn luyện.

Chỉ số @N:  Precision = |gợi ý ∩ liên quan| / N;   Recall = |gợi ý ∩ liên quan| / |liên quan|;
            HitRate = có ít nhất 1 sản phẩm đúng;   NDCG = Σ 1/log2(vị trí+1) / IDCG;
            Coverage = số sản phẩm khác nhau được gợi ý / số sản phẩm đang bán.

    python -m scripts.evaluate --n 10
"""

import argparse
import math

import pandas as pd

from app import config
from app.db import fetch_all
from app.recommender import data, engine


def load_events() -> pd.DataFrame:
    rows = fetch_all("""
        select ui.user_id, ui.product_id, ui.interaction_type, ui.timestamp
        from user_interactions ui join products p on p.product_id = ui.product_id and p.deleted_at is null
    """)
    return pd.DataFrame(rows)


def split(events: pd.DataFrame, train_ratio: float):
    cutoff = events.groupby("user_id")["timestamp"].quantile(train_ratio).rename("cutoff")
    events = events.join(cutoff, on="user_id")
    return events[events["timestamp"] <= events["cutoff"]], events[events["timestamp"] > events["cutoff"]]


def aggregate(events: pd.DataFrame) -> pd.DataFrame:
    return (events.groupby(["user_id", "product_id", "interaction_type"], as_index=False)
            .agg(n=("timestamp", "size"), last_at=("timestamp", "max")))


def metrics(recommended: list[int], relevant: set[int], n: int) -> dict:
    hits = [1 if pid in relevant else 0 for pid in recommended[:n]]
    dcg = sum(h / math.log2(i + 2) for i, h in enumerate(hits))
    idcg = sum(1 / math.log2(i + 2) for i in range(min(len(relevant), n)))
    return {"precision": sum(hits) / n, "recall": sum(hits) / len(relevant), "hit": float(any(hits)),
            "ndcg": dcg / idcg if idcg else 0.0}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--n", type=int, default=10)
    parser.add_argument("--train-ratio", type=float, default=0.8)
    args = parser.parse_args()

    train_events, test_events = split(load_events(), args.train_ratio)
    train_agg = aggregate(train_events)

    # mô hình chỉ được thấy dữ liệu huấn luyện
    data.load_interactions = lambda user_id=None: (
        train_agg if user_id is None else train_agg[train_agg["user_id"] == user_id])
    rec = engine.Recommender()
    rec._model = engine.train_model()
    valid_count = int(rec.model.valid.sum())

    seen = train_events.groupby("user_id")["product_id"].apply(set)
    counts = train_agg.groupby("user_id")["n"].sum()
    relevant = {u: set(g["product_id"]) - seen.get(u, set()) for u, g in test_events.groupby("user_id")}
    users = [u for u, items in relevant.items() if items and counts.get(u, 0) >= config.COLD_START_K]
    print(f"Users evaluated: {len(users)}  (train events {len(train_events)}, test events {len(test_events)})")

    configs = [("Phổ biến (baseline)", None)] + [("CBF (α=1)", 1.0)] + [("CF/ALS (α=0)", 0.0)] + [
        (f"Hybrid α={a:.1f}", a) for a in (0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9)]
    table = []
    for name, alpha in configs:
        scores, shown = [], set()
        for user in users:
            if alpha is None:
                allowed = rec.model.valid.copy()
                allowed[[rec.model.index[p] for p in seen.get(user, ()) if p in rec.model.index]] = False
                recs = rec.popular(args.n, allowed)
            else:
                recs = rec.recommend(user, args.n, alpha=alpha)
            ids = [r.product_id for r in recs]
            shown.update(ids)
            scores.append(metrics(ids, relevant[user], args.n))
        frame = pd.DataFrame(scores).mean()
        table.append({"Mô hình": name, f"Precision@{args.n}": frame["precision"], f"Recall@{args.n}": frame["recall"],
                      f"NDCG@{args.n}": frame["ndcg"], f"HitRate@{args.n}": frame["hit"],
                      "Coverage": len(shown) / valid_count})
    result = pd.DataFrame(table).set_index("Mô hình")
    print(result.round(4).to_string())
    best = result.iloc[3:][f"NDCG@{args.n}"].idxmax()
    print(f"\nBest Hybrid by NDCG@{args.n}: {best} → set REC_ALPHA accordingly")


if __name__ == "__main__":
    main()
