"""Nạp catalogue TGDĐ (tgdd_products_cleaned.csv) vào database TechShopping.

Chạy được nhiều lần: mỗi sản phẩm được nhận diện bằng products.sku = "TGDD-<source_key>".
Lần chạy đầu trên database có catalogue seed cũ (877 sản phẩm, xem legacy_seed_products.csv):
  * sản phẩm cũ có cùng URL với một dòng CSV được cập nhật tại chỗ (giữ product_id / variant_id,
    nên đơn hàng, giỏ hàng, khuyến mãi, tồn kho đang tham chiếu vẫn đúng);
  * sản phẩm cũ không còn trong CSV bị ẩn (is_active = false), không xoá;
  * thương hiệu bị tách ("iPhone (Apple)", "MacBook"...) được gộp về tên chuẩn, thương hiệu
    không còn sản phẩm đang bán bị ẩn.
Điểm / số lượt đánh giá của TGDĐ ghi vào products.tgdd_rating / tgdd_review_count (chỉ để hiển thị);
products.rating / total_reviews là đánh giá thật trên web, importer không đụng tới
(cần migration database/migrations/product_tgdd_rating.sql với database cũ).
Mọi thứ chạy trong một transaction; --dry-run chạy hết rồi rollback để xem trước số liệu.
"""

import argparse
import csv
import hashlib
import json
import math
import os
import re
import sys
import unicodedata
from collections import Counter
from pathlib import Path
from urllib.parse import urlparse

import psycopg


INPUT_FILE = Path(__file__).with_name("tgdd_products_cleaned.csv")
LEGACY_FILE = Path(__file__).with_name("legacy_seed_products.csv")
SKU_PREFIX = "TGDD-"
LEGACY_SKU_PREFIX = "TGDD-OLD-"
# Trang chủ TGDĐ: crawler gán nhầm cho vài dòng lỗi, không dùng làm khoá đối chiếu.
INVALID_URLS = {"https://www.thegioididong.com", "https://www.thegioididong.com/"}

ATTRIBUTE_COLOR = "Màu sắc"
ATTRIBUTE_RAM = "RAM"
ATTRIBUTE_STORAGE = "Bộ nhớ trong"
DEFAULT_VARIANT_NAME = "Tiêu chuẩn"

# Tên thương hiệu cũ / tách nhỏ -> tên chuẩn (so khớp không phân biệt hoa thường).
BRAND_ALIASES = {
    "iphone (apple)": "Apple",
    "ipad (apple)": "Apple",
    "macbook": "Apple",
    "nothing phone": "Nothing",
}
# Từ đầu tên sản phẩm -> thương hiệu, khi CSV không có cột brand.
NAME_BRAND_HINTS = {
    "iphone": "Apple",
    "ipad": "Apple",
    "macbook": "Apple",
    "apple watch": "Apple",
    "galaxy": "Samsung",
    "redmi": "Xiaomi",
    "poco": "Xiaomi",
    "mi band": "Xiaomi",
    "nothing phone": "Nothing",
    "thunderobot": "Thunderobot",
}
FASHION_WATCH_CATEGORY = "Đồng Hồ Thời Trang"
# Crawler lấy cả bảng so sánh trong bài viết nằm sau bảng thông số ("Phiên bản", "iPhone 18 Pro Max 512GB":
# "Từ 48.499.000đ", ...). Ở các danh mục này, cắt từ ô tiêu đề bảng đầu tiên và phần đuôi sau khoá
# thông số phổ biến cuối cùng (ô tiêu đề bảng thì cắt ở mọi danh mục, vd bảng so sánh loa JBL).
SPEC_TRIM_CATEGORIES = {"Điện Thoại", "Máy Tính Bảng", "Laptop", "Đồng Hồ Thông Minh"}
SPEC_TABLE_HEADERS = {
    "phiên bản", "các phiên bản", "tiêu chí", "tên sản phẩm", "tên phiên bản sản phẩm", "thông số",
    "thuộc tính", "hạng mục", "tính năng / thông số", "danh mục", "model", "so sánh",
}
SPEC_COMMON_SHARE = 0.2
# Tiền tố loại sản phẩm đứng trước thương hiệu trong tên.
NAME_PREFIXES = (
    "điện thoại", "máy tính bảng", "laptop", "đồng hồ thông minh", "đồng hồ định vị trẻ em",
    "vòng đeo tay thông minh", "vòng tay thông minh", "đồng hồ", "tai nghe", "loa", "sạc dự phòng",
    "pin sạc dự phòng", "camera", "chuột", "bàn phím", "cáp", "adapter", "củ sạc", "máy đọc sách",
)

