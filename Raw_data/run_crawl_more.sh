#!/bin/bash
cd "$(dirname "$0")"
export PYTHONIOENCODING=utf-8
py -3 crawl_variants.py --category "Phụ Kiện" --per-listing 30
for c in "Màn Hình Máy Tính" "Máy Tính Để Bàn" "Máy Tính Bảng" "Đồng Hồ Thông Minh"; do
  py -3 crawl_variants.py --category "$c" --limit 1000
done
py -3 crawl_variants.py --category "Đồng Hồ Thời Trang" --limit 500
echo ALL_DONE
