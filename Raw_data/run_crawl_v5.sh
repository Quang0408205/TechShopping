#!/bin/bash
cd "$(dirname "$0")"
export PYTHONIOENCODING=utf-8
# Windows: "py -3"; macOS / Linux: python3 (hoặc đặt PY=... trước khi chạy)
PY=${PY:-$(command -v py >/dev/null 2>&1 && echo "py -3" || echo python3)}
for c in "Màn Hình Máy Tính" "Máy Tính Để Bàn" "Máy Tính Bảng" "Đồng Hồ Thông Minh" "Đồng Hồ Thời Trang"; do
  $PY crawl_variants.py --category "$c" --limit 100
done
echo ALL_DONE
