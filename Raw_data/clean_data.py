import re
import pandas as pd

INPUT_FILE = "Raw_data/tgdd_all_products.csv"
OUTPUT_FILE = "tgdd_products_cleaned.csv"

# 1. Đọc dữ liệu
df = pd.read_csv(INPUT_FILE)

# 2. Loại bỏ trùng lặp (Dựa trên ID sản phẩm hoặc URL)
df = df.drop_duplicates(subset=["id"], keep="first")

# 3. Chuẩn hóa Tên danh mục & Hãng
df["category_group"] = df["category_group"].astype(str).str.strip().str.title()
df["brand"] = df["brand"].astype(str).str.strip()

# 4. Chuẩn hóa Giá tiền (về số nguyên Integer, điền giá trị thiếu)
df["price"] = pd.to_numeric(df["price"], errors="coerce").fillna(0).astype(int)

# 5. Xử lý giá trị thiếu cho Rating & Số lượng bán
df["rating"] = pd.to_numeric(df["rating"], errors="coerce").fillna(0.0)
df["sold_qty"] = df["sold_qty"].fillna("0")


# 6. Trích xuất & Chuẩn hóa Specs từ Tên sản phẩm (Regex)
def extract_ram(name):
    # Tìm dạng 4GB, 8GB, 12 GB, 16 GB...
    match = re.search(r"(\d+)\s*GB\s*RAM", name, re.IGNORECASE)
    if not match:
        # Nếu không có từ 'RAM', lấy dung lượng GB đầu tiên nếu có 2 cụm GB (thường cụm 1 là RAM, cụm 2 là ROM)
        matches = re.findall(r"(\d+)\s*GB", name, re.IGNORECASE)
        if len(matches) >= 2:
            return f"{matches[0]} GB"
    return f"{match.group(1)} GB" if match else None


def extract_rom(name):
    # Tìm dạng 64GB, 128GB, 256GB, 512GB, 1TB
    match = re.search(r"(\d+)\s*(GB|TB)", name, re.IGNORECASE)
    if match:
        val, unit = match.groups()
        unit = unit.upper()
        # Chuẩn hóa 1TB -> 1024 GB hoặc giữ nguyên dạng GB
        if unit == "TB":
            return f"{int(val) * 1024} GB"
        return f"{val} GB"
    return None


# Tạo các cột Specs riêng biệt
df["ram"] = df["name"].apply(extract_ram)
df["rom"] = df["name"].apply(extract_rom)

# 7. Sắp xếp lại thứ tự cột cho gọn gàng
columns_order = [
    "id",
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
]
df = df.reindex(columns=[col for col in columns_order if col in df.columns])

# 8. Lưu file sạch
df.to_csv(OUTPUT_FILE, index=False, encoding="utf-8-sig")
print(f"Đã làm sạch dữ liệu! File mới được lưu tại: {OUTPUT_FILE}")