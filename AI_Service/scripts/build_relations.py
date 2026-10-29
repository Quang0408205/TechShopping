"""
Tạo quan hệ ACCESSORY trong product_relations cho Cross-selling (báo cáo mục 3.5.6 b).

Dữ liệu đơn hàng còn ít nên chưa tính được "mua chung" (lift) đáng tin; quan hệ khai báo được ưu tiên.
Quy tắc: thiết bị chính → phụ kiện thuộc nhóm phù hợp (theo từ khoá trong tên), cùng hãng được ưu tiên,
phụ kiện chỉ dành cho iPhone / Lightning / Apple Pencil chỉ gắn với sản phẩm Apple.
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


def main():
    with connect(readonly=False) as conn:
        products = conn.execute("""
            select p.product_id, p.name, c.name as category, coalesce(b.name, '') as brand,
                   coalesce(p.total_reviews, 0) as reviews
            from products p join categories c using (category_id) left join brands b using (brand_id)
            where p.deleted_at is null and coalesce(p.is_active, true)
        """).fetchall()
        accessories = [p for p in products if p["category"] == ACCESSORY_CATEGORY]
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
