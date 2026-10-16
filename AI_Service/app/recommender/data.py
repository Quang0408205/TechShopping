"""Thu thập dữ liệu cho khuyến nghị (báo cáo mục 3.5.2, 3.5.3 k): Product / Interaction / Transaction Data."""

import pandas as pd

from app.db import fetch_all

# Thông số dùng làm đặc trưng số (Bảng 3.17); tên lấy theo dữ liệu crawl TGDĐ
SPEC_NAMES = (
    "RAM", "Loại RAM", "Dung lượng lưu trữ", "Ổ cứng", "Kích thước màn hình", "Độ phân giải camera sau",
)

# Quan hệ dùng cho Cross-selling (product_relations.relation_type); SIMILAR không dùng cho Cross-selling
CROSS_SELL_RELATIONS = ("ACCESSORY", "COMPATIBLE", "BOUGHT_TOGETHER")


def load_products() -> pd.DataFrame:
    """Sản phẩm chưa xoá. Giá = giá bán thấp nhất trong các phiên bản (giống giá hiện trên thẻ sản phẩm)."""
    rows = fetch_all("""
        select p.product_id, p.name, p.category_id, c.name as category, coalesce(b.name, 'Unknown') as brand,
               coalesce(
                   (select min(coalesce(v.discount_price, v.price)) from product_variants v
                    where v.product_id = p.product_id),
                   p.discount_price, p.base_price) as price,
               (select string_agg(v.ram, '|') from product_variants v where v.product_id = p.product_id) as variant_ram,
               (select string_agg(v.storage, '|') from product_variants v
                where v.product_id = p.product_id) as variant_storage,
               coalesce(p.is_active, true) as is_active,
               coalesce(p.total_reviews, 0) as review_count,
               coalesce(p.rating, 0) as rating,
               coalesce(p.updated_at, p.created_at) as updated_at,
               p.created_at
        from products p
        join categories c on c.category_id = p.category_id
        left join brands b on b.brand_id = p.brand_id
        where p.deleted_at is null
        order by p.product_id
    """)
    products = pd.DataFrame(rows)
    products["price"] = products["price"].astype(float)
    return products.set_index("product_id", drop=False)


def load_specs() -> pd.DataFrame:
    rows = fetch_all(
        "select product_id, spec_name, spec_value from product_specifications where spec_name = any(%s)",
        (list(SPEC_NAMES),))
    return pd.DataFrame(rows, columns=["product_id", "spec_name", "spec_value"])


def load_interactions(user_id: int | None = None) -> pd.DataFrame:
    """Số lần n(u,i,t) mỗi hành vi t của người dùng u với sản phẩm i + thời điểm gần nhất."""
    where = "where ui.user_id = %s" if user_id is not None else ""
    rows = fetch_all(f"""
        select ui.user_id, ui.product_id, ui.interaction_type, count(*) as n, max(ui.timestamp) as last_at
        from user_interactions ui
        join products p on p.product_id = ui.product_id and p.deleted_at is null
        {where}
        group by ui.user_id, ui.product_id, ui.interaction_type
    """, (user_id,) if user_id is not None else None)
    return pd.DataFrame(rows, columns=["user_id", "product_id", "interaction_type", "n", "last_at"])


def load_order_baskets() -> pd.DataFrame:
    """Transaction Data: các sản phẩm trong từng đơn chưa huỷ (dùng cho mua kèm / lift)."""
    rows = fetch_all("""
        select distinct o.order_id, o.user_id, v.product_id
        from orders o
        join order_items oi on oi.order_id = o.order_id
        join product_variants v on v.variant_id = oi.variant_id
        where upper(o.status) <> 'CANCELLED'
    """)
    return pd.DataFrame(rows, columns=["order_id", "user_id", "product_id"])


def load_relations() -> pd.DataFrame:
    rows = fetch_all(
        "select product_id, related_product_id, relation_type, coalesce(strength, 0.5) as strength "
        "from product_relations")
    frame = pd.DataFrame(rows, columns=["product_id", "related_product_id", "relation_type", "strength"])
    frame["strength"] = frame["strength"].astype(float)
    return frame
