import csv
import time
from urllib.parse import urljoin
from playwright.sync_api import sync_playwright

BASE_URL = "https://www.thegioididong.com"
OUTPUT_FILE = "tgdd_all_products.csv"

# Danh sách tất cả danh mục chính trên trang web
CATEGORIES = [
    {"name": "Điện thoại", "url": "https://www.thegioididong.com/dtdd"},
    {"name": "Laptop", "url": "https://www.thegioididong.com/laptop"},
    {"name": "Máy tính bảng", "url": "https://www.thegioididong.com/may-tinh-bang"},
    {"name": "Đồng hồ thông minh", "url": "https://www.thegioididong.com/dong-ho-thong-minh"},
    {"name": "Đồng hồ thời trang", "url": "https://www.thegioididong.com/dong-ho-thoi-trang"},
    {"name": "Phụ kiện", "url": "https://www.thegioididong.com/phu-kien"},
]

# ==== SELECTORS (Đã xác nhận) ====
PRODUCT_CARD_SELECTOR = "ul.listproduct li.item, div.listproduct div.item"
PRODUCT_LINK_SELECTOR = "a.main-contain"
PRODUCT_IMG_SELECTOR = "img.thumb"

LOAD_MORE_WRAPPER_SELECTOR = "div.view-more"
LOAD_MORE_LINK_SELECTOR = "div.view-more a"
REMAIN_COUNT_SELECTOR = "div.view-more span.remain"

MAX_CLICKS = 50  # Giới hạn số lần bấm "Xem thêm" cho mỗi danh mục
DELAY_BETWEEN_CLICKS = 1.5


def click_load_more(page):
    """Bấm nút 'Xem thêm' liên tục cho đến khi hết sản phẩm trong danh mục."""
    clicks = 0
    while clicks < MAX_CLICKS:
        wrapper = page.query_selector(LOAD_MORE_WRAPPER_SELECTOR)
        if not wrapper or not wrapper.is_visible():
            break

        remain_el = page.query_selector(REMAIN_COUNT_SELECTOR)
        remain_text = remain_el.inner_text().strip() if remain_el else "?"

        link = page.query_selector(LOAD_MORE_LINK_SELECTOR)
        if not link:
            break

        try:
            link.click()
            clicks += 1
            print(f"   └─ Bấm 'Xem thêm' lần {clicks} (còn {remain_text} sản phẩm)")
            time.sleep(DELAY_BETWEEN_CLICKS)
        except Exception:
            break


def crawl_single_category(page, cate_info):
    """Crawl toàn bộ sản phẩm của 1 danh mục cụ thể."""
    cate_name = cate_info["name"]
    cate_url = cate_info["url"]

    print(f"\n[+] Đang crawl danh mục: {cate_name} ({cate_url})")
    
    try:
        page.goto(cate_url, timeout=60000)
        page.wait_for_load_state("networkidle")
    except Exception as e:
        print(f"[-] Không thể truy cập {cate_url}: {e}")
        return []

    # Tải hết sản phẩm trong danh mục bằng cách click "Xem thêm"
    click_load_more(page)

    # Parse sản phẩm
    cards = page.query_selector_all(PRODUCT_CARD_SELECTOR)
    products = []

    for card in cards:
        link_el = card.query_selector(PRODUCT_LINK_SELECTOR)
        img_el = card.query_selector(PRODUCT_IMG_SELECTOR)
        rating_el = card.query_selector("div.rating_Compare div.vote-txt b")
        sold_el = card.query_selector("div.rating_Compare > span")

        if not link_el:
            continue

        name = link_el.get_attribute("data-name")
        price_raw = link_el.get_attribute("data-price")
        product_id = link_el.get_attribute("data-id")
        brand = link_el.get_attribute("data-brand")
        color = link_el.get_attribute("data-color")
        href = link_el.get_attribute("href")

        # Chuẩn hóa giá tiền
        try:
            price = int(float(price_raw)) if price_raw else None
        except ValueError:
            price = None

        url_full = urljoin(BASE_URL, href) if href else None
        rating = rating_el.inner_text().strip() if rating_el else None
        
        sold_text = None
        if sold_el:
            sold_text = sold_el.inner_text().lstrip("• ").strip()

        if name:
            products.append({
                "id": product_id,
                "category_group": cate_name,
                "name": name,
                "price": price,
                "brand": brand,
                "color": color,
                "rating": rating,
                "sold_qty": sold_text,
                "url": url_full,
                "image": img_el.get_attribute("src") if img_el else None,
            })

    print(f"[✓] Hoàn tất {cate_name}: Lấy được {len(products)} sản phẩm.")
    return products


def crawl_entire_site():
    """Lặp qua tất cả danh mục trên toàn hệ thống."""
    all_products = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(
            user_agent=(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"
            )
        )

        for cate in CATEGORIES:
            products = crawl_single_category(page, cate)
            all_products.extend(products)

        browser.close()

    return all_products


def save_to_csv(products, filename):
    if not products:
        print("Không có dữ liệu để lưu.")
        return
        
    # Lấy toàn bộ các field header
    fieldnames = list(products[0].keys())
    
    with open(filename, "w", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(products)
        
    print(f"\n==========================================")
    print(f"TỔNG CỘNG: Đã lưu {len(products)} sản phẩm vào file '{filename}'")
    print(f"==========================================")


if __name__ == "__main__":
    total_data = crawl_entire_site()
    save_to_csv(total_data, OUTPUT_FILE)