"""
Sinh dữ liệu hành vi MÔ PHỎNG cho Recommendation (chỉ dùng ở môi trường local / demo / đánh giá).

Hệ thống mới chạy nên user_interactions gần như trống, ALS không học được gì. Script này:
  1. ghi PURCHASE cho các đơn hàng thật đã có (đơn đặt trước khi backend ghi nhận hành vi);
  2. tạo N khách hàng mô phỏng (họ tên người Việt, email <tên><họ>5NNN@example.test, không đăng nhập được) và
     sinh chuỗi hành vi VIEW → CLICK → ADD_TO_CART → PURCHASE có cấu trúc:
       - mỗi khách có "hồ sơ": 1–2 danh mục quan tâm, 1–2 thương hiệu ưa thích, một tầm giá;
       - khách trung thành với hệ sinh thái (vd. Apple) xem cả điện thoại, máy tính bảng, phụ kiện cùng hãng,
         đây là mẫu hành vi chéo danh mục mà CF học được còn CBF thì không;
       - 10% hành vi ngẫu nhiên (nhiễu), sản phẩm nhiều đánh giá TGDĐ được xem nhiều hơn (phân phối lệch).
Báo cáo phải ghi rõ đây là dữ liệu mô phỏng.

    python -m scripts.seed_interactions --users 300 --seed 42
    python -m scripts.seed_interactions --reset          # xoá toàn bộ dữ liệu mô phỏng
"""

import argparse
import math
import random
import unicodedata
from datetime import datetime, timedelta

from app.db import connect

# Mật khẩu không đăng nhập được, cũng là dấu nhận ra khách mô phỏng (reset tìm theo nó)
UNUSABLE_PASSWORD = "!simulated-account-no-login"
# Email cũ "sim.customer.NNN@example.test" (trước 2026-10-30): đổi sang họ tên người Việt khi chạy lại
LEGACY_EMAIL_LIKE = "sim.customer.%@example.test"

FAMILY_NAMES = ("Nguyễn", "Nguyễn", "Nguyễn", "Trần", "Trần", "Lê", "Lê", "Phạm", "Hoàng", "Huỳnh",
                "Phan", "Vũ", "Võ", "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý")
MIDDLE_NAMES = {True: ("Thị", "Ngọc", "Thu", "Kim", "Thanh", "Bảo", "Mỹ", "Phương"),
                False: ("Văn", "Minh", "Quốc", "Đức", "Hoàng", "Gia", "Thanh", "Hữu")}
GIVEN_NAMES = {True: ("Anh", "Châu", "Chi", "Giang", "Hà", "Hạnh", "Hằng", "Hoa", "Hương", "Lan", "Linh", "Ly",
                      "Mai", "My", "Ngân", "Nhi", "Nhung", "Oanh", "Phương", "Quỳnh", "Tâm", "Thảo", "Trang",
                      "Trâm", "Uyên", "Vy", "Yến"),
               False: ("An", "Bảo", "Bình", "Dũng", "Duy", "Hải", "Hiếu", "Hùng", "Huy", "Khang", "Khoa", "Long",
                       "Minh", "Nam", "Phong", "Phúc", "Quân", "Quang", "Sơn", "Tài", "Thắng", "Thịnh", "Trí",
                       "Trung", "Tuấn", "Việt")}

# Mức quan tâm tới từng danh mục khi chọn hồ sơ khách (không theo số sản phẩm: 78% catalogue là đồng hồ thời trang)
CATEGORY_INTEREST = {
    "Điện Thoại": 0.32, "Laptop": 0.24, "Máy Tính Bảng": 0.10, "Đồng Hồ Thông Minh": 0.12,
    "Đồng Hồ Thời Trang": 0.14, "Phụ Kiện": 0.08,
}
ECOSYSTEM_BRANDS = ("Apple", "Samsung", "Xiaomi", "OPPO")