REQUIRED_COLUMNS = {
    "categories": {"category_id", "name", "slug", "is_active"},
    "brands": {"brand_id", "name", "slug", "is_active"},
    "products": {
        "product_id", "name", "slug", "sku", "description", "brand_id", "category_id",
        "base_price", "tgdd_rating", "tgdd_review_count", "is_active", "updated_at",
    },
    "product_variants": {
        "variant_id", "product_id", "variant_name", "sku_variant", "price",
        "color", "storage", "ram", "updated_at",
    },
    "product_images": {"product_id", "image_url", "alt_text", "display_order", "is_primary"},
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


def canonical_brand(name):
    name = nonempty(name)
    return BRAND_ALIASES.get(name.casefold(), name) if name else ""


def strip_type_prefix(name):
    """'Điện thoại Xiaomi Redmi 17' -> 'Xiaomi Redmi 17'."""
    rest = name.strip()
    lowered = rest.casefold()
    for prefix in NAME_PREFIXES:
        if lowered.startswith(prefix + " "):
            return rest[len(prefix) + 1:].strip()
    return rest


def caps_words(text):
    """Các từ viết hoa toàn bộ ở đầu tên: 'CITIZEN L 43 mm Nam' -> ['CITIZEN', 'L']."""
    words = []
    for word in text.split():
        if any(ch.isdigit() for ch in word) or word != word.upper() or not any(ch.isalpha() for ch in word):
            break
        words.append(word)
    return words


def collect_watch_brands(rows):
    """Thương hiệu đồng hồ thời trang lấy từ chính CSV: tên TGDĐ mở đầu bằng thương hiệu viết hoa
    ('CITIZEN 43 mm Nam ...', 'SMILE KID ...'). Bỏ tên dòng sản phẩm có thương hiệu ngắn hơn đứng
    trước ('CITIZEN L', 'TOMMY HILFIGER PIPPA'). Trả về {tên thường: tên hiển thị}."""
    prefixes = {}
    for row in rows:
        if nonempty(row.get("category_group")) == FASHION_WATCH_CATEGORY:
            words = caps_words(strip_type_prefix(nonempty(row.get("name"))))
            if words:
                prefixes.setdefault(" ".join(words).casefold(), " ".join(words))
    return {
        key: display for key, display in prefixes.items()
        if not any(" ".join(key.split()[:count]) in prefixes for count in range(1, len(key.split())))
    }


def infer_brand(name, category_name, url, known_brands, watch_brands):
    """Đoán thương hiệu khi CSV để trống cột brand; không đoán được -> ""."""
    rest = strip_type_prefix(name)
    lowered = rest.casefold()
    for hint, brand in NAME_BRAND_HINTS.items():
        if lowered == hint or lowered.startswith(hint + " "):
            return brand
    for brand in known_brands:  # dài trước, để "EDIFICE CASIO" thắng "EDIFICE"
        key = brand.casefold()
        if lowered == key or lowered.startswith(key + " "):
            return canonical_brand(brand)
    if category_name != FASHION_WATCH_CATEGORY:
        return ""
    # Ngắn trước: "CITIZEN L" -> CITIZEN, "FESTINA ON THE SQUARE" -> FESTINA, "Smile Kid SL088" -> SMILE KID.
    words = rest.split()
    for count in range(1, min(len(words), 4) + 1):
        brand = watch_brands.get(" ".join(words[:count]).casefold())
        if brand:
            return brand
    # Tên chỉ có mã ("NJ0238-57E"): thương hiệu nằm trong URL ".../dong-ho-citizen-automatic-40-mm-nam-nj0238-57e".
    url_slug = "-" + urlparse(url).path.rstrip("/").rsplit("/", 1)[-1] + "-"
    for brand in sorted(watch_brands.values(), key=len, reverse=True):  # dài trước: "edifice-casio" thắng "casio"
        if f"-{slugify(brand)}-" in url_slug:
            return brand
    return ""


def collect_spec_vocabulary(rows):
    """Khoá thông số có ở >= 20% sản phẩm cùng danh mục (bảng thông số chuẩn của TGDĐ)."""
    counts, totals = {}, Counter()
    for row in rows:
        category_name = nonempty(row.get("category_group"))
        if category_name in SPEC_TRIM_CATEGORIES:
            totals[category_name] += 1
            counts.setdefault(category_name, Counter()).update(parse_json(row.get("specifications"), dict).keys())
    return {
        category_name: {key for key, count in counter.items() if count >= SPEC_COMMON_SHARE * totals[category_name]}
        for category_name, counter in counts.items()
    }


def clean_specifications(specifications, vocabulary):
    """Bỏ bảng so sánh của bài viết; vocabulary = None: chỉ cắt theo ô tiêu đề bảng."""
    keys = list(specifications)
    for index, key in enumerate(keys):
        label = str(key).strip().casefold()
        if label in SPEC_TABLE_HEADERS or label.startswith("các phiên bản"):
            # Bảng đứng đầu (vd Apple Watch SE 3 chỉ có "Tiêu chí: Thông số chi tiết" + các dòng của nó)
            # là thông số thật của sản phẩm: chỉ bỏ dòng tiêu đề.
            if index == 0:
                return {key: specifications[key] for key in keys[1:]}
            keys = keys[:index]
            break
    if vocabulary is not None:
        last_common = max((index for index, key in enumerate(keys) if key in vocabulary), default=len(keys) - 1)
        keys = keys[:last_common + 1]
    return {key: specifications[key] for key in keys}


def size_text(value):
    """'8' / '8 GB' / '1024' / '1 TB' -> '8 GB' / '1 TB'; giá trị không phải dung lượng -> ''."""
    value = nonempty(value)
    if not value:
        return ""
    if value.isdigit():
        number, unit = int(value), "GB"
    else:
        match = re.match(r"^\s*(\d+(?:[.,]\d+)?)\s*(GB|TB|MB)\b", value, re.IGNORECASE)
        if not match:
            return ""
        number = float(match.group(1).replace(",", "."))
        number = int(number) if number.is_integer() else number
        unit = match.group(2).upper()
    if unit == "GB" and isinstance(number, int) and number >= 1024 and number % 1024 == 0:
        number, unit = number // 1024, "TB"
    return f"{number} {unit}"


def spec_value(specifications, labels):
    for label in labels:
        value = nonempty(str(specifications.get(label, "")))
        if value:
            return value
    return ""


def variant_details(row, specifications):
    color = nonempty(row.get("color"))
    if not color:
        # Thông số "Màu sắc" thường liệt kê mọi màu đang bán ("Đen, Bạc, Đỏ."): chỉ nhận khi có đúng 1 màu.
        listed = spec_value(specifications, ("Màu sắc", "Màu")).strip(" .")
        color = listed if listed and not re.search(r"[,;/]", listed) else ""
    ram = size_text(row.get("ram")) or size_text(spec_value(specifications, ("RAM", "Bộ nhớ RAM")))
    storage = size_text(row.get("rom")) or size_text(spec_value(
        specifications,
        ("Dung lượng lưu trữ", "Ổ cứng", "Bộ nhớ trong", "Bộ nhớ trong (ROM)", "Bộ nhớ", "Lưu trữ"),
    ))
    return color[:50], storage[:50], ram[:50]


def variant_name(color, storage, ram):
    """Cùng kiểu với catalogue cũ: '8GB/256GB - Tím', '256 GB - Đỏ', 'Bạc'."""
    if ram and storage:
        size = f"{ram.replace(' ', '')}/{storage.replace(' ', '')}"
    else:
        size = storage or ram
    if size and color:
        return f"{size} - {color}"
    return size or color or DEFAULT_VARIANT_NAME


def image_key(url):
    """Cùng một ảnh có thể khác host / kích thước / mã số: so theo tên file đã chuẩn hoá."""
    name = urlparse(url).path.rsplit("/", 1)[-1].lower()
    stem, _, ext = name.rpartition(".")
    stem = re.sub(r"-\d+x\d+$", "", stem or name)
    stem = re.sub(r"-\d{10,}$", "", stem)
    stem = re.sub(r"-fix$", "", stem)
    return f"{stem}.{ext}" if stem else name


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


def load_legacy():
    if not LEGACY_FILE.is_file():
        return {}
    with LEGACY_FILE.open("r", encoding="utf-8", newline="") as legacy_file:
        return {row["url"]: row for row in csv.DictReader(legacy_file) if row["url"] not in INVALID_URLS}


def rekey_legacy(cursor, legacy):
    """Đổi SKU seed cũ TGDD-<id> thành TGDD-OLD-<id> để không đụng SKU của dữ liệu mới.

    Chỉ đổi dòng còn đúng tên seed: một sản phẩm mới vô tình trùng số id (vd 199205) đã đổi tên
    nên không bị đụng ở lần chạy sau."""
    renamed = 0
    for row in legacy.values():
        cursor.execute(
            "UPDATE products SET sku = %s WHERE sku = %s AND name = %s RETURNING product_id",
            (LEGACY_SKU_PREFIX + row["legacy_id"], SKU_PREFIX + row["legacy_id"], row["name"]),
        )
        found = cursor.fetchone()
        if found:
            renamed += 1
            cursor.execute(
                "UPDATE product_variants SET sku_variant = %s WHERE product_id = %s AND sku_variant = %s",
                (f"{LEGACY_SKU_PREFIX}{row['legacy_id']}-V1", found[0], f"{SKU_PREFIX}{row['legacy_id']}-V1"),
            )
    return renamed


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


class BrandStore:
    """Tìm thương hiệu theo tên (không phân biệt hoa thường), chưa có thì tạo."""

    def __init__(self, cursor):
        self.cursor = cursor
        self.ids = {}
        cursor.execute("SELECT brand_id, name FROM brands")
        for brand_id, name in cursor.fetchall():
            self.ids.setdefault(name.casefold(), brand_id)

    def known_names(self, extra):
        names = {canonical_brand(n) for n in extra if n} | {n for n in BRAND_ALIASES}
        cursor = self.cursor
        cursor.execute("SELECT name FROM brands WHERE slug NOT LIKE 'e2e-%'")
        names |= {row[0] for row in cursor.fetchall()}
        return sorted(names, key=len, reverse=True)

    def resolve(self, name):
        if not name:
            return None
        name = name[:100]
        brand_id = self.ids.get(name.casefold())
        if brand_id is None:
            slug = slugify(name)[:100] or f"brand-{hashlib.sha256(name.encode()).hexdigest()[:12]}"
            self.cursor.execute(
                """
                INSERT INTO brands (name, slug)
                VALUES (%s, %s)
                ON CONFLICT (slug) DO UPDATE SET is_active = true
                RETURNING brand_id
                """,
                (name, slug),
            )
            brand_id = self.cursor.fetchone()[0]
            self.ids[name.casefold()] = brand_id
        return brand_id


def ensure_attribute_value(cursor, cache, category_id, attribute_name, value):
    attribute_id = cache.get(attribute_name)
    if attribute_id is None:
        cursor.execute(
            """
            INSERT INTO attributes (name)
            VALUES (%s)
            ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name
            RETURNING attribute_id
            """,
            (attribute_name,),
        )
        attribute_id = cache[attribute_name] = cursor.fetchone()[0]
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
    return attribute_id, value_id


def unique_slug(cursor, name, key):
    base = slugify(name)[:240] or "san-pham"
    cursor.execute("SELECT 1 FROM products WHERE slug = %s", (base,))
    return base if cursor.fetchone() is None else f"{base}-{slugify(key)}"[:255]


class Importer:
    def __init__(self, cursor, legacy, rows):
        self.cursor = cursor
        self.legacy = legacy
        self.brands = BrandStore(cursor)
        self.known_brands = self.brands.known_names(row.get("brand") for row in rows)
        self.watch_brands = collect_watch_brands(rows)
        self.spec_vocabulary = collect_spec_vocabulary(rows)  # chỉ có các danh mục trong SPEC_TRIM_CATEGORIES
        self.attribute_ids = {}
        self.claimed_legacy = set()
        self.stats = Counter()

    def find_product(self, sku, url):
        cursor = self.cursor
        cursor.execute(
            """
            SELECT p.product_id, b.name FROM products p LEFT JOIN brands b ON b.brand_id = p.brand_id
            WHERE p.sku = %s
            """,
            (sku,),
        )
        found = cursor.fetchone()
        if found:
            return found, "updated"
        legacy = self.legacy.get(url)
        if legacy and legacy["legacy_id"] not in self.claimed_legacy:
            cursor.execute(
                """
                SELECT p.product_id, b.name FROM products p LEFT JOIN brands b ON b.brand_id = p.brand_id
                WHERE p.sku = %s
                """,
                (LEGACY_SKU_PREFIX + legacy["legacy_id"],),
            )
            found = cursor.fetchone()
            if found:
                self.claimed_legacy.add(legacy["legacy_id"])
                return found, "legacy_matched"
        return None, "inserted"

    def import_row(self, row):
        cursor = self.cursor
        source_key = nonempty(row.get("source_key")) or nonempty(row.get("id"))
        url = nonempty(row.get("url"))
        name = nonempty(row.get("name"))[:255]
        category_name = nonempty(row.get("category_group"))
        if url in INVALID_URLS:
            self.stats["skipped_invalid_url"] += 1
            return
        if not source_key or not name or not category_name:
            self.stats["skipped_missing_fields"] += 1
            return
        try:
            price = float(row.get("price", ""))
        except (TypeError, ValueError):
            price = -1
        if not math.isfinite(price) or price <= 0 or price >= 10**13:
            self.stats["skipped_price"] += 1
            return
        price = int(price)

        sku = f"{SKU_PREFIX}{source_key}"[:50]
        found, outcome = self.find_product(sku, url)
        category_id = upsert_category(cursor, category_name)
        brand_name = canonical_brand(row.get("brand"))
        if not brand_name:
            brand_name = infer_brand(name, category_name, url, self.known_brands, self.watch_brands)
            if brand_name:
                self.stats["brand_inferred"] += 1
        if not brand_name and found:
            brand_name = canonical_brand(found[1])
        if not brand_name:
            self.stats["brand_missing"] += 1
        brand_id = self.brands.resolve(brand_name)
        rating = optional_number(row.get("rating"))
        review_count = optional_number(row.get("review_count"), integer=True)
        description = nonempty(row.get("description")) or None

        if found:
            product_id = found[0]
            cursor.execute(
                """
                UPDATE products SET
                    name = %s, sku = %s,
                    description = COALESCE(%s, description),
                    brand_id = %s, category_id = %s, base_price = %s,
                    tgdd_rating = COALESCE(%s, tgdd_rating),
                    tgdd_review_count = COALESCE(%s, tgdd_review_count),
                    updated_at = current_timestamp
                WHERE product_id = %s
                """,
                (name, sku, description, brand_id, category_id, price, rating, review_count, product_id),
            )
        else:
            cursor.execute(
                """
                INSERT INTO products
                    (name, slug, sku, description, brand_id, category_id, base_price,
                     tgdd_rating, tgdd_review_count)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, COALESCE(%s, 0))
                RETURNING product_id
                """,
                (name, unique_slug(cursor, name, source_key), sku, description, brand_id,
                 category_id, price, rating, review_count),
            )
            product_id = cursor.fetchone()[0]
        self.stats[outcome] += 1

        specifications = clean_specifications(
            parse_json(row.get("specifications"), dict), self.spec_vocabulary.get(category_name)
        )
        self.save_variant(product_id, category_id, sku, price, row, specifications)
        self.save_images(product_id, name, row)
        self.save_specifications(product_id, specifications)

    def save_variant(self, product_id, category_id, sku, price, row, specifications):
        cursor = self.cursor
        color, storage, ram = variant_details(row, specifications)
        cursor.execute(
            """
            SELECT variant_id, color, storage, ram FROM product_variants
            WHERE product_id = %s ORDER BY variant_id LIMIT 1
            """,
            (product_id,),
        )
        existing = cursor.fetchone()
        if existing:
            variant_id = existing[0]
            # CSV mới thiếu màu ở hầu hết dòng: giữ giá trị cũ khi CSV để trống.
            color, storage, ram = color or existing[1] or "", storage or existing[2] or "", ram or existing[3] or ""
            cursor.execute(
                """
                UPDATE product_variants SET
                    variant_name = %s, sku_variant = %s, price = %s,
                    color = %s, storage = %s, ram = %s, updated_at = current_timestamp
                WHERE variant_id = %s
                """,
                (variant_name(color, storage, ram), f"{sku}-V1"[:50], price,
                 color or None, storage or None, ram or None, variant_id),
            )
        else:
            cursor.execute(
                """
                INSERT INTO product_variants (product_id, variant_name, sku_variant, price, color, storage, ram)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
                RETURNING variant_id
                """,
                (product_id, variant_name(color, storage, ram), f"{sku}-V1"[:50], price,
                 color or None, storage or None, ram or None),
            )
            variant_id = cursor.fetchone()[0]

        attribute_ids, value_ids = [], []
        for attribute_name, value in ((ATTRIBUTE_COLOR, color), (ATTRIBUTE_RAM, ram), (ATTRIBUTE_STORAGE, storage)):
            if value:
                attribute_id, value_id = ensure_attribute_value(
                    cursor, self.attribute_ids, category_id, attribute_name, value
                )
                attribute_ids.append(attribute_id)
                value_ids.append(value_id)
        cursor.execute(
            """
            DELETE FROM variant_attribute_values vav
            USING attribute_values av, attributes a
            WHERE vav.variant_id = %s AND av.value_id = vav.value_id AND a.attribute_id = av.attribute_id
              AND a.name IN (%s, %s, %s) AND NOT (vav.value_id = ANY(%s::bigint[]))
            """,
            (variant_id, ATTRIBUTE_COLOR, ATTRIBUTE_RAM, ATTRIBUTE_STORAGE, value_ids),
        )
        for value_id in value_ids:
            cursor.execute(
                """
                INSERT INTO variant_attribute_values (variant_id, value_id)
                VALUES (%s, %s)
                ON CONFLICT (variant_id, value_id) DO NOTHING
                """,
                (variant_id, value_id),
            )

    def save_images(self, product_id, name, row):
        cursor = self.cursor
        images = parse_json(row.get("images"), list)
        listing_image = nonempty(row.get("image"))
        if listing_image:
            images.insert(0, listing_image)
        cursor.execute(
            """
            SELECT image_url, COALESCE(display_order, 0), COALESCE(is_primary, false)
            FROM product_images WHERE product_id = %s
            """,
            (product_id,),
        )
        existing = cursor.fetchall()
        seen = {image_key(url) for url, _, _ in existing}
        next_order = max((order for _, order, _ in existing), default=-1) + 1
        has_primary = any(primary for _, _, primary in existing)
        for image_url in images:
            if not isinstance(image_url, str) or urlparse(image_url).scheme not in {"http", "https"}:
                continue
            key = image_key(image_url)
            if key in seen:
                continue
            seen.add(key)
            cursor.execute(
                """
                INSERT INTO product_images (product_id, image_url, alt_text, display_order, is_primary)
                VALUES (%s, %s, %s, %s, %s)
                """,
                (product_id, image_url, name, next_order, not has_primary),
            )
            has_primary = True
            next_order += 1
            self.stats["images_added"] += 1

    def save_specifications(self, product_id, specifications):
        rows = []
        for spec_name, spec_value_raw in specifications.items():
            label = nonempty(str(spec_name))
            value = nonempty(str(spec_value_raw))
            if label and len(label) <= 100 and value:
                rows.append((label, value))
        if not rows:
            return
        cursor = self.cursor
        cursor.execute("DELETE FROM product_specifications WHERE product_id = %s", (product_id,))
        for order, (label, value) in enumerate(rows):
            cursor.execute(
                """
                INSERT INTO product_specifications (product_id, spec_name, spec_value, spec_order)
                VALUES (%s, %s, %s, %s)
                """,
                (product_id, label, value, order),
            )
        self.stats["products_with_specs"] += 1

    def finish(self):
        cursor = self.cursor
        cursor.execute(
            """
            UPDATE products SET is_active = false, updated_at = current_timestamp
            WHERE sku LIKE %s AND is_active
            """,
            (LEGACY_SKU_PREFIX + "%",),
        )
        self.stats["legacy_hidden"] = cursor.rowcount
        cursor.execute(
            """
            UPDATE brands b SET is_active = false, updated_at = current_timestamp
            WHERE COALESCE(b.is_active, true) AND NOT EXISTS (
                SELECT 1 FROM products p
                WHERE p.brand_id = b.brand_id AND p.is_active AND p.deleted_at IS NULL
            )
            RETURNING name
            """
        )
        hidden = sorted(row[0] for row in cursor.fetchall())
        self.stats["brands_hidden"] = len(hidden)
        cursor.execute(
            """
            UPDATE brands b SET is_active = true, updated_at = current_timestamp
            WHERE b.is_active = false AND EXISTS (
                SELECT 1 FROM products p
                WHERE p.brand_id = b.brand_id AND p.is_active AND p.deleted_at IS NULL
            )
            """
        )
        return hidden


def main():
    # Console Windows mặc định không phải UTF-8: in tiếng Việt sẽ lỗi UnicodeEncodeError
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description="Import catalogue TGDĐ vào database TechShopping.")
    parser.add_argument("--dry-run", action="store_true", help="chạy hết rồi rollback, chỉ in số liệu")
    args = parser.parse_args()
    if not INPUT_FILE.is_file():
        raise FileNotFoundError(f"Không tìm thấy {INPUT_FILE}; hãy chạy clean_data.py trước.")

    with INPUT_FILE.open("r", encoding="utf-8-sig", newline="") as csv_file:
        rows = list(csv.DictReader(csv_file))
    legacy = load_legacy()

    conninfo = os.environ.get("DATABASE_URL")
    connect_args = (conninfo,) if conninfo else ()
    connect_kwargs = {} if conninfo else {"dbname": os.environ.get("PGDATABASE", "techshopping")}
    with psycopg.connect(*connect_args, **connect_kwargs) as connection:
        with connection.cursor() as cursor:
            check_schema(cursor)
            renamed = rekey_legacy(cursor, legacy)
            importer = Importer(cursor, legacy, rows)
            for row in rows:
                importer.import_row(row)
            hidden_brands = importer.finish()
        if args.dry_run:
            connection.rollback()

    stats = importer.stats
    print(f"{'[DRY RUN, đã rollback] ' if args.dry_run else ''}Đọc {len(rows)} dòng CSV.")
    print(f"  Sản phẩm seed cũ đổi SKU sang {LEGACY_SKU_PREFIX}<id>: {renamed}")
    print(f"  Cập nhật sản phẩm cũ trùng URL: {stats['legacy_matched']}")
    print(f"  Cập nhật sản phẩm đã import trước đó: {stats['updated']}")
    print(f"  Thêm sản phẩm mới: {stats['inserted']}")
    print(f"  Ẩn sản phẩm cũ không còn trong CSV: {stats['legacy_hidden']}")
    print(f"  Bỏ qua: URL lỗi {stats['skipped_invalid_url']}, thiếu dữ liệu {stats['skipped_missing_fields']}, "
          f"giá không hợp lệ {stats['skipped_price']}")
    print(f"  Thương hiệu suy từ tên: {stats['brand_inferred']}; không xác định được: {stats['brand_missing']}")
    print(f"  Ảnh thêm mới: {stats['images_added']}; sản phẩm ghi lại thông số: {stats['products_with_specs']}")
    print(f"  Thương hiệu bị ẩn vì không còn sản phẩm đang bán ({stats['brands_hidden']}): "
          f"{', '.join(hidden_brands) or '-'}")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"Import catalog thất bại: {exc}", file=sys.stderr)
        raise
