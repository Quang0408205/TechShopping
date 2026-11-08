"""Luật gắn phụ kiện cho mua kèm (scripts/build_relations.py): phụ kiện chỉ vừa một số máy."""

from scripts.build_relations import fits, phone_model

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


def test_laptop_bag_must_be_at_least_screen_size():
    bag = "Túi chống sốc Laptop 15.6 inch Togo Kéo Ngang có Ngăn Phụ"
    assert fits("Laptop A", "Laptop", bag, "15.6 inch")
    assert fits("Laptop B", "Laptop", bag, '14"')
    assert not fits("Laptop C", "Laptop", bag, "16 inch")
    assert not fits("Laptop D", "Laptop", bag, None)        # không biết cỡ màn hình thì không gắn túi có cỡ


def test_generic_accessories_fit_everything():
    assert fits("iPhone 17e", "Điện Thoại", "Cáp sạc nhanh Type-C 1m")
    assert fits("Laptop E", "Laptop", "Chuột không dây Logitech", None)
