import csv
import hashlib
import json
import math
import os
import re
import sys
import unicodedata
from pathlib import Path
from urllib.parse import urlparse

import psycopg


INPUT_FILE = Path(__file__).with_name("tgdd_products_cleaned.csv")
REQUIRED_COLUMNS = {
    "categories": {"category_id", "name", "slug"},
    "brands": {"brand_id", "name", "slug"},
    "products": {
        "product_id", "name", "slug", "description", "brand_id",
        "category_id", "base_price", "rating", "total_reviews",
    },
    "product_variants": {
        "variant_id", "product_id", "variant_name", "sku_variant",
        "price", "discount_price",
    },
    "product_images": {"product_id", "image_url", "display_order", "is_primary"},
    "product_specifications": {"product_id", "spec_name", "spec_value", "spec_order"},
    "attributes": {"attribute_id", "name"},
    "attribute_values": {"value_id", "attribute_id", "value"},
    "category_attributes": {"category_id", "attribute_id", "is_required", "is_filterable"},
    "variant_attribute_values": {"variant_id", "value_id"},
}


def slugify(value):
    normalized = unicodedata.normalize("NFKD", value.replace("đ", "d").replace("Đ", "D"))
    ascii_value = normalized.encode("ascii", "ignore").decode("ascii")
    return re.sub(r"-+", "-", re.sub(r"[^a-z0-9]+", "-", ascii_value.lower())).strip("-")


def stable_key(value):
    readable = re.sub(r"[^A-Za-z0-9]+", "-", value).strip("-")[:24] or "item"
    digest = hashlib.sha256(value.encode("utf-8")).hexdigest()[:16]
    return f"{readable}-{digest}"


def parse_json(value, expected_type):
    if not value:
        return expected_type()
    try:
        parsed = json.loads(value)
    except (TypeError, json.JSONDecodeError) as exc:
        raise ValueError(f"CSV chứa JSON không hợp lệ: {value!r}") from exc
    if not isinstance(parsed, expected_type):
        raise ValueError(f"CSV JSON phải có kiểu {expected_type.__name__}.")
    return parsed


def optional_number(value, *, integer=False):
    if value is None or str(value).strip() == "":
        return None
    try:
        number = float(value)
    except (TypeError, ValueError):
        return None
    if not math.isfinite(number):
        return None
    if integer:
        return int(number) if number >= 0 else None
    return number if 0 <= number <= 5 else None


def nonempty(value):
    text = (value or "").strip()
    return "" if text.lower() in {"nan", "none", "null"} else text


def upsert_category(cursor, name):
    name = name[:100]
    slug = slugify(name)[:100] or f"category-{hashlib.sha256(name.encode()).hexdigest()[:12]}"
    cursor.execute(
        """
        INSERT INTO categories (name, slug)
        VALUES (%s, %s)
        ON CONFLICT (slug) DO UPDATE SET name = EXCLUDED.name, is_active = true
        RETURNING category_id
        """,
        (name, slug),
    )
    return cursor.fetchone()[0]


def upsert_brand(cursor, name):
    if not name:
        return None
    name = name[:100]
    slug = slugify(name)[:100] or f"brand-{hashlib.sha256(name.encode()).hexdigest()[:12]}"
    cursor.execute(
        """
        INSERT INTO brands (name, slug)
        VALUES (%s, %s)
        ON CONFLICT (slug) DO UPDATE SET name = EXCLUDED.name, is_active = true
        RETURNING brand_id
        """,
        (name, slug),
    )
    return cursor.fetchone()[0]


