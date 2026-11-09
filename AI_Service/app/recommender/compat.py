"""
Phụ kiện có dùng được với máy này không (báo cáo mục 3.5.6 b). Dùng ở hai chỗ:
  - scripts/build_relations.py khi tạo quan hệ ACCESSORY;
  - Recommender.cross_sell lọc gợi ý đến từ luật mua chung (cùng đơn, "khách mua A cũng mua B"), để luật học từ hành vi
    không gợi ý phụ kiện không vừa máy (vd. cáp Lightning cho điện thoại Android) hay không thuộc nhóm phụ kiện của loại
    máy đó (vd. camera IP cho điện thoại).
"""

import re

ACCESSORY_CATEGORY = "Phụ Kiện"

# danh mục thiết bị → nhóm phụ kiện phù hợp (so khớp tên phụ kiện, không phân biệt hoa thường)
RULES = {
    "Điện Thoại": ("ốp lưng", "kính cường lực", "sạc", "cáp", "pin sạc dự phòng", "tai nghe", "airpods"),
    "Máy Tính Bảng": ("apple pencil", "bàn phím", "sạc", "cáp", "pin sạc dự phòng", "tai nghe", "airpods"),
    "Laptop": ("chuột", "bàn phím", "balo laptop", "túi chống sốc laptop", "tai nghe", "router", "sạc nhanh"),
    "Đồng Hồ Thông Minh": ("sạc", "tai nghe", "airpods", "pin sạc dự phòng"),
    "Màn Hình Máy Tính": ("cáp", "chuột", "bàn phím", "tai nghe", "loa"),
    "Máy Tính Để Bàn": ("chuột", "bàn phím", "tai nghe", "loa", "router"),
}

APPLE_ONLY = re.compile(r"iphone|lightning|apple pencil|magic keyboard|magic mouse|magsafe iphone", re.IGNORECASE)
MODEL_SPECIFIC = re.compile(r"ốp|kính cường lực|miếng dán|bao da", re.IGNORECASE)
PHONE_MODEL = re.compile(r"\b(iphone|galaxy)\s+([a-z]?\d+[a-z]?)(?:\s+(pro max|pro|plus|ultra|fe))?\b", re.IGNORECASE)
WATCH_CHARGER = re.compile(r"(cáp|sạc).*magnetic.*apple", re.IGNORECASE)  # sạc từ tính riêng của Apple Watch
INCH = re.compile(r"(\d+(?:[.,]\d+)?)\s*(?:inch|\")", re.IGNORECASE)
LAST_LIGHTNING_IPHONE = 14  # từ iPhone 15 (2023) và mọi iPad, Apple Watch đang bán đều không dùng cổng Lightning


def phone_model(name: str) -> str | None:
    """'Ốp lưng MagSafe iPhone 16 Nhựa' → 'iphone 16'; 'iPhone 16 Plus 128GB' → 'iphone 16 plus'."""
    match = PHONE_MODEL.search(name)
    return " ".join(part.lower() for part in match.groups() if part) if match else None


def inches(text: str | None) -> float | None:
    match = INCH.search(text or "")
    return float(match.group(1).replace(",", ".")) if match else None


def uses_lightning(device_name: str) -> bool:
    match = re.search(r"\biphone\s+(\d+)", device_name.lower())
    return match is not None and int(match.group(1)) <= LAST_LIGHTNING_IPHONE


def fits(device_name: str, device_category: str, accessory_name: str, device_screen: str | None = None) -> bool:
    """Phụ kiện có dùng được với máy này không (ngoài luật theo nhóm và luật Apple)."""
    name = accessory_name.lower()
    if MODEL_SPECIFIC.search(name) and phone_model(name):
        return phone_model(device_name) == phone_model(name)
    if "lightning" in name:
        return device_category == "Điện Thoại" and uses_lightning(device_name)
    if "apple pencil pro" in name:
        return device_category == "Máy Tính Bảng" and re.search(r"ipad (pro|air|mini)", device_name.lower()) is not None
    if WATCH_CHARGER.search(name) and "pin sạc dự phòng" not in name:
        return device_category == "Đồng Hồ Thông Minh" and "apple watch" in device_name.lower()
    if device_category == "Máy Tính Bảng" and "bàn phím" in name and "có dây" in name:
        return False                                  # máy tính bảng dùng bàn phím Bluetooth / không dây
    if device_category == "Laptop" and re.search(r"balo|túi", name) and inches(name):
        screen = inches(device_screen)
        return screen is not None and screen <= inches(name) + 0.05
    return True


def compatible(device_name: str, device_category: str, device_brand: str, accessory_name: str,
               accessory_category: str, device_screen: str | None = None, accessory_brand: str | None = None) -> bool:
    """Phụ kiện phải thuộc nhóm hợp với loại máy (RULES), qua luật Apple và fits. Thiết bị gợi ý cho thiết bị khác
    (vd. đồng hồ cho điện thoại) chỉ khi cùng hãng, tức cùng hệ sinh thái (OPPO → OPPO Watch, Apple Watch → MacBook).
    Danh mục ngoài catalogue này (vd. dữ liệu Amazon) không bị lọc."""
    if accessory_category != ACCESSORY_CATEGORY:
        if device_category in RULES and accessory_category in RULES and accessory_brand is not None:
            return str(device_brand).lower() == str(accessory_brand).lower()
        return True
    keywords = RULES.get(device_category)
    if keywords is not None and not any(word in accessory_name.lower() for word in keywords):
        return False
    if APPLE_ONLY.search(accessory_name) and str(device_brand).lower() != "apple":
        return False
    return fits(device_name, device_category, accessory_name, device_screen)
