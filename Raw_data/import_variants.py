"""Nạp kết quả của crawl_variants.py (Raw_data/crawl_v5/*.jsonl) vào database: đủ phiên bản × màu + ảnh theo màu.

Chỉ thêm + cập nhật, không xoá:
  * sản phẩm đã có (nhận ra qua URL trong tgdd_products_cleaned.csv → sku TGDD-<mã nguồn>) giữ product_id;
    phiên bản cũ khớp (bộ nhớ + màu) giữ variant_id, nên giỏ / đơn / tồn kho không hỏng; phiên bản mới được thêm;
  * ảnh gắn theo tên màu (product_images.color), ảnh xuất hiện ở mọi màu là ảnh chung (color NULL);
  * --sample-stock: tồn kho mẫu cho phiên bản mới còn hàng (mỗi chi nhánh ~90% phiên bản có 5–40 cái).
Mọi thứ chạy trong một transaction; --dry-run chạy hết rồi rollback.
"""
import argparse
import csv
import json
import os
import random
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path
from urllib.parse import urlparse

import psycopg

import import_catalog as ic

CRAWL_DIR = Path(__file__).with_name("crawl_v5")
SKU_PREFIX = ic.SKU_PREFIX
MEMORY = re.compile(r"(\d+(?:[.,]\d+)?)\s*(GB|TB)\b", re.IGNORECASE)


def norm(value):
    """So khớp màu / bộ nhớ không phân biệt hoa thường, dấu cách."""
    return re.sub(r"\s+", "", ic.nonempty(value)).casefold()


def path_of(url):
    return urlparse(url).path.rstrip("/").lower()


def url_source_keys():
    """đường dẫn TGDĐ -> mã nguồn (cột source_key của CSV đã import trước đó)."""
    with ic.INPUT_FILE.open("r", encoding="utf-8-sig", newline="") as handle:
        return {path_of(r["url"]): r["source_key"] for r in csv.DictReader(handle) if ic.nonempty(r.get("source_key"))}


def combo_fields(combo, specs):
    """(màu, bộ nhớ, RAM, có-tính-làm-thuộc-tính) của một tổ hợp."""
    labels = combo["labels"]
    color = next((v for k, v in labels.items() if "màu" in k.lower()), "")
    others = " ".join(v for k, v in labels.items() if "màu" not in k.lower())
    tokens = [ic.size_text(f"{n} {u}") for n, u in MEMORY.findall(others)]
    ram = storage = ""
    if len(tokens) >= 2:
        ram, storage = tokens[-2], tokens[-1]
    elif len(tokens) == 1:
        if re.search(r"\bram\b", others, re.IGNORECASE):
            ram = tokens[0]
        else:
            storage = tokens[0]
    free_text = "" if tokens else others.strip()[:50]
    spec_color, spec_storage, spec_ram = ic.variant_details({"color": color}, specs)
    return (spec_color or color)[:50], storage or free_text or spec_storage, ram or spec_ram, bool(tokens or not free_text)


def split_images(record):
    """{màu: [url]} riêng của màu, và [url] chung (xuất hiện ở mọi màu hoặc sản phẩm không có màu)."""
    per_color = defaultdict(list)
    for combo in record["combos"]:
        color = next((v for k, v in combo["labels"].items() if "màu" in k.lower()), "")
        for url in combo["images"]:
            if ic.image_key(url) not in {ic.image_key(u) for u in per_color[color]}:
                per_color[color].append(url)
    colors = [c for c in per_color if c]
    common = []
    if not colors:
        fallback = per_color.get("", []) or [u for u in record.get("images") or [] if isinstance(u, str)]
        return {}, fallback
    keys = [{ic.image_key(u) for u in per_color[c]} for c in colors]
    shared = set.intersection(*keys) if len(colors) > 1 else set()
    if len(colors) > 1:
        first = per_color[colors[0]]
        common = [u for u in first if ic.image_key(u) in shared]
    own = {c: [u for u in per_color[c] if ic.image_key(u) not in shared] for c in colors}
    common += [u for u in per_color.get("", []) if ic.image_key(u) not in {ic.image_key(x) for x in common}]
    return {c: urls for c, urls in own.items() if urls}, common


