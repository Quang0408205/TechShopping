#!/bin/bash
cd "$(dirname "$0")"
export PYTHONIOENCODING=utf-8
# Windows: "py -3"; macOS / Linux: python3 (hoặc đặt PY=... trước khi chạy)
PY=${PY:-$(command -v py >/dev/null 2>&1 && echo "py -3" || echo python3)}
for c in "Điện Thoại" "Laptop" "Phụ Kiện"; do
  $PY crawl_variants.py --category "$c" --limit 600
done
echo ALL_DONE
