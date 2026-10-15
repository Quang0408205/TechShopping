"""Làm giàu + chuẩn hoá dữ liệu (báo cáo mục 3.5.3 c–i): trích thông số số, Min-Max, One-Hot, điểm r(u,i)."""

import re

import numpy as np
import pandas as pd

from app.config import INTERACTION_WEIGHTS

NUMERIC_FEATURES = ["ram_gb", "storage_gb", "screen_inch", "camera_mp"]

UNKNOWN_VALUES = ("hãng không công bố", "không", "đang cập nhật", "")


def _number(text: str | None, pattern: str) -> float | None:
    if not text or text.strip().lower() in UNKNOWN_VALUES:
        return None
    match = re.search(pattern, text, flags=re.IGNORECASE)
    return float(match.group(1).replace(",", ".")) if match else None


def _gigabytes(text: str | None) -> float | None:
    """"256 GB" → 256, "1 TB" → 1024; nhiều phiên bản ("8 GB|16 GB") → lấy giá trị lớn nhất."""
    if not text:
        return None
    if str(text).strip().lower() in UNKNOWN_VALUES:
        return None
    values = []
    for part in str(text).split("|"):
        match = re.search(r"(\d+(?:[.,]\d+)?)\s*(GB|TB)", part, re.IGNORECASE)
        if match:
            size = float(match.group(1).replace(",", "."))
            values.append(size * 1024 if match.group(2).upper() == "TB" else size)
    return max(values) if values else None


# Tên sản phẩm TGDĐ ghi sẵn cấu hình: "OPPO Reno16F 8GB/256GB", "Laptop Asus ... (Core 5, 16GB, 512GB, ...)"
NAME_RAM_STORAGE = re.compile(r"(\d+)\s*GB\s*[/,]\s*(\d+)\s*(GB|TB)", re.IGNORECASE)


def _from_name(name: str) -> tuple[float | None, float | None]:
    match = NAME_RAM_STORAGE.search(name or "")
    if not match:
        return None, None
    storage = float(match.group(2)) * (1024 if match.group(3).upper() == "TB" else 1)
    return float(match.group(1)), storage


def extract_numeric_specs(products: pd.DataFrame, specs: pd.DataFrame) -> pd.DataFrame:
    """
    Bảng 3.17: ram_gb, storage_gb, screen_inch, camera_mp (None khi không có).
    Thứ tự tin cậy: tên sản phẩm → bảng thông số → cột ram/storage của phiên bản. Với laptop, dữ liệu import
    ghi RAM vào cột storage của phiên bản ("16 GB"), nên dung lượng < 64 GB ở đó bị bỏ qua.
    """
    by_product = {pid: dict(zip(g.spec_name, g.spec_value)) for pid, g in specs.groupby("product_id")}
    rows = []
    for product in products.itertuples():
        spec = by_product.get(product.product_id, {})
        name_ram, name_storage = _from_name(product.name)
        variant_storage = _gigabytes(product.variant_storage)
        if variant_storage is not None and variant_storage < 64 and product.category == "Laptop":
            variant_storage = None
        rows.append({
            "product_id": product.product_id,
            "ram_gb": name_ram or _gigabytes(spec.get("RAM")) or _gigabytes(spec.get("Loại RAM"))
                      or _gigabytes(product.variant_ram),
            "storage_gb": name_storage or _gigabytes(spec.get("Dung lượng lưu trữ")) or _gigabytes(spec.get("Ổ cứng"))
                          or variant_storage,
            "screen_inch": _number(spec.get("Kích thước màn hình"), r"(\d+(?:[.,]\d+)?)"),
            "camera_mp": _number(spec.get("Độ phân giải camera sau"), r"(\d+(?:[.,]\d+)?)\s*MP"),
        })
    return pd.DataFrame(rows).set_index("product_id")


def min_max(values: pd.Series) -> pd.Series:
    """x' = (x − x_min) / (x_max − x_min); cột hằng số → 0."""
    low, high = values.min(), values.max()
    if pd.isna(low) or high == low:
        return pd.Series(0.0, index=values.index)
    return (values - low) / (high - low)


def build_item_features(products: pd.DataFrame, specs: pd.DataFrame) -> tuple[pd.DataFrame, np.ndarray]:
    """
    Trả về (bảng thuộc tính đã làm giàu, ma trận vector v_i).
    v_i = [One-Hot danh mục | One-Hot thương hiệu | One-Hot phân khúc giá | giá chuẩn hoá | thông số chuẩn hoá].
    Giá và phân khúc giá tính TRONG từng danh mục (điện thoại 5 triệu là "thấp", đồng hồ 5 triệu là "cao").
    """
    items = products.join(extract_numeric_specs(products, specs))

    # c. giá trị thiếu: trung vị cùng danh mục; danh mục không có thông số đó → 0 sau chuẩn hoá
    for feature in NUMERIC_FEATURES:
        items[feature] = items.groupby("category_id")[feature].transform(lambda s: s.fillna(s.median()))

    # f. phân khúc giá theo phân vị 1/3, 2/3 trong danh mục
    def segment(prices: pd.Series) -> pd.Series:
        low, high = prices.quantile(1 / 3), prices.quantile(2 / 3)
        return pd.Series(np.where(prices <= low, "Thấp", np.where(prices <= high, "Trung", "Cao")), index=prices.index)

    items["price_segment"] = items.groupby("category_id")["price"].transform(segment)
    items["price_norm"] = items.groupby("category_id")["price"].transform(min_max)
    for feature in NUMERIC_FEATURES:
        items[feature + "_norm"] = min_max(items[feature]).fillna(0.0)

    # h. One-Hot cho thuộc tính phân loại không thứ tự
    one_hot = pd.get_dummies(items[["category", "brand", "price_segment"]].astype(str), dtype=float)
    numeric = items[["price_norm"] + [f + "_norm" for f in NUMERIC_FEATURES]].astype(float)
    vectors = np.hstack([one_hot.to_numpy(), numeric.to_numpy()])
    return items, vectors


def interaction_scores(interactions: pd.DataFrame) -> pd.DataFrame:
    """i. r(u,i) = Σ_t w_t · log(1 + n(u,i,t)); trả về bảng (user_id, product_id, r, last_at)."""
    if interactions.empty:
        return pd.DataFrame(columns=["user_id", "product_id", "r", "last_at"])
    frame = interactions.copy()
    frame["r"] = frame["interaction_type"].map(INTERACTION_WEIGHTS).fillna(0) * np.log1p(frame["n"].astype(float))
    return frame.groupby(["user_id", "product_id"], as_index=False).agg(r=("r", "sum"), last_at=("last_at", "max"))
