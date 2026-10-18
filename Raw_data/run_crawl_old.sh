#!/bin/bash
cd "$(dirname "$0")"
export PYTHONIOENCODING=utf-8
for c in "Điện Thoại" "Laptop" "Phụ Kiện"; do
  py -3 crawl_variants.py --category "$c" --limit 600
done
echo ALL_DONE
