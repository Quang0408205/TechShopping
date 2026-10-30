"""Cào TGDĐ theo từng sản phẩm: đủ phiên bản × màu, giá, tình trạng và bộ ảnh riêng của từng màu.

Mỗi dòng JSONL = 1 sản phẩm (đã gộp các phiên bản cùng mẫu):
  {category_group, name, brand, url, rating, sold_qty, description, specifications, images (ảnh chung),
   combos: [{code, url, labels: {"Phiên bản": "256GB", "Màu": "Tím"}, price, available, images: [...]}]}

Dùng: py -3 crawl_variants.py --category "Máy Tính Bảng" [--limit 100] [--only-url URL]
      py -3 crawl_variants.py --category "Điện Thoại" --fix-images   (chỉ bổ sung ảnh đúng màu cho file đã cào)
"""
import argparse
import json
import re
import time
from pathlib import Path
from urllib.parse import urljoin

from playwright.sync_api import sync_playwright

import crawler

OUT_DIR = Path(__file__).with_name("crawl_v5")
LISTINGS = {
    "Điện Thoại": ["https://www.thegioididong.com/dtdd"],
    "Laptop": ["https://www.thegioididong.com/laptop"],
    "Phụ Kiện": [
        "https://www.thegioididong.com/sac-dtdd",
        "https://www.thegioididong.com/sac-cap",
        "https://www.thegioididong.com/op-lung-flipcover",
        "https://www.thegioididong.com/mieng-dan-man-hinh",
        "https://www.thegioididong.com/ong-kinh-tele",
        "https://www.thegioididong.com/quat-mini",
        "https://www.thegioididong.com/hub-chuyen-doi",
        "https://www.thegioididong.com/chuot-may-tinh",
        "https://www.thegioididong.com/ban-phim",
        "https://www.thegioididong.com/thiet-bi-mang",
        "https://www.thegioididong.com/tui-chong-soc",
        "https://www.thegioididong.com/tui-dung-phu-kien",
        "https://www.thegioididong.com/mieng-phu-ban-phim",
        "https://www.thegioididong.com/gia-treo-man-hinh",
        "https://www.thegioididong.com/mieng-lot-chuot",
        "https://www.thegioididong.com/bang-ve-dien-tu",
        "https://www.thegioididong.com/thiet-bi-am-thanh",
        "https://www.thegioididong.com/tai-nghe",
        "https://www.thegioididong.com/loa-laptop",
        "https://www.thegioididong.com/micro-cac-loai",
        "https://www.thegioididong.com/may-chieu",
        "https://www.thegioididong.com/mat-kinh",
        "https://www.thegioididong.com/o-cung-di-dong",
        "https://www.thegioididong.com/the-nho",
        "https://www.thegioididong.com/usb",
        "https://www.thegioididong.com/thiet-bi-nha-thong-minh",
        "https://www.thegioididong.com/camera-giam-sat",
        "https://www.thegioididong.com/chuong-cua-thong-minh",
        "https://www.thegioididong.com/den-nang-luong-mat-troi",
        "https://www.thegioididong.com/may-anh",
        "https://www.thegioididong.com/camera-hanh-trinh-hanh-dong",
        "https://www.thegioididong.com/flycam",
        "https://www.thegioididong.com/gay-tu-suong",
        "https://www.thegioididong.com/phu-kien-gaming",
        "https://www.thegioididong.com/ghe-gaming-ghe-cong-thai-hoc",
        "https://www.thegioididong.com/phu-kien-khac",
        "https://www.thegioididong.com/robot-thong-minh",
        "https://www.thegioididong.com/airtag",
        "https://www.thegioididong.com/phu-kien-oto",
        "https://www.thegioididong.com/den-thong-minh",
        "https://www.thegioididong.com/pin",
        "https://www.thegioididong.com/day-dong-ho",
        "https://www.thegioididong.com/quat-suoi",
        "https://www.thegioididong.com/do-choi-dien-thoai",
    ],
    "Màn Hình Máy Tính": ["https://www.thegioididong.com/man-hinh-may-tinh"],
    "Máy Tính Để Bàn": ["https://www.thegioididong.com/may-tinh-de-ban"],
    "Máy Tính Bảng": [
        "https://www.thegioididong.com/may-tinh-bang-apple-ipad",
        "https://www.thegioididong.com/may-tinh-bang",
    ],
    "Đồng Hồ Thông Minh": ["https://www.thegioididong.com/dong-ho-thong-minh"],
    "Đồng Hồ Thời Trang": ["https://www.thegioididong.com/dong-ho-deo-tay"],
}
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/120.0 Safari/537.36")
SOLD_OUT = re.compile(r"Tạm hết hàng|Hết hàng|Ngừng kinh doanh|Ngưng kinh doanh", re.I)
IMG_RE = re.compile(r"/Products/Images/\d+/\d+/|/pim/cdn/images/")


