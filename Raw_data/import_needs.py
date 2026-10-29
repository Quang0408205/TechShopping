"""Nạp "nhu cầu" đã cào (crawl_v5/needs.json) vào database: bảng needs + product_needs.

Chỉ thêm + cập nhật (không xoá nhu cầu / liên kết cũ). Sản phẩm nhận ra bằng cùng cách của import_variants.py
(URL trang sản phẩm / mã phiên bản trong crawl_v5/*.jsonl). --dry-run chạy hết rồi rollback.
"""
import argparse
import csv
import json
import os
import sys
from collections import Counter
from pathlib import Path

import psycopg

import import_catalog as ic
import import_variants as iv

NEEDS_FILE = iv.CRAWL_DIR / "needs.json"


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    needs = json.loads(NEEDS_FILE.read_text(encoding="utf-8"))

    by_path = {}
    for file in sorted(iv.CRAWL_DIR.glob("*.jsonl")):
        for line in file.read_text(encoding="utf-8").splitlines():
            if line.strip():
                record = json.loads(line)
                by_path[iv.path_of(record["url"])] = record
                for combo in record["combos"]:
                    by_path.setdefault(iv.path_of(combo["url"]), record)

    stats = Counter()
    conninfo = os.environ.get("DATABASE_URL")
    with psycopg.connect(*((conninfo,) if conninfo else ()),
                         **({} if conninfo else {"dbname": os.environ.get("PGDATABASE", "techshopping")})) as connection:
        with connection.cursor() as cursor:
            importer = iv.VariantImporter(cursor, [])
            order = Counter()
            for need in needs:
                cursor.execute("SELECT category_id FROM categories WHERE name = %s", (need["category"],))
                row = cursor.fetchone()
                if not row:
                    stats["category_missing"] += 1
                    continue
                category_id = row[0]
                order[category_id] += 1
                cursor.execute(
                    """
                    INSERT INTO needs (category_id, slug, name, display_order) VALUES (%s, %s, %s, %s)
                    ON CONFLICT (category_id, slug) DO UPDATE SET name = EXCLUDED.name, display_order = EXCLUDED.display_order
                    RETURNING need_id
                    """,
                    (category_id, need["slug"], need["name"], order[category_id]),
                )
                need_id = cursor.fetchone()[0]
                stats["needs"] += 1
                for url in need["urls"]:
                    record = by_path.get(iv.path_of(url))
                    if record is None:
                        stats["url_not_crawled"] += 1
                        continue
                    ids, _ = importer.find_product(record)
                    if not ids:
                        stats["product_not_in_db"] += 1
                        continue
                    cursor.execute(
                        "SELECT 1 FROM products WHERE product_id = %s AND category_id = %s", (ids[0], category_id))
                    if not cursor.fetchone():
                        stats["other_category"] += 1
                        continue
                    cursor.execute(
                        "INSERT INTO product_needs (product_id, need_id) VALUES (%s, %s) ON CONFLICT DO NOTHING",
                        (ids[0], need_id),
                    )
                    stats["links_added" if cursor.rowcount else "links_existing"] += 1
        if args.dry_run:
            connection.rollback()
    print(f"{'[DRY RUN, đã rollback] ' if args.dry_run else ''}Nhu cầu: {stats['needs']}; liên kết thêm {stats['links_added']}, "
          f"đã có {stats['links_existing']}; URL chưa cào {stats['url_not_crawled']}, sản phẩm không có trong DB {stats['product_not_in_db']}, "
          f"khác danh mục {stats['other_category']}, thiếu danh mục {stats['category_missing']}")


if __name__ == "__main__":
    main()
