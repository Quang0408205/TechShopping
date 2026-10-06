import re
from pathlib import Path

import pandas as pd

INPUT_FILE = Path(__file__).with_name("tgdd_all_products.csv")
OUTPUT_FILE = Path(__file__).with_name("tgdd_products_cleaned.csv")

# 1. Đọc dữ liệu
df = pd.read_csv(INPUT_FILE, dtype={"id": "string"})

# 2. Loại bỏ trùng lặp theo ID; dùng URL khi ID không có.
df["id"] = df["id"].fillna("").astype(str).str.strip()
df["url"] = df["url"].fillna("").astype(str).str.strip()
df["source_key"] = df["id"]
missing_id = df["source_key"].eq("")
df.loc[missing_id, "source_key"] = df.loc[missing_id, "url"].map(
    lambda url: f"url:{url}" if url else ""
)
missing_key = df["source_key"].eq("")
df.loc[missing_key, "source_key"] = [
    f"row:{index}" for index in df.index[missing_key]
]
df = df.drop_duplicates(subset=["source_key"], keep="first")

# 3. Chuẩn hóa Tên danh mục & Hãng
df["category_group"] = df["category_group"].fillna("").astype(str).str.strip().str.title()
df.loc[df["category_group"].str.lower().isin(["nan", "none"]), "category_group"] = ""
df["brand"] = df["brand"].fillna("").astype(str).str.strip()
df.loc[df["brand"].str.lower().isin(["nan", "none"]), "brand"] = ""

# 4. Giá không hợp lệ thì loại bỏ, không biến dữ liệu thiếu thành giá 0.
df["price"] = pd.to_numeric(df["price"], errors="coerce")
df = df[
    df["price"].notna()
    & df["price"].gt(0)
    & df["price"].lt(10**13)
].copy()
df["price"] = df["price"].astype("int64")


def parse_sold(s):
    s = str(s).lower().replace("đã bán", "").strip().replace(",", ".")
    if s in ("", "nan", "none"):
        return pd.NA
    if s.endswith("k"):
        try:
            return int(float(s[:-1]) * 1000)
        except (ValueError, OverflowError):
            return pd.NA
    try:
        return int(float(s))
    except (ValueError, OverflowError):
        return pd.NA


# 5. Giữ giá trị thiếu là thiếu, không coi như rating / số bán bằng 0.
df["rating"] = pd.to_numeric(df["rating"], errors="coerce")
df["sold_qty"] = df["sold_qty"].apply(parse_sold).astype("Int64")

TOKEN = re.compile(r"(\d+)\s*(GB|TB)\b", re.IGNORECASE)

def to_gb(val, unit):
    return int(val) * 1024 if unit.upper() == "TB" else int(val)


def matched_size(match):
    if not match:
        return None
    groups = match.groups()
    return to_gb(*(groups[:2] if groups[0] else groups[2:]))


def extract_ram_rom(name, category):
    # chỉ áp dụng cho nhóm có RAM / ROM
    if category not in ("Laptop", "Điện Thoại", "Máy Tính Bảng"):
        return None, None
    lower_name = name.casefold()
    ram_match = re.search(
        r"(?:(\d+)\s*(GB|TB)\s*(?:RAM|bộ nhớ RAM)\b|(?:RAM|bộ nhớ RAM)\s*(\d+)\s*(GB|TB))",
        name,
        re.IGNORECASE,
    )
    rom_match = re.search(
        r"(?:(\d+)\s*(GB|TB)\s*(?:ROM|SSD|eMMC|UFS|bộ nhớ trong|storage)\b|"
        r"(?:ROM|SSD|eMMC|UFS|bộ nhớ trong|storage)\s*(\d+)\s*(GB|TB))",
        name,
        re.IGNORECASE,
    )
    ram = matched_size(ram_match)
    rom = matched_size(rom_match)
    if ram is not None or rom is not None:
        return ram, rom
    if re.search(r"\b(?:RTX|GTX|Radeon|GeForce|Arc)\b", lower_name):
        return None, None

    sizes = [to_gb(v, u) for v, u in TOKEN.findall(name)]
    if not sizes:
        return None, None
    if len(sizes) == 1:  # chỉ có một cụm: >= 64 là ROM (vd iPhone 256GB), nhỏ hơn là RAM
        return (None, sizes[0]) if sizes[0] >= 64 else (sizes[0], None)
    if len(sizes) > 2:
        return None, None
    if sizes[0] >= 64 and sizes[1] < 64:
        return sizes[1], sizes[0]
    ram = sizes[0]
    rom = sizes[1] if sizes[1] >= 64 else None
    return ram, rom

df[["ram", "rom"]] = df.apply(
    lambda r: pd.Series(extract_ram_rom(r["name"], r["category_group"])), axis=1
)
df["ram"] = df["ram"].astype("Int64")
df["rom"] = df["rom"].astype("Int64")


# 7. Sắp xếp lại thứ tự cột cho gọn gàng
columns_order = [
    "id",
    "source_key",
    "category_group",
    "brand",
    "name",
    "price",
    "ram",
    "rom",
    "color",
    "rating",
    "sold_qty",
    "url",
    "image",
    "source_sku",
    "description",
    "images",
    "specifications",
    "review_count",
    "detail_status",
    "category_complete",
]
df = df.reindex(columns=[col for col in columns_order if col in df.columns])

# 8. Lưu file sạch
df.to_csv(OUTPUT_FILE, index=False, encoding="utf-8-sig")
print(f"Đã làm sạch dữ liệu! File mới được lưu tại: {OUTPUT_FILE}")