def listing_cards(page, url, want):
    """Mở danh mục, bấm 'Xem thêm' tới khi đủ `want` thẻ; trả về danh sách thẻ (giữ lại sau mỗi lần bấm)."""
    page.goto(url, timeout=60000, wait_until="domcontentloaded")
    page.wait_for_function("sel => document.querySelectorAll(sel).length > 0",
                           arg=crawler.PRODUCT_CARD_SELECTOR, timeout=30000)
    page.wait_for_timeout(3000)
    found = {}

    def harvest():
        for card in page.query_selector_all(crawler.PRODUCT_CARD_SELECTOR):
            item = crawler.extract_listing_product(card, {"name": ""}, True)
            if item and item.get("url"):
                found.setdefault(item["url"].split("?")[0], item)

    harvest()
    while len(found) < want:
        button = page.locator("button").filter(has_text=re.compile(r"^Xem thêm\s+\d+")).first
        if not button.count() or not button.is_visible():
            break
        before = crawler.count_cards(page)
        try:
            button.click()
            page.wait_for_function("([s, n]) => document.querySelectorAll(s).length > n",
                                   arg=[crawler.PRODUCT_CARD_SELECTOR, before], timeout=25000)
            page.wait_for_timeout(2500)
        except Exception as exc:
            print(f"   [!] không tải thêm được: {exc}")
            break
        harvest()
    return list(found.values())


def embedded_text(page):
    return page.content().replace('\\"', '"')


def parse_filters(html):
    """Đọc ma trận phiên bản × màu từ JSON nhúng: trả về (filters, {productCode: url})."""
    filters = []
    i = html.find('"filter":[{"label"')
    if i >= 0:
        try:
            filters, _ = json.JSONDecoder().raw_decode(html[i + len('"filter":'):])
        except ValueError:
            filters = []
    code_url = {}
    j = html.find('ProductCode":[{"productCode"')
    if j >= 0:
        try:
            arr, _ = json.JSONDecoder().raw_decode(html[html.find("[", j):])
            code_url = {x["productCode"]: x["url"] for x in arr if x.get("productCode")}
        except ValueError:
            pass
    return filters, code_url


def current_code(html):
    m = re.search(r'"productCode":"(\d+)","variantCode"', html)
    return m.group(1) if m else None


def slides(page):
    """Ảnh trong khung ảnh chính (đã theo màu đang chọn), theo thứ tự."""
    urls = page.evaluate("""() => [...document.querySelectorAll('div[class*="flex-[0_0_100%]"] img')]
        .map(i => i.currentSrc || i.src).filter(Boolean)""")
    return list(dict.fromkeys(u for u in urls if IMG_RE.search(u)))


def color_images(page):
    """{màu (casefold): [ảnh]} từ JSON-LD ProductGroup.hasVariant: ảnh chính đúng màu của từng phiên bản.
    Khung ảnh của một số trang (vd iPhone) mở đầu bằng ảnh bìa video giống nhau cho mọi màu, nên slides() không
    phân biệt được màu; JSON-LD thì luôn có ảnh riêng của màu."""
    found = {}
    for raw in page.eval_on_selector_all('script[type="application/ld+json"]', "els => els.map(e => e.textContent)"):
        try:
            data = json.loads(raw)
        except ValueError:
            continue
        for item in data if isinstance(data, list) else [data]:
            variants = item.get("hasVariant") if isinstance(item, dict) else None
            for variant in variants or []:
                color = str(variant.get("color") or "").strip()
                images = variant.get("image") or []
                images = images if isinstance(images, list) else [images]
                urls = [i.get("url") if isinstance(i, dict) else i for i in images]
                for url in urls:
                    if isinstance(url, str) and IMG_RE.search(url) and color:
                        found.setdefault(color.casefold(), [])
                        if url not in found[color.casefold()]:
                            found[color.casefold()].append(url)
    return found