def fallback_brand(name):
    """Tên không khớp hãng nào đã biết: Mac / iMac -> Apple, 'MiniPC <hãng> …' -> hãng, còn lại từ đầu tiên."""
    words = name.split()
    if not words:
        return ""
    if words[0].casefold() in {"mac", "imac"}:
        return "Apple"
    if words[0].casefold() == "minipc" and len(words) > 1:
        return words[1]
    return words[0] if words[0].isalpha() or words[0].isupper() else ""


class VariantImporter:
    def __init__(self, cursor, rows_for_brands):
        self.cursor = cursor
        self.keys = url_source_keys()
        self.brands = ic.BrandStore(cursor)
        self.known_brands = self.brands.known_names(r.get("brand") for r in rows_for_brands)
        self.watch_brands = ic.collect_watch_brands(rows_for_brands)
        self.attribute_ids = {}
        self.new_variants = []
        self.stats = Counter()

    # ----- sản phẩm
    def find_product(self, record):
        cursor = self.cursor
        wanted = []
        for url in [record["url"]] + [c["url"] for c in record["combos"]]:
            key = self.keys.get(path_of(url))
            if key and key not in wanted:
                wanted.append(key)
        own_sku = f"{SKU_PREFIX}{record['combos'][0]['code']}"[:50]
        ids = []
        # lần nhập trước đã thêm phiên bản `TGDD-<mã>`; file gộp có thể đã dời chúng sang sản phẩm khác
        for combo in record["combos"]:
            cursor.execute("SELECT product_id FROM product_variants WHERE sku_variant = %s",
                           (f"{SKU_PREFIX}{combo['code']}"[:50],))
            ids += [r[0] for r in cursor.fetchall() if r[0] not in ids]
        for key in wanted:
            cursor.execute(
                """
                SELECT product_id FROM products WHERE sku = %s
                UNION SELECT product_id FROM product_variants WHERE sku_variant = %s
                """,
                (f"{SKU_PREFIX}{key}", f"{SKU_PREFIX}{key}-V1"),
            )
            ids += [r[0] for r in cursor.fetchall() if r[0] not in ids]
        cursor.execute("SELECT product_id FROM products WHERE sku = %s", (own_sku,))
        ids += [r[0] for r in cursor.fetchall() if r[0] not in ids]
        return sorted(ids), own_sku

    def base_name(self, record):
        name = ic.nonempty(record["name"])
        for combo in record["combos"][:1]:
            for label, value in combo["labels"].items():
                if "màu" not in label.lower() and name.casefold().endswith(" " + value.casefold()):
                    name = name[: -len(value)].rstrip()
        return name[:255]

    def import_record(self, record):
        cursor = self.cursor
        combos = [c for c in record["combos"] if c["price"] and c["price"] > 0]
        if not combos:
            self.stats["skipped_no_price"] += 1
            return
        record = {**record, "combos": combos}
        ids, own_sku = self.find_product(record)
        category = record["category_group"]
        category_id = ic.upsert_category(cursor, category)
        specs = ic.clean_specifications(record["specifications"] or {}, None)
        name = self.base_name(record)
        brand = ic.canonical_brand(record.get("brand")) or ic.infer_brand(
            name, category, record["url"], self.known_brands, self.watch_brands)
        brand = brand or fallback_brand(name)
        brand_id = self.brands.resolve(brand)
        description = ic.nonempty(record.get("description")) or None
        base_price = int(min(c["price"] for c in combos))

        if ids:
            product_id = ids[0]
            cursor.execute(
                """
                UPDATE products SET description = COALESCE(%s, description), brand_id = COALESCE(%s, brand_id),
                    category_id = %s, updated_at = current_timestamp WHERE product_id = %s
                """,
                (description, brand_id, category_id, product_id),
            )
            self.stats["updated"] += 1
            if len(ids) > 1:
                self.stats["duplicate_products"] += len(ids) - 1
        else:
            cursor.execute(
                """
                INSERT INTO products (name, slug, sku, description, brand_id, category_id, base_price)
                VALUES (%s, %s, %s, %s, %s, %s, %s) RETURNING product_id
                """,
                (name, ic.unique_slug(cursor, name, record["combos"][0]["code"]), own_sku, description,
                 brand_id, category_id, base_price),
            )
            product_id = cursor.fetchone()[0]
            self.stats["inserted"] += 1
        if not brand_id:
            self.stats["brand_missing"] += 1
        self.save_variants(product_id, category_id, record, specs, bool(ids))
        self.save_images(product_id, name, record)
        self.save_specifications(product_id, specs)

    # ----- phiên bản
    def save_variants(self, product_id, category_id, record, specs, existed):
        cursor = self.cursor
        cursor.execute(
            "SELECT variant_id, color, storage, ram, sku_variant FROM product_variants WHERE product_id = %s ORDER BY variant_id",
            (product_id,),
        )
        existing = [dict(zip(("id", "color", "storage", "ram", "sku"), r)) for r in cursor.fetchall()]
        claimed = set()
        for combo in record["combos"]:
            color, storage, ram, memory_like = combo_fields(combo, specs)
            match = next((v for v in existing if v["id"] not in claimed and norm(v["storage"]) == norm(storage)
                          and norm(v["color"]) == norm(color)), None)
            if match is None and existed:
                # phiên bản cũ chưa có màu (crawler cũ không lấy): nhận cho tổ hợp cùng bộ nhớ
                match = next((v for v in existing if v["id"] not in claimed and not ic.nonempty(v["color"])
                              and norm(v["storage"]) == norm(storage)), None)
            name = ic.variant_name(color, storage, ram)
            if match:
                claimed.add(match["id"])
                variant_id = match["id"]
                cursor.execute(
                    """
                    UPDATE product_variants SET variant_name = %s, price = %s, color = %s, storage = %s, ram = %s,
                        updated_at = current_timestamp WHERE variant_id = %s
                    """,
                    (name, int(combo["price"]), color or None, storage or None, ram or None, variant_id),
                )
                self.stats["variants_updated"] += 1
            else:
                sku_variant = f"{SKU_PREFIX}{combo['code']}"[:50]
                cursor.execute("SELECT variant_id FROM product_variants WHERE sku_variant = %s", (sku_variant,))
                found = cursor.fetchone()
                if found:  # chạy lại lần 2: tổ hợp đã thêm
                    variant_id = found[0]
                    cursor.execute(
                        "UPDATE product_variants SET price = %s, updated_at = current_timestamp WHERE variant_id = %s",
                        (int(combo["price"]), variant_id),
                    )
                    self.stats["variants_updated"] += 1
                else:
                    cursor.execute(
                        """
                        INSERT INTO product_variants (product_id, variant_name, sku_variant, price, color, storage, ram)
                        VALUES (%s, %s, %s, %s, %s, %s, %s) RETURNING variant_id
                        """,
                        (product_id, name, sku_variant, int(combo["price"]), color or None, storage or None, ram or None),
                    )
                    variant_id = cursor.fetchone()[0]
                    self.stats["variants_added"] += 1
                    if combo["available"]:
                        self.new_variants.append(variant_id)
            self.save_attributes(variant_id, category_id, color, storage if memory_like else "", ram)
        self.cursor.execute(
            "UPDATE products SET base_price = COALESCE((SELECT min(price) FROM product_variants WHERE product_id = %s), base_price) "
            "WHERE product_id = %s", (product_id, product_id))

    def save_attributes(self, variant_id, category_id, color, storage, ram):
        cursor = self.cursor
        value_ids = []
        for attribute_name, value in ((ic.ATTRIBUTE_COLOR, color), (ic.ATTRIBUTE_RAM, ram), (ic.ATTRIBUTE_STORAGE, storage)):
            if value:
                value_ids.append(ic.ensure_attribute_value(cursor, self.attribute_ids, category_id, attribute_name, value)[1])
        cursor.execute(
            """
            DELETE FROM variant_attribute_values vav USING attribute_values av, attributes a
            WHERE vav.variant_id = %s AND av.value_id = vav.value_id AND a.attribute_id = av.attribute_id
              AND a.name IN (%s, %s, %s) AND NOT (vav.value_id = ANY(%s::bigint[]))
            """,
            (variant_id, ic.ATTRIBUTE_COLOR, ic.ATTRIBUTE_RAM, ic.ATTRIBUTE_STORAGE, value_ids),
        )
        for value_id in value_ids:
            cursor.execute(
                "INSERT INTO variant_attribute_values (variant_id, value_id) VALUES (%s, %s) ON CONFLICT DO NOTHING",
                (variant_id, value_id),
            )

    # ----- ảnh
    def save_images(self, product_id, name, record):
        cursor = self.cursor
        own, common = split_images(record)
        cursor.execute(
            "SELECT image_id, image_url, color, COALESCE(display_order, 0), COALESCE(is_primary, false) "
            "FROM product_images WHERE product_id = %s", (product_id,))
        existing = cursor.fetchall()
        by_key = defaultdict(list)
        for image_id, url, color, order, primary in existing:
            by_key[ic.image_key(url)].append((image_id, color))
        next_order = max((e[3] for e in existing), default=-1) + 1
        has_primary = any(e[4] for e in existing)

        def put(url, color):
            nonlocal next_order, has_primary
            if urlparse(url).scheme not in {"http", "https"}:
                return
            key = ic.image_key(url)
            same = by_key.get(key, [])
            if any(c == color for _, c in same):
                return
            if color and any(c is None for _, c in same):
                # ảnh cũ chưa gắn màu mà chỉ thuộc một màu: gắn màu cho chính dòng đó
                image_id = next(i for i, c in same if c is None)
                cursor.execute("UPDATE product_images SET color = %s WHERE image_id = %s", (color, image_id))
                by_key[key] = [(image_id, color)]
                self.stats["images_colored"] += 1
                return
            if not color and same:
                return
            primary = not has_primary and not color
            cursor.execute(
                """
                INSERT INTO product_images (product_id, image_url, alt_text, display_order, is_primary, color)
                VALUES (%s, %s, %s, %s, %s, %s) RETURNING image_id
                """,
                (product_id, url, name, next_order, primary, color or None),
            )
            by_key[key].append((cursor.fetchone()[0], color or None))
            has_primary = has_primary or primary
            next_order += 1
            self.stats["images_added"] += 1

        for url in common:
            put(url, None)
        for color, urls in own.items():
            for url in urls:
                put(url, color)
        if not has_primary:
            cursor.execute(
                "UPDATE product_images SET is_primary = true WHERE image_id = "
                "(SELECT image_id FROM product_images WHERE product_id = %s ORDER BY (color IS NULL) DESC, display_order, image_id LIMIT 1)",
                (product_id,))

    def save_specifications(self, product_id, specs):
        rows = [(ic.nonempty(str(k)), ic.nonempty(str(v))) for k, v in specs.items()]
        rows = [(k, v) for k, v in rows if k and v and len(k) <= 100]
        if not rows:
            return
        self.cursor.execute("DELETE FROM product_specifications WHERE product_id = %s", (product_id,))
        for order, (label, value) in enumerate(rows):
            self.cursor.execute(
                "INSERT INTO product_specifications (product_id, spec_name, spec_value, spec_order) VALUES (%s, %s, %s, %s)",
                (product_id, label, value, order))
        self.stats["products_with_specs"] += 1

    # ----- tồn kho mẫu
    def sample_stock(self):
        cursor = self.cursor
        cursor.execute("SELECT store_id FROM stores ORDER BY store_id")
        stores = [r[0] for r in cursor.fetchall()]
        rng = random.Random(20261018)
        for variant_id in self.new_variants:
            for store_id in stores:
                cursor.execute("SELECT 1 FROM inventory WHERE store_id = %s AND variant_id = %s", (store_id, variant_id))
                if cursor.fetchone() or rng.random() > 0.9:
                    continue
                qty = rng.randint(5, 40)
                cursor.execute("INSERT INTO inventory (store_id, variant_id, quantity) VALUES (%s, %s, %s)",
                               (store_id, variant_id, qty))
                cursor.execute(
                    "INSERT INTO stock_movements (store_id, variant_id, movement_type, quantity_change, note) "
                    "VALUES (%s, %s, 'IN', %s, 'Nhập kho mẫu')", (store_id, variant_id, qty))
                self.stats["stock_rows"] += 1

    def finish(self):
        cursor = self.cursor
        cursor.execute(
            """
            UPDATE brands b SET is_active = true, updated_at = current_timestamp
            WHERE b.is_active = false AND EXISTS (
                SELECT 1 FROM products p WHERE p.brand_id = b.brand_id AND p.is_active AND p.deleted_at IS NULL)
            """)


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--sample-stock", action="store_true")
    parser.add_argument("--files", nargs="*", help="chỉ nạp các file JSONL này (mặc định: mọi file trong crawl_v5)")
    args = parser.parse_args()
    files = [Path(f) for f in args.files] if args.files else sorted(CRAWL_DIR.glob("*.jsonl"))
    records = []
    for f in files:
        records += [json.loads(line) for line in f.read_text(encoding="utf-8").splitlines() if line.strip()]
    with ic.INPUT_FILE.open("r", encoding="utf-8-sig", newline="") as handle:
        csv_rows = list(csv.DictReader(handle))
    brand_rows = csv_rows + [{"brand": r.get("brand"), "category_group": r["category_group"], "name": r["name"]}
                             for r in records]
    ic.REQUIRED_COLUMNS["product_images"].add("color")
    conninfo = os.environ.get("DATABASE_URL")
    with psycopg.connect(*( (conninfo,) if conninfo else ()),
                         **({} if conninfo else {"dbname": os.environ.get("PGDATABASE", "techshopping")})) as connection:
        with connection.cursor() as cursor:
            ic.check_schema(cursor)
            importer = VariantImporter(cursor, brand_rows)
            for record in records:
                importer.import_record(record)
            if args.sample_stock:
                importer.sample_stock()
            importer.finish()
            merge = ic.merge_variants(cursor)
        if args.dry_run:
            connection.rollback()
    s = importer.stats
    print(f"{'[DRY RUN, đã rollback] ' if args.dry_run else ''}Đọc {len(records)} sản phẩm từ {len(files)} file.")
    print(f"  Sản phẩm: thêm mới {s['inserted']}, cập nhật {s['updated']} (trùng thêm trong DB: {s['duplicate_products']}), "
          f"bỏ qua không giá {s['skipped_no_price']}, chưa rõ hãng {s['brand_missing']}")
    print(f"  Phiên bản: thêm {s['variants_added']}, cập nhật {s['variants_updated']}")
    print(f"  Ảnh: thêm {s['images_added']}, gắn màu cho ảnh cũ {s['images_colored']}; sản phẩm ghi lại thông số {s['products_with_specs']}")
    print(f"  Tồn kho mẫu: {s['stock_rows']} dòng; gộp phiên bản: {merge}")


if __name__ == "__main__":
    main()
