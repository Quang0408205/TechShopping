import csv
import json
import re
import time
from pathlib import Path
from urllib.parse import urljoin
from playwright.sync_api import sync_playwright

BASE_URL = "https://www.thegioididong.com"
OUTPUT_FILE = Path(__file__).with_name("tgdd_all_products.csv")

# Danh sách tất cả danh mục chính trên trang web
CATEGORIES = [
    {"name": "Điện thoại", "url": "https://www.thegioididong.com/dtdd"},
    {"name": "Laptop", "url": "https://www.thegioididong.com/laptop"},
    {"name": "Máy tính bảng", "url": "https://www.thegioididong.com/may-tinh-bang"},
    {"name": "Đồng hồ thông minh", "url": "https://www.thegioididong.com/dong-ho-thong-minh"},
    {"name": "Đồng hồ thời trang", "url": "https://www.thegioididong.com/dong-ho-deo-tay"},
    {"name": "Phụ kiện", "url": "https://www.thegioididong.com/phu-kien"},
]

# TGDD alternates between its legacy card markup and newer product-card markup.
PRODUCT_CARD_SELECTOR = (
    "li.item[data-id]:has(a.main-contain), "
    "[data-product-code][data-model-code]"
)

FIELDNAMES = [
    "id", "category_group", "name", "price", "brand",
    "color", "rating", "sold_qty", "url", "image",
    "source_sku", "description", "images", "specifications", "review_count",
    "detail_status", "category_complete",
]

HEADLESS = True              # đặt False để xem trình duyệt chạy thật (hữu ích khi bị chặn)
WAIT_AFTER_CLICK_MS = 25000   # Chờ tải thêm sản phẩm, một số danh mục phản hồi chậm
LOAD_MORE_ATTEMPTS = 2
DELAY_BETWEEN_CATEGORIES = 4  # Giây nghỉ giữa các danh mục
DELAY_BETWEEN_DETAILS = 1.0   # Giây nghỉ giữa các trang chi tiết
MAX_DETAIL_ATTEMPTS = 3


def debug_dump(page, cate_name):
    """Khi không thấy sản phẩm: in thông tin chẩn đoán, lưu ảnh chụp và HTML để xem trang thật sự hiển thị gì."""
    try:
        safe = cate_name.replace(" ", "_")
        print(f"   [debug] URL hiện tại: {page.url}")
        print(f"   [debug] Tiêu đề trang: {page.title()!r}")
        print(f"   [debug] Số thẻ khớp selector: {count_cards(page)}")
        page.screenshot(path=f"debug_{safe}.png", full_page=False)
        with open(f"debug_{safe}.html", "w", encoding="utf-8") as f:
            f.write(page.content())
        print(f"   [debug] Đã lưu debug_{safe}.png và debug_{safe}.html")
    except Exception as e:
        print(f"   [debug] Không lưu được thông tin chẩn đoán: {e}")


def count_cards(page):
    return len(page.query_selector_all(PRODUCT_CARD_SELECTOR))


def click_load_more(page, category_name):
    """Bấm 'Xem thêm' cho đến khi hết sản phẩm. Trả về True nếu đã tải hết."""
    clicks = 0
    while True:
        legacy_link = page.locator("div.view-more a").first
        if legacy_link.count() and legacy_link.is_visible():
            button = legacy_link
        else:
            button = page.locator("button").filter(
                has_text=re.compile(rf"^Xem thêm\s+\d+\s+{re.escape(category_name)}")
            ).first
        if not button.count() or not button.is_visible():
            if clicks == 0:
                print(
                    f"   [!] Không thấy nút tải thêm cho {category_name}; "
                    "trang có thể chỉ hiển thị một phần sản phẩm."
                )
                return False
            return True

        remain_text = button.inner_text().strip()
        before = count_cards(page)

        for attempt in range(1, LOAD_MORE_ATTEMPTS + 1):
            try:
                button.click()
                if attempt == 1:
                    clicks += 1
                    print(f"   └─ Bấm 'Xem thêm' lần {clicks} (còn {remain_text} sản phẩm)")
                page.wait_for_function(
                    "([sel, n]) => document.querySelectorAll(sel).length > n",
                    arg=[PRODUCT_CARD_SELECTOR, before],
                    timeout=WAIT_AFTER_CLICK_MS,
                )
                break
            except Exception as exc:
                if count_cards(page) > before:
                    break
                if attempt == LOAD_MORE_ATTEMPTS:
                    print(f"   [!] Không tải thêm được sản phẩm: {exc}")
                    return False
                print(f"   [!] Lần tải thêm bị chậm, đang thử lại: {exc}")
                time.sleep(2)
                legacy_link = page.locator("div.view-more a").first
                button = legacy_link if legacy_link.count() else page.locator("button").filter(
                    has_text=re.compile(rf"^Xem thêm\s+\d+\s+{re.escape(category_name)}")
                ).first
                if not button.count() or not button.is_visible():
                    if count_cards(page) > before:
                        break
                    return False