def with_color_images(combos, by_color):
    """Đặt ảnh đúng màu (JSON-LD) lên đầu ảnh của từng tổ hợp; trả về số tổ hợp được thêm ảnh."""
    changed = 0
    for combo in combos:
        color = next((v for k, v in combo["labels"].items() if "màu" in k.lower()), "")
        extra = [u for u in by_color.get(color.casefold(), []) if u not in combo["images"]]
        if extra:
            combo["images"] = extra + combo["images"]
            changed += 1
    return changed


def missing_color_images(record):
    """Sản phẩm nhiều màu mà có màu chưa có ảnh riêng (mọi ảnh của nó đều xuất hiện ở các màu khác)."""
    per_color = {}
    for combo in record["combos"]:
        color = next((v for k, v in combo["labels"].items() if "màu" in k.lower()), "")
        if color:
            per_color.setdefault(color.casefold(), set()).update(combo["images"])
    if len(per_color) < 2:
        return False
    shared = set.intersection(*per_color.values())
    return any(not (urls - shared) for urls in per_color.values())


def goto_code(page, url, code, retries=3):
    target = f"{url}?code={code}" if code else url
    for attempt in range(retries):
        try:
            page.goto(target, timeout=60000, wait_until="load")
            page.wait_for_timeout(1200)
            return True
        except Exception as exc:
            print(f"   [!] lỗi tải {target} ({attempt + 1}/{retries}): {exc}")
            time.sleep(2)
    return False


def label_values(filters):
    """{productCode: {label: value}} và {(label, value): price}."""
    attrs, prices = {}, {}
    for f in filters:
        for v in f.get("values", []):
            for code in v.get("productCodes", []):
                attrs.setdefault(code, {})[f["label"]] = v["name"]
            if v.get("price"):
                prices[(f["label"], v["name"])] = v["price"]
    return attrs, prices


def crawl_product(page, item):
    url = item["url"].split("?")[0]
    if not goto_code(page, url, None):
        return None
    html = embedded_text(page)
    filters, code_url = parse_filters(html)
    by_color = color_images(page)
    details = crawler.extract_product_details(page)
    first_code = current_code(html)
    attrs, prices = label_values(filters)
    codes = list(attrs) or ([first_code] if first_code else [])
    combos = []
    loaded = {}
    for code in codes:
        cu = urljoin(crawler.BASE_URL, "/" + code_url[code]) if code in code_url else url
        cu = cu.split("?")[0]
        if code == first_code and cu == url:
            ok = True
        else:
            ok = goto_code(page, cu, code)
        if not ok:
            continue
        if code != first_code or cu != url:
            html_c = embedded_text(page)
        else:
            html_c = html
        text = page.evaluate("() => document.body.innerText")
        labels = attrs.get(code, {})
        price = max((p for (lab, val), p in prices.items() if labels.get(lab) == val), default=0)
        if not price:
            price = item.get("price") or 0
        combos.append({
            "code": code, "url": cu, "labels": labels, "price": price,
            "available": not SOLD_OUT.search(text.split("Thông số kỹ thuật")[0][:6000]),
            "images": slides(page),
        })
        loaded[code] = current_code(html_c)
        time.sleep(crawler.DELAY_BETWEEN_DETAILS * 0.5)
    with_color_images(combos, by_color)
    return {
        "category_group": item["category_group"], "name": item["name"], "brand": item.get("brand"),
        "url": url, "rating": item.get("rating"), "sold_qty": item.get("sold_qty"),
        "source_sku": first_code or item.get("source_sku"),
        "description": details["description"], "specifications": details["specifications"],
        "images": details["images"], "review_count": details["review_count"],
        "filters": [{"label": f["label"], "values": [v["name"] for v in f["values"]]} for f in filters],
        "combos": combos,
    }


