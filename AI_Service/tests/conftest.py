"""
Dữ liệu nhỏ dựng tay cho unit test: không cần database. train_model đọc sản phẩm, thông số, đơn hàng và quan hệ qua
app.recommender.data, nên các hàm đó được thay bằng bảng dựng sẵn; hành vi người dùng truyền qua load_interactions.
"""

from datetime import datetime

import pandas as pd
import pytest

from app.recommender import data, engine

PHONE, LAPTOP, ACCESSORY = 1, 2, 3

# product_id, tên, danh mục, hãng, giá, RAM, bộ nhớ, đang bán
CATALOG = [
    (1, "iPhone 17", PHONE, "Apple", 28_490_000, "8 GB", "256 GB", True),
    (2, "iPhone 16e", PHONE, "Apple", 16_990_000, "8 GB", "128 GB", True),
    (3, "Galaxy S25", PHONE, "Samsung", 20_000_000, "12 GB", "256 GB", True),
    (4, "Galaxy A56", PHONE, "Samsung", 9_000_000, "8 GB", "128 GB", True),
    (5, "ThinkPad E14", LAPTOP, "Lenovo", 38_390_000, "16 GB", "512 GB", True),
    (6, "Yoga Slim 7", LAPTOP, "Lenovo", 40_990_000, "32 GB", "1 TB", True),
    (7, "MacBook Air", LAPTOP, "Apple", 27_000_000, "16 GB", "256 GB", True),
    (8, "Ốp lưng iPhone", ACCESSORY, "Apple", 500_000, None, None, True),
    (9, "Điện thoại ngừng bán", PHONE, "Apple", 15_000_000, "8 GB", "128 GB", False),
]
CATEGORY_NAMES = {PHONE: "Điện Thoại", LAPTOP: "Laptop", ACCESSORY: "Phụ kiện"}


def catalog_frame() -> pd.DataFrame:
    created = datetime(2026, 10, 1)
    rows = [{"product_id": pid, "name": name, "category_id": cat, "category": CATEGORY_NAMES[cat], "brand": brand,
             "price": float(price), "variant_ram": ram, "variant_storage": storage, "is_active": active,
             "review_count": 0, "rating": 0, "updated_at": created, "created_at": created}
            for pid, name, cat, brand, price, ram, storage, active in CATALOG]
    return pd.DataFrame(rows).set_index("product_id", drop=False)


def interactions_frame(events) -> pd.DataFrame:
    """events: (user_id, product_id, hành vi, số lần) → bảng như data.load_interactions."""
    at = datetime(2026, 10, 20)
    return pd.DataFrame([{"user_id": u, "product_id": p, "interaction_type": t, "n": n, "last_at": at}
                         for u, p, t, n in events],
                        columns=["user_id", "product_id", "interaction_type", "n", "last_at"])


# Nhóm thích Apple (101–104), nhóm thích Samsung (105–106), nhóm laptop Lenovo (107–108);
# 200: khách Apple có 6 lượt (đủ k = 5 → Hybrid); 201: khách mới chỉ 2 lượt (dưới k → chỉ CBF)
EVENTS = [
    (101, 1, "PURCHASE", 1), (101, 2, "VIEW", 2), (101, 8, "PURCHASE", 1), (101, 7, "CLICK", 1),
    (102, 1, "ADD_TO_CART", 1), (102, 8, "PURCHASE", 1), (102, 7, "VIEW", 1),
    (103, 2, "PURCHASE", 1), (103, 8, "ADD_TO_CART", 1), (103, 1, "VIEW", 3),
    (104, 7, "PURCHASE", 1), (104, 1, "CLICK", 1), (104, 8, "VIEW", 1),
    (105, 3, "PURCHASE", 1), (105, 4, "VIEW", 2),
    (106, 4, "PURCHASE", 1), (106, 3, "CLICK", 1),
    (107, 5, "PURCHASE", 1), (107, 6, "VIEW", 2),
    (108, 6, "ADD_TO_CART", 1), (108, 5, "VIEW", 1),
    (200, 1, "ADD_TO_CART", 1), (200, 1, "CLICK", 1), (200, 2, "VIEW", 2), (200, 7, "VIEW", 2),
    (201, 3, "VIEW", 1), (201, 4, "CLICK", 1),
]


@pytest.fixture
def toy_recommender(monkeypatch):
    """Recommender học trên CATALOG + EVENTS, không đụng database."""
    monkeypatch.setattr(data, "load_products", catalog_frame)
    monkeypatch.setattr(data, "load_specs", lambda: pd.DataFrame(columns=["product_id", "spec_name", "spec_value"]))
    monkeypatch.setattr(data, "load_order_baskets", lambda: pd.DataFrame(columns=["order_id", "user_id", "product_id"]))
    monkeypatch.setattr(data, "load_relations", lambda: pd.DataFrame(
        columns=["product_id", "related_product_id", "relation_type", "strength"]))
    events = interactions_frame(EVENTS)

    def load(user_id=None):
        return events if user_id is None else events[events["user_id"] == user_id]

    rec = engine.Recommender(load_interactions=load)
    rec._model = engine.train_model(load_interactions=load)
    return rec