def scroll_to_load_images(page):
    """Cuộn từ trên xuống dưới để các ảnh lazy-load kịp tải."""
    y = 0
    step = 800
    for _ in range(400):  # chặn vòng lặp vô hạn
        height = page.evaluate("document.body.scrollHeight")
        if y >= height:
            break
        page.evaluate(f"window.scrollTo(0, {y})")
        time.sleep(0.15)
        y += step
    page.evaluate("window.scrollTo(0, 0)")
    time.sleep(0.5)


def get_image_url(img_el):
    """Lấy link ảnh thật, ưu tiên thuộc tính lazy-load."""
    if not img_el:
        return None
    for attr in ("data-src", "data-original", "src"):
        val = img_el.get_attribute(attr)
        if val and not val.startswith("data:"):
            return val
    return None


def _walk_product_nodes(value):
    if isinstance(value, dict):
        types = value.get("@type") or []
        if isinstance(types, str):
            types = [types]
        elif not isinstance(types, list):
            types = []
        if any(t in ("Product", "ProductGroup") for t in types):
            yield value
        for child in value.values():
            yield from _walk_product_nodes(child)
    elif isinstance(value, list):
        for child in value:
            yield from _walk_product_nodes(child)


def extract_product_details(page):
    """Read schema.org JSON-LD and common key/value tables without inventing missing values."""
    product = None
    for script_text in page.locator('script[type="application/ld+json"]').all_text_contents():
        try:
            parsed = json.loads(script_text)
        except json.JSONDecodeError:
            continue
        product = next(_walk_product_nodes(parsed), None)
        if product:
            break

    images = []
    specifications = {}
    details = {
        "source_sku": None,
        "description": None,
        "images": [],
        "specifications": {},
        "review_count": None,
    }
    if product:
        details["source_sku"] = product.get("sku")
        details["description"] = product.get("description")
        image_data = product.get("image") or []
        if isinstance(image_data, str):
            image_data = [image_data]
        elif not isinstance(image_data, list):
            image_data = []
        for image in image_data:
            image_url = (
                image.get("url") or image.get("contentUrl")
                if isinstance(image, dict)
                else image
            )
            if image_url:
                images.append(urljoin(page.url, image_url))

        properties = product.get("additionalProperty") or []
        if isinstance(properties, dict):
            properties = [properties]
        elif not isinstance(properties, list):
            properties = []
        for prop in properties:
            if isinstance(prop, dict) and prop.get("name") and prop.get("value") is not None:
                specifications[str(prop["name"]).strip()] = str(prop["value"]).strip()

        aggregate = product.get("aggregateRating") or {}
        if isinstance(aggregate, dict):
            details["review_count"] = aggregate.get("reviewCount") or aggregate.get("ratingCount")

    # Product detail pages may render specifications as ordinary HTML tables.
    for row in page.locator("table tr").all():
        cells = [cell.strip() for cell in row.locator("th, td").all_inner_texts() if cell.strip()]
        if len(cells) >= 2:
            specifications.setdefault(cells[0], " ".join(cells[1:]))

    details["images"] = list(dict.fromkeys(images))
    details["specifications"] = specifications
    return details


