#!/bin/bash
cd "$(dirname "$0")"
export PYTHONIOENCODING=utf-8
# Windows: "py -3"; macOS / Linux: python3 (hoặc đặt PY=... trước khi chạy)
PY=${PY:-$(command -v py >/dev/null 2>&1 && echo "py -3" || echo python3)}
$PY crawl_variants.py --category "Phụ Kiện" --per-listing 30
for c in "Màn Hình Máy Tính" "Máy Tính Để Bàn" "Máy Tính Bảng" "Đồng Hồ Thông Minh"; do
  $PY crawl_variants.py --category "$c" --limit 1000
done
$PY crawl_variants.py --category "Đồng Hồ Thời Trang" --limit 500
echo ALL_DONE