def check_schema(cursor):
    for table, columns in REQUIRED_COLUMNS.items():
        cursor.execute(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = current_schema() AND table_name = %s
            """,
            (table,),
        )
        present = {row[0] for row in cursor.fetchall()}
        if not columns.issubset(present):
            missing = ", ".join(sorted(columns - present))
            raise RuntimeError(
                f"Schema bảng {table} không khớp database/techshopping.sql; thiếu cột: {missing}"
            )


def ensure_attribute(cursor, category_id, name, value):
    if not value:
        return
    cursor.execute(
        """
        INSERT INTO attributes (name)
        VALUES (%s)
        ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name
        RETURNING attribute_id
        """,
        (name,),
    )
    attribute_id = cursor.fetchone()[0]
    cursor.execute(
        """
        INSERT INTO attribute_values (attribute_id, value)
        VALUES (%s, %s)
        ON CONFLICT (attribute_id, value) DO UPDATE SET value = EXCLUDED.value
        RETURNING value_id
        """,
        (attribute_id, value[:255]),
    )
    value_id = cursor.fetchone()[0]
    cursor.execute(
        """
        INSERT INTO category_attributes (category_id, attribute_id, is_required, is_filterable)
        VALUES (%s, %s, false, true)
        ON CONFLICT (category_id, attribute_id) DO NOTHING
        """,
        (category_id, attribute_id),
    )
    return value_id


def get_spec_attribute(specifications, names):
    for label, value in specifications.items():
        if any(name in label.casefold() for name in names) and nonempty(str(value)):
            return nonempty(str(value))
    return ""


def import_row(cursor, row):
    source_key = nonempty(row.get("source_key")) or nonempty(row.get("id"))
    source_url = nonempty(row.get("url"))
    if not source_key:
        source_key = source_url
    if not source_key or not nonempty(row.get("name")):
        return False

    category_name = nonempty(row.get("category_group"))
    try:
        parsed_price = float(row.get("price", ""))
    except (TypeError, ValueError):
        return False
    if not math.isfinite(parsed_price) or parsed_price >= 10**13:
        return False
    price = int(parsed_price)
    if not category_name or price <= 0:
        return False

    key = stable_key(source_key)
    category_id = upsert_category(cursor, category_name)
    brand_id = upsert_brand(cursor, nonempty(row.get("brand")))
    rating = optional_number(row.get("rating"))
    review_count = optional_number(row.get("review_count"), integer=True)
    description = nonempty(row.get("description")) or None
    product_slug = f"tgdd-{key}"[:255]

    cursor.execute(
        """
        INSERT INTO products
            (name, slug, description, brand_id, category_id, base_price, rating, total_reviews)
        VALUES (%s, %s, %s, %s, %s, %s, COALESCE(%s, 0), COALESCE(%s, 0))
        ON CONFLICT (slug) DO UPDATE SET
            name = EXCLUDED.name,
            description = COALESCE(EXCLUDED.description, products.description),
            brand_id = COALESCE(EXCLUDED.brand_id, products.brand_id),
            category_id = EXCLUDED.category_id,
            base_price = EXCLUDED.base_price,
            rating = COALESCE(%s, products.rating),
            total_reviews = COALESCE(%s, products.total_reviews),
            is_active = true,
            deleted_at = NULL
        RETURNING product_id
        """,
        (
            nonempty(row["name"])[:255], product_slug, description, brand_id,
            category_id, price, rating, review_count, rating, review_count,
        ),
    )
    product_id = cursor.fetchone()[0]

    sku = f"TGDD-{key}"[:50]
    cursor.execute(
        """
        INSERT INTO product_variants (product_id, variant_name, sku_variant, price)
        VALUES (%s, %s, %s, %s)
        ON CONFLICT (sku_variant) DO UPDATE SET
            product_id = EXCLUDED.product_id,
            variant_name = EXCLUDED.variant_name,
            price = EXCLUDED.price
        RETURNING variant_id
        """,
        (product_id, nonempty(row["name"])[:255], sku, price),
    )
    variant_id = cursor.fetchone()[0]

    specifications = parse_json(row.get("specifications"), dict)
    attributes = {
        "Color": nonempty(row.get("color")) or get_spec_attribute(specifications, ("màu", "color")),
        "RAM": get_spec_attribute(specifications, ("ram", "bộ nhớ ram"))
        or nonempty(str(row.get("ram", ""))),
        "Storage": get_spec_attribute(
            specifications, ("rom", "bộ nhớ trong", "dung lượng lưu trữ", "storage")
        )
        or nonempty(str(row.get("rom", ""))),
    }
    for name, value in attributes.items():
        if name in ("RAM", "Storage") and value and value.isdigit():
            value = f"{value} GB"
        value_id = ensure_attribute(cursor, category_id, name, value)
        if value_id is not None:
            cursor.execute(
                """
                INSERT INTO variant_attribute_values (variant_id, value_id)
                VALUES (%s, %s)
                ON CONFLICT (variant_id, value_id) DO NOTHING
                """,
                (variant_id, value_id),
            )

    images = parse_json(row.get("images"), list)
    listing_image = nonempty(row.get("image"))
    if listing_image and listing_image not in images:
        images.insert(0, listing_image)
    for order, image_url in enumerate(images):
        if not isinstance(image_url, str) or urlparse(image_url).scheme not in {"http", "https"}:
            continue
        cursor.execute(
            """
            SELECT EXISTS (
                SELECT 1 FROM product_images
                WHERE product_id = %s AND image_url = %s
            )
            """,
            (product_id, image_url),
        )
        if cursor.fetchone()[0]:
            continue
        cursor.execute(
            """
            INSERT INTO product_images (product_id, image_url, alt_text, display_order, is_primary)
            VALUES (
                %s, %s, %s, %s,
                NOT EXISTS (SELECT 1 FROM product_images WHERE product_id = %s AND is_primary)
            )
            """,
            (product_id, image_url, nonempty(row["name"])[:255], order, product_id),
        )

    for order, (spec_name, spec_value) in enumerate(specifications.items()):
        label = nonempty(str(spec_name))
        value = nonempty(str(spec_value))
        if not label or len(label) > 100 or not value:
            continue
        cursor.execute(
            """
            INSERT INTO product_specifications (product_id, spec_name, spec_value, spec_order)
            SELECT %s, %s, %s, %s
            WHERE NOT EXISTS (
                SELECT 1 FROM product_specifications
                WHERE product_id = %s AND spec_name = %s AND spec_value = %s
            )
            """,
            (product_id, label, value, order, product_id, label, value),
        )
    return True


def main():
    if not INPUT_FILE.is_file():
        raise FileNotFoundError(f"Không tìm thấy {INPUT_FILE}; hãy chạy clean_data.py trước.")

    conninfo = os.environ.get("DATABASE_URL")
    connect_args = (conninfo,) if conninfo else ()
    connect_kwargs = {} if conninfo else {"dbname": os.environ.get("PGDATABASE", "tgdd")}
    inserted = skipped = 0
    with psycopg.connect(*connect_args, **connect_kwargs) as connection:
        with connection.cursor() as cursor:
            check_schema(cursor)
            with INPUT_FILE.open("r", encoding="utf-8-sig", newline="") as csv_file:
                for row in csv.DictReader(csv_file):
                    if import_row(cursor, row):
                        inserted += 1
                    else:
                        skipped += 1
    print(f"Đã import/cập nhật {inserted} sản phẩm; bỏ qua {skipped} dòng không hợp lệ.")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"Import catalog thất bại: {exc}", file=sys.stderr)
        raise
