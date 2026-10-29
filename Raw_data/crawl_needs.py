"""Cào "nhu cầu" của TGDĐ: mỗi nhu cầu là một trang danh sách (vd /dtdd-choi-game); lấy URL các sản phẩm thuộc trang đó.

Ghi Raw_data/crawl_v5/needs.json: [{category, slug, name, urls: [...]}]. Nhập vào DB bằng import_needs.py.
Dùng: py -3 crawl_needs.py
"""
import json
import re
import time
from pathlib import Path

from playwright.sync_api import sync_playwright

import crawl_variants as cv

OUT = Path(__file__).with_name("crawl_v5") / "needs.json"
BASE = "https://www.thegioididong.com"

# (danh mục trong DB, slug nhu cầu, tên hiển thị, đường dẫn trang TGDĐ)
NEEDS = [
    ("Điện Thoại", "choi-game", "Chơi game / Cấu hình cao", "/dtdd-choi-game"),
    ("Điện Thoại", "chup-anh-quay-phim", "Chụp ảnh, quay phim", "/dtdd-chup-anh-quay-phim"),
    ("Điện Thoại", "pin-khung", "Pin khủng", "/dtdd-pin-khung"),
    ("Điện Thoại", "livestream", "Livestream", "/dtdd-livestream"),
    ("Điện Thoại", "mong-nhe", "Mỏng nhẹ", "/dtdd-mong-nhe"),
    ("Điện Thoại", "pho-thong", "Phổ thông", "/dtdd-pho-thong"),
    ("Máy Tính Bảng", "choi-game", "Chơi game / Cấu hình cao", "/may-tinh-bang-choi-game"),
    ("Máy Tính Bảng", "tre-em", "Cho trẻ em", "/may-tinh-bang-che-do-cho-tre-em"),
    ("Laptop", "gaming", "Gaming", "/laptop?g=laptop-gaming"),
    ("Laptop", "hoc-tap-van-phong", "Học tập, văn phòng", "/laptop?g=hoc-tap-van-phong"),
    ("Laptop", "do-hoa", "Đồ hoạ", "/laptop?g=do-hoa"),
    ("Laptop", "mong-nhe", "Mỏng nhẹ", "/laptop?g=mong-nhe-thoi-trang"),
    ("Laptop", "cao-cap", "Cao cấp", "/laptop?g=cao-cap"),
    ("Màn Hình Máy Tính", "van-phong", "Văn phòng", "/man-hinh-may-tinh-van-phong"),
    ("Màn Hình Máy Tính", "gaming", "Gaming", "/man-hinh-may-tinh-gaming"),
    ("Màn Hình Máy Tính", "do-hoa", "Đồ hoạ", "/man-hinh-may-tinh-do-hoa"),
    ("Màn Hình Máy Tính", "di-dong", "Màn hình di động", "/man-hinh-may-tinh-di-dong"),
    ("Máy Tính Để Bàn", "hoc-tap-van-phong", "Học tập, văn phòng", "/may-tinh-de-ban-hoc-tap-van-phong"),
    ("Máy Tính Để Bàn", "do-hoa", "Đồ hoạ, kỹ thuật", "/may-tinh-de-ban-do-hoa-ky-thuat"),
    ("Máy Tính Để Bàn", "gaming", "Gaming", "/may-tinh-de-ban-pc-gaming"),
    ("Máy Tính Để Bàn", "ky-thuat", "Kỹ thuật", "/may-tinh-de-ban-ky-thuat"),
]


def main():
    result = []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(user_agent=cv.UA, viewport={"width": 1400, "height": 1000})
        for category, slug, name, path in NEEDS:
            try:
                cards = cv.listing_cards(page, BASE + path, 3000)
            except Exception as exc:
                print(f"   [-] bỏ qua {path}: {exc}", flush=True)
                cards = []
            urls = [c["url"].split("?")[0] for c in cards]
            result.append({"category": category, "slug": slug, "name": name, "path": path, "urls": urls})
            print(f"[{category}] {name}: {len(urls)} sản phẩm", flush=True)
            OUT.write_text(json.dumps(result, ensure_ascii=False, indent=1), encoding="utf-8")
            time.sleep(2)
        browser.close()


if __name__ == "__main__":
    main()
