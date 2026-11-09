"""Luật phụ kiện cho mua kèm (app/recommender/compat.py, dùng ở scripts/build_relations.py và cross_sell)."""

from app.recommender.compat import compatible, fits, phone_model

CASE_16 = "Ốp lưng MagSafe iPhone 16 Nhựa Trong Apple"


def test_phone_model_reads_line_and_variant():
    assert phone_model(CASE_16) == "iphone 16"
    assert phone_model("iPhone 16 Plus 128GB") == "iphone 16 plus"
    assert phone_model("iPhone 17e") == "iphone 17e"
    assert phone_model("Galaxy S25 Ultra 5G") == "galaxy s25 ultra"


def test_case_only_fits_the_exact_model():
    assert fits("iPhone 16", "Điện Thoại", CASE_16)
    assert not fits("iPhone 16e", "Điện Thoại", CASE_16)
    assert not fits("iPhone 16 Plus", "Điện Thoại", CASE_16)
    assert not fits("iPhone 17e", "Điện Thoại", "Ốp lưng Magsafe iPhone 15 Pro Nhựa trong Apple")


def test_apple_pencil_pro_only_for_ipad_pro_air_mini():
    assert fits("iPad Air M4 11 inch WiFi", "Máy Tính Bảng", "Apple Pencil Pro")
    assert fits("iPad mini 7 WiFi", "Máy Tính Bảng", "Apple Pencil Pro")
    assert not fits("iPad A16 WiFi", "Máy Tính Bảng", "Apple Pencil Pro")


def test_apple_watch_magnetic_charger_only_for_apple_watch():
    cable = "Cáp sạc nhanh Type-C - Magnetic 1m Apple"
    assert fits("Apple Watch Series 12 GPS 42mm viền nhôm dây silicone", "Đồng Hồ Thông Minh", cable)
    assert not fits("Garmin Forerunner 265", "Đồng Hồ Thông Minh", cable)
    assert not fits("OPPO A6t Pro", "Điện Thoại", cable)
    assert not fits("iPhone 16 Plus 128GB", "Điện Thoại", cable)
    assert fits("OPPO A6t Pro", "Điện Thoại", "Pin sạc dự phòng 10000mAh Không dây Magnetic Qi2 Type C PD 30W Xmobile JP387")


def test_lightning_cable_only_for_iphone_14_and_older():
    cable = "Cáp sạc nhanh và truyền dữ liệu Type-C - Lightning 87W 1m Apple"
    assert fits("iPhone 14 Pro 128GB", "Điện Thoại", cable)
    assert not fits("iPhone 15 128GB", "Điện Thoại", cable)          # từ iPhone 15 dùng USB-C
    assert not fits("iPhone 16 Plus 128GB", "Điện Thoại", cable)
    assert not fits("iPad Air M4 11 inch WiFi", "Máy Tính Bảng", cable)
    assert not fits("Apple Watch SE 3 GPS 40mm", "Đồng Hồ Thông Minh", cable)


def test_tablet_gets_wireless_keyboards_only():
    assert not fits("iPad A16 WiFi", "Máy Tính Bảng", "Bàn phím mô phỏng Cơ Có Dây Gaming Razer Ornata V3")
    assert fits("iPad A16 WiFi", "Máy Tính Bảng", "Bàn Phím Bluetooth Logitech Wave Keys Ergonomic")
    assert fits("Laptop A", "Laptop", "Bàn phím mô phỏng Cơ Có Dây Gaming Razer Ornata V3")


def test_compatible_applies_apple_rule_to_accessories_only():
    cable = "Cáp sạc nhanh và truyền dữ liệu Type-C - Lightning 87W 1m Apple"
    assert not compatible("vivo Y05e", "Điện Thoại", "vivo", cable, "Phụ Kiện")
    assert not compatible("vivo Y05e", "Điện Thoại", "vivo", "Ốp lưng iPhone 16", "Phụ Kiện")


def test_device_to_device_only_within_the_same_brand():
    assert compatible("OPPO A6t Pro", "Điện Thoại", "OPPO", "OPPO Watch X3", "Đồng Hồ Thông Minh", accessory_brand="OPPO")
    assert not compatible("Acer Aspire Go 15", "Laptop", "Acer", "iPhone Air", "Điện Thoại", accessory_brand="Apple")
    assert compatible("A", "Cell Phones", "x", "B", "Cases", accessory_brand="y")   # ngoài catalogue: không lọc


def test_compatible_needs_an_accessory_group_of_the_device_category():
    assert not compatible("Nothing Phone (4b) 5G", "Điện Thoại", "Nothing", "Camera IP Ngoài Trời 360 Độ 2MP TP-Link",
                          "Phụ Kiện")
    assert not compatible("Xiaomi POCO M8 5G", "Điện Thoại", "Xiaomi", "Bàn Phím Bluetooth Logitech Wave Keys",
                          "Phụ Kiện")
    assert compatible("Xiaomi POCO M8 5G", "Điện Thoại", "Xiaomi", "Tai nghe Bluetooth Xiaomi Redmi Buds 8", "Phụ Kiện")
    assert compatible("Laptop B", "Laptop", "Lenovo", "Router Wifi Asus RT-BE50", "Phụ Kiện")


def test_laptop_bag_must_be_at_least_screen_size():
    bag = "Túi chống sốc Laptop 15.6 inch Togo Kéo Ngang có Ngăn Phụ"
    assert fits("Laptop A", "Laptop", bag, "15.6 inch")
    assert fits("Laptop B", "Laptop", bag, '14"')
    assert not fits("Laptop C", "Laptop", bag, "16 inch")
    assert not fits("Laptop D", "Laptop", bag, None)        # không biết cỡ màn hình thì không gắn túi có cỡ


def test_generic_accessories_fit_everything():
    assert fits("iPhone 17e", "Điện Thoại", "Cáp sạc nhanh Type-C 1m")
    assert fits("Laptop E", "Laptop", "Chuột không dây Logitech", None)
