#!/bin/bash
cd "$(dirname "$0")"
export PYTHONIOENCODING=utf-8
for c in "Màn Hình Máy Tính" "Máy Tính Để Bàn" "Máy Tính Bảng" "Đồng Hồ Thông Minh" "Đồng Hồ Thời Trang"; do
  py -3 crawl_variants.py --category "$c" --limit 100
done
echo ALL_DONE