def enrich_product_details(page, products, details_cache):
    """Visit each product page once; preserve listing data and report detail failures."""
    for product in products:
        product["detail_status"] = "unavailable"
        product["description"] = None
        product["images"] = "[]"
        product["specifications"] = "{}"
        product["review_count"] = None
        url = product.get("url")
        if not url:
            continue
        if url in details_cache:
            product.update(details_cache[url])
            continue
        last_error = None
        for attempt in range(1, MAX_DETAIL_ATTEMPTS + 1):
            try:
                page.goto(url, timeout=60000, wait_until="load")
                details = extract_product_details(page)
                details_cache[url] = {
                    "source_sku": details["source_sku"] or product.get("source_sku"),
                    "description": details["description"],
                    "images": json.dumps(details["images"], ensure_ascii=False),
                    "specifications": json.dumps(details["specifications"], ensure_ascii=False),
                    "review_count": details["review_count"],
                    "detail_status": "ok",
                }
                product.update(details_cache[url])
                last_error = None
                break
            except Exception as exc:
                last_error = exc
                if attempt < MAX_DETAIL_ATTEMPTS:
                    print(
                        f"   [!] Lỗi khi tải chi tiết (lần {attempt}/{MAX_DETAIL_ATTEMPTS}), "
                        f"đang thử lại {url}: {exc}"
                    )
                    time.sleep(DELAY_BETWEEN_DETAILS)

        if last_error is not None:
            product["detail_status"] = "failed"
            print(
                f"   [!] Không lấy được chi tiết sau {MAX_DETAIL_ATTEMPTS} lần "
                f"{url}: {last_error}"
            )
            details_cache[url] = {
                "source_sku": product.get("source_sku"),
                "description": None,
                "images": "[]",
                "specifications": "{}",
                "review_count": None,
                "detail_status": "failed",
            }
        time.sleep(DELAY_BETWEEN_DETAILS)


def extract_listing_product(card, cate_info, category_complete):
    product_id = card.get_attribute("data-model-code") or card.get_attribute("data-id")
    source_sku = card.get_attribute("data-product-code") or card.get_attribute("data-productcode")
    if not product_id:
        return None

    product_link = card.query_selector("a.main-contain") or next(
        (
            link
            for link in card.query_selector_all("a[href]")
            if link.get_attribute("href")
            and not link.get_attribute("href").startswith(("javascript:", "#"))
            and link.inner_text().strip()
        ),
        None,
    )
    if not product_link:
        return None

    name = (
        product_link.get_attribute("data-name")
        or product_link.inner_text().strip()
    )
    href = product_link.get_attribute("href")
    url_full = urljoin(BASE_URL, href) if href else None
    price = None
    price_raw = product_link.get_attribute("data-price") or card.get_attribute("data-price")
    if price_raw:
        try:
            price = int(float(price_raw))
            if price <= 0:
                price = None
        except ValueError:
            price = None
    for price_link in card.query_selector_all("a[href]"):
        if price is not None:
            break
        price_match = re.search(
            r"(\d[\d.,]*)\s*(?:₫|đ)",
            price_link.inner_text().replace("\xa0", " "),
            re.IGNORECASE,
        )
        if price_match:
            digits = re.sub(r"\D", "", price_match.group(1))
            if digits:
                price = int(digits)
                break

    card_text = card.inner_text()
    rating_match = re.search(r"\b([0-5](?:[.,]\d)?)\s*•", card_text)
    sold_match = re.search(r"•\s*Đã bán\s*([\d.,]+\s*k?)", card_text, re.IGNORECASE)
    brand_name = next(
        (
            brand
            for prefix, brand in (
                ("iPhone", "Apple"), ("iPad", "Apple"), ("MacBook", "Apple"),
                ("Redmi", "Xiaomi"), ("POCO", "Xiaomi"), ("Samsung", "Samsung"),
                ("Xiaomi", "Xiaomi"), ("OPPO", "OPPO"), ("realme", "realme"),
                ("vivo", "vivo"), ("Motorola", "Motorola"), ("HONOR", "HONOR"),
                ("Huawei", "Huawei"), ("Nokia", "Nokia"), ("Tecno", "Tecno"),
                ("Lenovo", "Lenovo"), ("Dell", "Dell"), ("HP", "HP"),
                ("Asus", "Asus"), ("Acer", "Acer"), ("MSI", "MSI"),
                ("Garmin", "Garmin"), ("Amazfit", "Amazfit"),
            )
            if name.casefold().startswith(prefix.casefold())
        ),
        "",
    )

    return {
        "id": product_id,
        "source_sku": source_sku,
        "category_group": cate_info["name"],
        "name": name,
        "price": price,
        "brand": product_link.get_attribute("data-brand") or brand_name,
        "color": product_link.get_attribute("data-color"),
        "rating": rating_match.group(1).replace(",", ".") if rating_match else None,
        "sold_qty": sold_match.group(1) if sold_match else None,
        "url": url_full,
        "image": get_image_url(card.query_selector("img")),
        "category_complete": category_complete,
    }