def load_catalog(conn):
    rows = conn.execute("""
        select p.product_id, c.name as category, coalesce(b.name, 'Unknown') as brand,
               coalesce((select min(coalesce(v.discount_price, v.price)) from product_variants v
                         where v.product_id = p.product_id), p.discount_price, p.base_price)::float as price,
               coalesce(p.total_reviews, 0) as reviews
        from products p join categories c using (category_id) left join brands b using (brand_id)
        where p.deleted_at is null and coalesce(p.is_active, true)
    """).fetchall()
    catalog = {}
    for row in rows:
        if row["price"] and row["price"] > 0:
            catalog.setdefault(row["category"], []).append(row)
    return catalog


def ascii_name(text: str) -> str:
    text = unicodedata.normalize("NFD", text.lower()).replace("đ", "d")
    return "".join(ch for ch in text if not unicodedata.combining(ch))


def sim_identity(n: int) -> tuple[str, str, str, str]:
    """Khách mô phỏng thứ n luôn ra cùng một người: "Trần Thu Hà" → hatran5012@example.test (số 5NNN không trùng
    khách mẫu của 07_sample_sales.sql)."""
    rng = random.Random(f"sim-customer-{n}")
    family = rng.choice(FAMILY_NAMES)
    female = rng.random() < 0.5
    middle, given = rng.choice(MIDDLE_NAMES[female]), rng.choice(GIVEN_NAMES[female])
    username = f"{ascii_name(given)}{ascii_name(family)}{5000 + n}"
    phone = rng.choice(("09", "03", "07", "08")) + f"{(n * 104729 + 271828) % 10**8:08d}"
    return f"{username}@example.test", username, f"{family} {middle} {given}", phone


def rename_legacy_users(conn) -> int:
    """Khách mô phỏng cũ (sim_customer_NNN, "Khách mô phỏng NNN") → họ tên như sim_identity; đơn hàng đổi tên người nhận."""
    rows = conn.execute("select user_id, email, fullname from users where email like %s",
                        (LEGACY_EMAIL_LIKE,)).fetchall()
    for row in rows:
        email, username, fullname, phone = sim_identity(int(row["email"].split("@")[0].rsplit(".", 1)[1]))
        conn.execute("""
            update users
            set email = %s, username = %s, fullname = %s, updated_at = current_timestamp,
                phone = coalesce(phone, (select o.recipient_phone from orders o where o.user_id = users.user_id
                                         order by o.order_date desc limit 1), %s)
            where user_id = %s
        """, (email, username, fullname, phone, row["user_id"]))
        conn.execute("update orders set recipient_name = %s where user_id = %s and recipient_name = %s",
                     (fullname, row["user_id"], row["fullname"]))
    return len(rows)


def reset(conn):
    deleted = conn.execute("""
        delete from user_interactions where user_id in (select user_id from users where password_hash = %s)
    """, (UNUSABLE_PASSWORD,)).rowcount
    # khách mô phỏng đã có đơn (dữ liệu doanh thu mẫu) thì giữ lại, lần tạo sau dùng lại đúng tài khoản đó
    users = conn.execute("""
        delete from users u where u.password_hash = %s and not exists (select 1 from orders o where o.user_id = u.user_id)
    """, (UNUSABLE_PASSWORD,)).rowcount
    print(f"Removed {deleted} simulated interactions and {users} simulated users")


def backfill_real_orders(conn) -> int:
    return conn.execute("""
        insert into user_interactions (user_id, product_id, interaction_type, interaction_score, timestamp)
        select distinct o.user_id, v.product_id, 'PURCHASE', 5, o.order_date
        from orders o
        join order_items oi on oi.order_id = o.order_id
        join product_variants v on v.variant_id = oi.variant_id
        where upper(o.status) <> 'CANCELLED'
          and not exists (select 1 from user_interactions ui
                          where ui.user_id = o.user_id and ui.product_id = v.product_id
                            and ui.interaction_type = 'PURCHASE' and ui.timestamp = o.order_date)
    """).rowcount


def create_users(conn, count: int) -> list[int]:
    customer_role = conn.execute("select role_id from roles where name = 'CUSTOMER'").fetchone()["role_id"]
    ids = []
    for n in range(1, count + 1):
        email, username, fullname, phone = sim_identity(n)
        row = conn.execute("""
            insert into users (email, username, password_hash, fullname, phone, is_active)
            values (%s, %s, %s, %s, %s, true)
            on conflict (email) do update set email = excluded.email
            returning user_id
        """, (email, username, UNUSABLE_PASSWORD, fullname, phone)).fetchone()
        conn.execute("insert into user_roles (user_id, role_id) values (%s, %s) on conflict do nothing",
                     (row["user_id"], customer_role))
        ids.append(row["user_id"])
    return ids


