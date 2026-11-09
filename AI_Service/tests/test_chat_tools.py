"""Chunk sản phẩm cho RAG và các tool của chatbot (không cần database, không gọi LLM)."""

import json

from app.chatbot import rag
from app.chatbot.tools import ToolRunner
from scripts.build_index import MAX_SPECS, pick_specs


def test_pick_specs_puts_category_key_specs_first_then_original_order():
    specs = [("Radio", "FM"), ("Nghe nhạc", "MP3"), ("Dung lượng pin", "5000 mAh"),
             ("Độ phân giải camera sau", "50 MP"), ("Chip xử lý (CPU)", "Helio G85")]
    assert pick_specs("Điện Thoại", specs) == [
        "Chip xử lý (CPU): Helio G85", "Độ phân giải camera sau: 50 MP", "Dung lượng pin: 5000 mAh",
        "Radio: FM", "Nghe nhạc: MP3"]


def test_pick_specs_keeps_first_duplicate_and_caps_count():
    specs = [("RAM", "8 GB"), ("RAM", "12 GB")] + [(f"Thông số {i}", str(i)) for i in range(20)]
    picked = pick_specs("Danh mục lạ", specs)
    assert picked[0] == "RAM: 8 GB" and len(picked) == MAX_SPECS


def test_brief_shortens_long_variant_list_so_specs_fit():
    variants = ", ".join(f"8GB/256GB - Màu {i}" for i in range(20))
    content = (f"Máy X – Điện Thoại hãng Y. Giá bán từ 5.000.000đ. Bảo hành 12 tháng. Phiên bản: {variants}. "
               "Thông số: Độ phân giải camera sau: 50 MP; Dung lượng pin: 5000 mAh.")
    brief = ToolRunner._brief(content)
    assert "Dung lượng pin: 5000 mAh" in brief and "…" in brief and len(brief) <= 500


def test_brief_keeps_short_variant_list_unchanged():
    content = "Ốp lưng – Phụ Kiện hãng Z. Giá bán từ 100.000đ. Bảo hành 12 tháng. Phiên bản: Đen."
    assert ToolRunner._brief(content) == content


def test_unknown_tool_arguments_are_dropped(monkeypatch):
    monkeypatch.setattr(rag, "search", lambda *args, **kwargs: [])
    out = json.loads(ToolRunner(None).run("search_products", {"query": "điện thoại", "city": "Hà Nội"}))
    assert out["products"] == []          # trước đây: TypeError → TOOL_FAILED


def test_backend_reply_missing_fields_fails_the_tool_not_the_chat(monkeypatch):
    runner = ToolRunner("token")
    monkeypatch.setattr(runner, "_call", lambda *args, **kwargs: None)          # Backend trả "data": null
    out = json.loads(runner.run("get_my_orders", {}))
    assert out["error"] == "TOOL_FAILED"                                        # trước đây: AttributeError ném ra


def test_my_orders_separates_unfinished_orders(monkeypatch):
    statuses = ["DELIVERED", "PENDING", "CANCELLED", "DELIVERED", "SHIPPING", "DELIVERED", "DELIVERED"]
    page = {"content": [{"id": i, "code": f"DH{i:08d}", "status": s, "items": []} for i, s in enumerate(statuses)]}
    runner = ToolRunner("token")
    monkeypatch.setattr(runner, "_call", lambda *args, **kwargs: page)
    out = runner._get_my_orders()
    assert [o["status"] for o in out["active_orders"]] == ["PENDING", "SHIPPING"]
    assert [o["id"] for o in out["recent_completed_orders"]] == [0, 2, 3]
    assert out["answer_lines"] == ["DH00000001 – Chờ xác nhận", "DH00000004 – Đang giao"]


def test_phantom_confirm_button_sentence_is_dropped():
    from app.chatbot.agent import drop_phantom_buttons
    reply = ("Đơn hàng của bạn đang chờ xác nhận (PENDING).\n\n- **DH00000389** – Chờ xác nhận – Casio x1\n\n"
             "Bạn có thể bấm nút Xác nhận bên dưới để hủy đơn nếu muốn.")
    assert drop_phantom_buttons(reply) == ("Đơn hàng của bạn đang chờ xác nhận (PENDING).\n\n"
                                           "- **DH00000389** – Chờ xác nhận – Casio x1")


IPHONE_17E_VARIANTS = [{"id": 1, "variantName": "256 GB - Đen", "price": 17990000},
                       {"id": 2, "variantName": "256 GB - Trắng", "price": 17990000},
                       {"id": 3, "variantName": "512 GB - Đen", "price": 23990000, "discountPrice": 22990000}]


def test_variant_matching_by_words():
    match = ToolRunner._match_variants
    assert [v["id"] for v in match(IPHONE_17E_VARIANTS, "256GB màu Đen")] == [1]   # trước: so nguyên chuỗi, không khớp
    assert [v["id"] for v in match(IPHONE_17E_VARIANTS, "256GB")] == [1, 2]
    assert match(IPHONE_17E_VARIANTS, "") == []


def test_prices_by_version_takes_cheapest_color(monkeypatch):
    runner = ToolRunner(None)
    monkeypatch.setattr(runner, "_call", lambda *args, **kwargs: IPHONE_17E_VARIANTS)
    assert runner._prices_by_version(37) == {"256 GB": 17990000, "512 GB": 22990000}


def test_recommend_products_resolves_product_name(monkeypatch):
    monkeypatch.setattr(rag, "search", lambda *args, **kwargs: [{"product_id": 37}])
    runner = ToolRunner(None)
    sent = {}
    def call(method, path, **kwargs):
        sent.update(kwargs["json"])
        return {"items": [], "source": "AI"}
    monkeypatch.setattr(runner, "_call", call)
    runner._recommend_products("CROSS_SELL", product_name="iPhone 17e")
    assert sent["productId"] == 37                      # trước: mô hình không biết id → productId rỗng → không có gợi ý


def test_add_to_cart_reads_variant_from_product_name(monkeypatch):
    monkeypatch.setattr(rag, "search", lambda *args, **kwargs: [{"product_id": 37}])
    runner = ToolRunner("token")
    product = {"id": 37, "name": "iPhone 17e", "isActive": True, "effectivePrice": 17990000}
    monkeypatch.setattr(runner, "_call", lambda method, path, **kwargs:
                        IPHONE_17E_VARIANTS if path.endswith("/variants") else product)
    runner._propose_add_to_cart(product_name="iPhone 17e 256GB Đen")
    assert runner.actions[0]["variantId"] == 1