def fix_images(out_file):
    """Bổ sung ảnh đúng màu cho các sản phẩm trong out_file mà có màu chưa có ảnh riêng; ghi đè file (qua file tạm)."""
    records = [json.loads(line) for line in out_file.read_text(encoding="utf-8").splitlines() if line.strip()]
    todo = [r for r in records if missing_color_images(r)]
    print(f"{out_file.name}: {len(todo)}/{len(records)} sản phẩm thiếu ảnh riêng của màu", flush=True)
    fixed = 0
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(user_agent=UA, viewport={"width": 1400, "height": 1000})
        for index, record in enumerate(todo, 1):
            if not goto_code(page, record["url"], None):
                continue
            changed = with_color_images(record["combos"], color_images(page))
            fixed += bool(changed)
            print(f"   [{index}/{len(todo)}] {record['name']}: thêm ảnh cho {changed} tổ hợp"
                  f"{'' if not missing_color_images(record) else ' (vẫn thiếu)'}", flush=True)
            time.sleep(crawler.DELAY_BETWEEN_DETAILS)
        browser.close()
    temp = out_file.with_suffix(".jsonl.tmp")
    temp.write_text("".join(json.dumps(r, ensure_ascii=False) + "\n" for r in records), encoding="utf-8")
    temp.replace(out_file)
    print(f"Đã bổ sung ảnh cho {fixed} sản phẩm; ghi lại {out_file}", flush=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--category", required=True, choices=list(LISTINGS))
    ap.add_argument("--limit", type=int, default=100, help="số sản phẩm (đã gộp phiên bản) tối đa")
    ap.add_argument("--only-url")
    ap.add_argument("--per-listing", type=int, default=0, help="tối đa N sản phẩm mỗi trang nhóm (bỏ qua --limit)")
    ap.add_argument("--fix-images", action="store_true",
                    help="không cào mới: mở lại 1 trang mỗi sản phẩm đã cào còn thiếu ảnh riêng của màu, bổ sung từ JSON-LD")
    args = ap.parse_args()

    OUT_DIR.mkdir(exist_ok=True)
    out_file = OUT_DIR / (re.sub(r"\W+", "_", args.category) + ".jsonl")
    if args.fix_images:
        fix_images(out_file)
        return
    done = set()
    if out_file.exists():
        for line in out_file.read_text(encoding="utf-8").splitlines():
            rec = json.loads(line)
            done.add(rec["url"])
            done.update(c["url"] for c in rec["combos"])
    count = len(done and [1 for _ in out_file.read_text(encoding="utf-8").splitlines()])

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(user_agent=UA, viewport={"width": 1400, "height": 1000})
        groups = []
        for listing in LISTINGS[args.category]:
            per = args.per_listing or (args.limit * 2 if len(LISTINGS[args.category]) == 1 else args.limit)
            try:
                got = listing_cards(page, listing, per)
            except Exception as exc:
                print(f"   [-] bỏ qua trang {listing}: {exc}", flush=True)
                continue
            print(f"[{args.category}] {listing}: {len(got)} thẻ", flush=True)
            for g in got:
                g["category_group"] = args.category
            groups.append(got)
        seen = set()
        for cards in groups:
            if args.only_url:
                cards = [c for c in cards if args.only_url in c["url"]]
            taken = 0
            for card in cards:
                if (args.per_listing and taken >= args.per_listing) or (not args.per_listing and count >= args.limit):
                    break
                u = card["url"].split("?")[0]
                if u in done or u in seen:
                    continue
                started = time.time()
                rec = crawl_product(page, card)
                if not rec or not rec["combos"]:
                    print(f"   [-] bỏ qua {u}", flush=True)
                    seen.add(u)
                    continue
                for c in rec["combos"]:
                    seen.add(c["url"])
                seen.add(u)
                with out_file.open("a", encoding="utf-8") as f:
                    f.write(json.dumps(rec, ensure_ascii=False) + chr(10))
                count += 1
                taken += 1
                colors = {c["labels"].get("Màu") for c in rec["combos"]}
                print(f"   [{count}] {rec['name']}: {len(rec['combos'])} tổ hợp, {len(colors)} màu, "
                      f"{sum(len(c['images']) for c in rec['combos'])} ảnh, {time.time() - started:.0f}s", flush=True)
        browser.close()


if __name__ == "__main__":
    main()
