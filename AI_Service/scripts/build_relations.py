"""
Tạo quan hệ ACCESSORY trong product_relations cho Cross-selling.

Dữ liệu đơn hàng còn ít nên chưa tính được "mua chung" (lift) đáng tin; quan hệ khai báo được ưu tiên.
Quy tắc: thiết bị chính → phụ kiện thuộc nhóm phù hợp (theo từ khoá trong tên), cùng hãng được ưu tiên,
phụ kiện chỉ dành cho iPhone / Lightning / Apple Pencil chỉ gắn với sản phẩm Apple. Phụ kiện chỉ vừa một số máy
(fits): ốp lưng / kính / miếng dán ghi tên đời máy chỉ gắn với đúng đời đó ("Ốp lưng iPhone 16" không gắn với iPhone 16e,
16 Plus hay 17e), Apple Pencil Pro chỉ với iPad Pro / Air / mini, balo / túi laptop ghi cỡ inch chỉ với laptop màn hình
không lớn hơn.
Chạy lại an toàn: xoá các dòng ACCESSORY cũ rồi tạo lại.

    python -m scripts.build_relations
"""

import re

from app.db import connect

ACCESSORY_CATEGORY = "Phụ Kiện"
PER_PRODUCT = 8

# danh mục thiết bị → nhóm phụ kiện phù hợp (so khớp tên phụ kiện, không phân biệt hoa thường)
RULES = {
    "Điện Thoại": ("ốp lưng", "kính cường lực", "sạc", "cáp", "pin sạc dự phòng", "tai nghe", "airpods"),
    "Máy Tính Bảng": ("apple pencil", "bàn phím", "sạc", "cáp", "pin sạc dự phòng", "tai nghe", "airpods"),
    "Laptop": ("chuột", "bàn phím", "balo laptop", "túi chống sốc laptop", "tai nghe", "router", "sạc nhanh"),
    "Đồng Hồ Thông Minh": ("sạc", "tai nghe", "airpods", "pin sạc dự phòng"),
    "Màn Hình Máy Tính": ("cáp", "chuột", "bàn phím", "tai nghe", "loa"),
    "Máy Tính Để Bàn": ("chuột", "bàn phím", "tai nghe", "loa", "router"),
}
APPLE_ONLY = re.compile(r"iphone|lightning|apple pencil|magic keyboard|magic mouse|magsafe iphone", re.IGNORECASE)
MODEL_SPECIFIC = re.compile(r"ốp|kính cường lực|miếng dán|bao da", re.IGNORECASE)
PHONE_MODEL = re.compile(r"\b(iphone|galaxy)\s+([a-z]?\d+[a-z]?)(?:\s+(pro max|pro|plus|ultra|fe))?\b", re.IGNORECASE)
INCH = re.compile(r"(\d+(?:[.,]\d+)?)\s*(?:inch|\")", re.IGNORECASE)


def phone_model(name: str) -> str | None:
    """'Ốp lưng MagSafe iPhone 16 Nhựa' → 'iphone 16'; 'iPhone 16 Plus 128GB' → 'iphone 16 plus'."""
    match = PHONE_MODEL.search(name)
    return " ".join(part.lower() for part in match.groups() if part) if match else None


def inches(text: str | None) -> float | None:
    match = INCH.search(text or "")
    return float(match.group(1).replace(",", ".")) if match else None


def fits(device_name: str, device_category: str, accessory_name: str, device_screen: str | None = None) -> bool:
    """Phụ kiện có dùng được với máy này không (ngoài luật theo nhóm và luật Apple)."""
    name = accessory_name.lower()
    if MODEL_SPECIFIC.search(name) and phone_model(name):
        return phone_model(device_name) == phone_model(name)
    if "apple pencil pro" in name:
        return device_category == "Máy Tính Bảng" and re.search(r"ipad (pro|air|mini)", device_name.lower()) is not None
    if device_category == "Laptop" and re.search(r"balo|túi", name) and inches(name):
        screen = inches(device_screen)
        return screen is not None and screen <= inches(name) + 0.05
    return True


def main():
    with connect(readonly=False) as conn:
        products = conn.execute("""
            select p.product_id, p.name, c.name as category, coalesce(b.name, '') as brand,
                   coalesce(p.total_reviews, 0) as reviews
            from products p join categories c using (category_id) left join brands b using (brand_id)
            where p.deleted_at is null and coalesce(p.is_active, true)
        """).fetchall()
        accessories = [p for p in products if p["category"] == ACCESSORY_CATEGORY]
        screens = {row["product_id"]: row["spec_value"] for row in conn.execute(
            "select product_id, spec_value from product_specifications where spec_name = 'Kích thước màn hình'")}
        removed = conn.execute("delete from product_relations where relation_type = 'ACCESSORY'").rowcount

        rows = []
        for device in products:
            keywords = RULES.get(device["category"])
            if not keywords:
                continue
            is_apple = device["brand"].lower() == "apple"
            matches = []
            for acc in accessories:
                name = acc["name"].lower()
                rank = next((k for k, word in enumerate(keywords) if word in name), None)
                if rank is None or (APPLE_ONLY.search(name) and not is_apple):
                    continue
                if not fits(device["name"], device["category"], acc["name"], screens.get(device["product_id"])):
                    continue
                same_brand = acc["brand"].lower() == device["brand"].lower()
                strength = (0.9 if same_brand else 0.7) - 0.03 * rank
                matches.append((strength, acc["reviews"], acc["product_id"], rank))
            matches.sort(reverse=True)
            # mỗi nhóm phụ kiện lấy tối đa 2 để danh sách đa dạng
            per_group, picked = {}, 0
            for strength, _, accessory_id, rank in matches:
                if picked == PER_PRODUCT or per_group.get(rank, 0) == 2:
                    continue
                per_group[rank] = per_group.get(rank, 0) + 1
                picked += 1
                rows.append((device["product_id"], accessory_id, "ACCESSORY", round(strength, 3)))

        with conn.cursor() as cur:
            cur.executemany("""
                insert into product_relations (product_id, related_product_id, relation_type, strength)
                values (%s, %s, %s, %s)
                on conflict (product_id, related_product_id, relation_type) do update set strength = excluded.strength
            """, rows)
        print(f"Removed {removed} old ACCESSORY relations, created {len(rows)}")


if __name__ == "__main__":
    main()