def crawl_single_category(page, cate_info):
    """Crawl toàn bộ sản phẩm của 1 danh mục cụ thể."""
    cate_name = cate_info["name"]
    cate_url = cate_info["url"]

    print(f"\n[+] Đang crawl danh mục: {cate_name} ({cate_url})")

    try:
        page.goto(cate_url, timeout=60000, wait_until="domcontentloaded")
        page.wait_for_function(
            "sel => document.querySelectorAll(sel).length > 0",
            arg=PRODUCT_CARD_SELECTOR,
            timeout=30000,
        )
    except Exception as e:
        print(f"[-] Không thể truy cập {cate_url}: {e}")
        debug_dump(page, cate_name)
        return []

    # The cards are server-rendered; wait briefly for the category's click handlers to hydrate.
    page.wait_for_timeout(3000)

    # Tải hết sản phẩm, rồi cuộn để ảnh lazy-load hiện ra
    category_complete = click_load_more(page, cate_name)
    if not category_complete:
        print(f"   [!] Danh mục {cate_name} có thể chưa tải hết; dữ liệu được đánh dấu chưa hoàn chỉnh.")
    scroll_to_load_images(page)

    cards = page.query_selector_all(PRODUCT_CARD_SELECTOR)
    products = []

    for card in cards:
        product = extract_listing_product(card, cate_info, category_complete)
        if product:
            products.append(product)

    no_img = sum(1 for p in products if not p["image"])
    state = "đã tải hết" if category_complete else "có thể chưa tải hết"
    print(f"[✓] {cate_name}: Lấy được {len(products)} sản phẩm, {state} ({no_img} thiếu ảnh).")
    return products


def save_to_csv(products, filename):
    if not products:
        print("Không có dữ liệu để lưu.")
        return

    with open(filename, "w", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=FIELDNAMES)
        writer.writeheader()
        writer.writerows(products)

    print(f"   [💾] Đã lưu tạm {len(products)} sản phẩm vào '{filename}'")


def crawl_entire_site():
    """Lặp qua tất cả danh mục; lưu file sau mỗi danh mục để lỗi giữa chừng không mất dữ liệu."""
    all_products = []
    details_cache = {}

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=HEADLESS)
        page = browser.new_page(
            user_agent=(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"
            )
        )

        for i, cate in enumerate(CATEGORIES):
            products = crawl_single_category(page, cate)
            all_products.extend(products)
            save_to_csv(all_products, OUTPUT_FILE)
            enrich_product_details(page, products, details_cache)
            save_to_csv(all_products, OUTPUT_FILE)
            if i < len(CATEGORIES) - 1:
                time.sleep(DELAY_BETWEEN_CATEGORIES)

        browser.close()

    return all_products


if __name__ == "__main__":
    total_data = crawl_entire_site()
    print("\n==========================================")
    print(f"TỔNG CỘNG: Đã lưu {len(total_data)} sản phẩm vào file '{OUTPUT_FILE}'")
    print("==========================================")