def make_persona(rng: random.Random, catalog):
    categories = [c for c in CATEGORY_INTEREST if c in catalog]
    weights = [CATEGORY_INTEREST[c] for c in categories]
    loyal = rng.random() < 0.35
    if loyal:
        brand = rng.choice(ECOSYSTEM_BRANDS)
        liked = [c for c in categories if any(p["brand"] == brand for p in catalog[c])]
        return {"categories": liked, "weights": [CATEGORY_INTEREST[c] for c in liked], "brands": {brand},
                "budget": rng.choice((0.4, 0.7, 0.95))}
    chosen = list(dict.fromkeys(rng.choices(categories, weights, k=2)))
    brands = set()
    for category in chosen:
        options = sorted({p["brand"] for p in catalog[category]})
        brands.update(rng.sample(options, k=min(len(options), rng.choice((1, 2)))))
    return {"categories": chosen, "weights": [CATEGORY_INTEREST[c] for c in chosen], "brands": brands,
            "budget": rng.choice((0.2, 0.5, 0.8))}


def pick_product(rng: random.Random, catalog, persona):
    if rng.random() < 0.1:                                   # nhiễu
        category = rng.choice(list(catalog))
        pool = catalog[category]
    else:
        category = rng.choices(persona["categories"], persona["weights"])[0]
        pool = catalog[category]
        prices = sorted(p["price"] for p in pool)
        target = prices[int(persona["budget"] * (len(prices) - 1))]
        fitting = [p for p in pool if p["brand"] in persona["brands"] and 0.6 * target <= p["price"] <= 1.5 * target]
        if not fitting:
            fitting = [p for p in pool if p["brand"] in persona["brands"]]
        if fitting and rng.random() < 0.8:
            pool = fitting
    weights = [1 + math.log1p(p["reviews"]) for p in pool]
    return rng.choices(pool, weights)[0]


def simulate(conn, user_ids, rng: random.Random, days: int = 90) -> int:
    catalog = load_catalog(conn)
    now = datetime.now().replace(microsecond=0)
    rows = []
    for user_id in user_ids:
        persona = make_persona(rng, catalog)
        moment = now - timedelta(days=days) + timedelta(minutes=rng.randint(0, 60 * 24 * 10))
        for _ in range(rng.randint(12, 60)):
            moment += timedelta(minutes=rng.randint(5, 60 * 30))
            if moment >= now:
                break
            product = pick_product(rng, catalog, persona)
            pid = product["product_id"]
            if rng.random() < 0.6:
                rows.append((user_id, pid, "CLICK", 2, moment))
            rows.append((user_id, pid, "VIEW", 1, moment + timedelta(seconds=5)))
            if rng.random() < 0.18:
                rows.append((user_id, pid, "ADD_TO_CART", 4, moment + timedelta(minutes=2)))
                if rng.random() < 0.35:
                    rows.append((user_id, pid, "PURCHASE", 5, moment + timedelta(minutes=10)))
    with conn.cursor() as cur:
        cur.executemany("""
            insert into user_interactions (user_id, product_id, interaction_type, interaction_score, timestamp)
            values (%s, %s, %s, %s, %s)
        """, rows)
    return len(rows)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--users", type=int, default=300)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--reset", action="store_true", help="only delete the simulated data")
    args = parser.parse_args()

    with connect(readonly=False) as conn:
        print(f"Renamed {rename_legacy_users(conn)} legacy simulated customers")
        reset(conn)
        if args.reset:
            return
        print(f"Backfilled {backfill_real_orders(conn)} PURCHASE interactions from existing orders")
        user_ids = create_users(conn, args.users)
        print(f"Inserted {simulate(conn, user_ids, random.Random(args.seed))} interactions "
              f"for {len(user_ids)} simulated customers")


if __name__ == "__main__":
    main()
