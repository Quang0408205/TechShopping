"""
Function Calling (báo cáo mục 3.6.3, Bảng 3.24). Mỗi tool là một chức năng nghiệp vụ; tool cần dữ liệu riêng của
khách gọi API Spring Boot KÈM access token của chính khách đó, nên Backend vẫn tự xác thực và kiểm tra quyền
(đơn hàng phải thuộc tài khoản đang đăng nhập). Chatbot Service không truy cập bảng nghiệp vụ trực tiếp;
nó chỉ đọc kho tri thức kb_chunks của riêng nó.
"""

import json
import logging
from typing import Any

import httpx

from app.chatbot import rag
from app import config
from app.config import BACKEND_URL

log = logging.getLogger(__name__)

LOGIN_REQUIRED = {"error": "LOGIN_REQUIRED", "message": "Khách chưa đăng nhập nên không xem được dữ liệu này."}

TOOLS: list[dict] = [
    {
        "name": "search_policies",
        "description": "Tra cứu kho tri thức chính sách của cửa hàng POY: giao hàng và phí giao hàng, thanh toán, "
                       "chuyển khoản, trả góp, bảo hành, bảo trì, trả hàng hoàn tiền, cách gửi yêu cầu, liên hệ, tài "
                       "khoản. Dùng cho mọi câu hỏi về quy định / chính sách / hướng dẫn.",
        "input_schema": {
            "type": "object",
            "properties": {"query": {"type": "string", "description": "Câu hỏi của khách, viết lại cho rõ nghĩa"}},
            "required": ["query"],
        },
    },
    {
        "name": "search_products",
        "description": "Tìm sản phẩm đang bán theo nhu cầu (tìm theo ngữ nghĩa) và/hoặc bộ lọc. Danh mục hiện có: "
                       "Điện Thoại, Laptop, Máy Tính Bảng, Đồng Hồ Thông Minh, Đồng Hồ Thời Trang, Phụ Kiện. "
                       "Giá tính bằng đồng (10 triệu = 10000000).",
        "input_schema": {
            "type": "object",
            "properties": {
                "query": {"type": "string", "description": "Tên sản phẩm hoặc nhu cầu, vd. 'iPhone 17 Pro', "
                                                           "'laptop mỏng nhẹ cho sinh viên', 'pin trâu chụp ảnh đẹp'"},
                "category": {"type": "string", "description": "Tên danh mục, bỏ trống nếu không rõ"},
                "brand": {"type": "string", "description": "Thương hiệu, vd. Apple, Samsung"},
                "min_price": {"type": "integer"},
                "max_price": {"type": "integer"},
                "limit": {"type": "integer", "description": "Số sản phẩm (mặc định 5, tối đa 8)"},
            },
            "required": ["query"],
        },
    },
    {
        "name": "get_product_details",
        "description": "Thông tin chi tiết, giá hiện tại, các phiên bản (màu, dung lượng) và thông số kỹ thuật của "
                       "một sản phẩm theo product_id (lấy từ kết quả search_products). Khi khách hỏi giá một sản "
                       "phẩm cụ thể, gọi search_products với tên sản phẩm rồi dùng giá trong kết quả.",
        "input_schema": {
            "type": "object",
            "properties": {"product_id": {"type": "integer"}},
            "required": ["product_id"],
        },
    },
    {
        "name": "recommend_products",
        "description": "Gợi ý sản phẩm từ hệ thống khuyến nghị: PERSONAL = hợp với sở thích của khách (khách chưa "
                       "đăng nhập nhận sản phẩm phổ biến); SIMILAR = tương tự sản phẩm product_id; UPSELL = bản cao "
                       "cấp hơn của product_id; CROSS_SELL = phụ kiện / sản phẩm nên mua kèm product_id.",
        "input_schema": {
            "type": "object",
            "properties": {
                "type": {"type": "string", "enum": ["PERSONAL", "SIMILAR", "UPSELL", "CROSS_SELL"]},
                "product_id": {"type": "integer", "description": "Bắt buộc với SIMILAR, UPSELL, CROSS_SELL"},
            },
            "required": ["type"],
        },
    },
    {
        "name": "get_my_orders",
        "description": "Danh sách đơn hàng gần đây của khách đang đăng nhập (mã đơn, trạng thái, tổng tiền, sản "
                       "phẩm). Gọi NGAY khi khách hỏi về đơn hàng của họ mà không nêu mã đơn (vd. 'đơn của tôi tới "
                       "đâu rồi'); không cần hỏi mã đơn. Cần đăng nhập.",
        "input_schema": {"type": "object", "properties": {}},
    },
    {
        "name": "get_order_detail",
        "description": "Chi tiết và trạng thái một đơn hàng của khách đang đăng nhập, theo mã đơn (vd. DH000123) "
                       "hoặc số id. Cần đăng nhập.",
        "input_schema": {
            "type": "object",
            "properties": {"order": {"type": "string", "description": "Mã đơn hoặc id"}},
            "required": ["order"],
        },
    },
    {
        "name": "get_my_cart",
        "description": "Các sản phẩm đang có trong giỏ hàng của khách và tạm tính. Cần đăng nhập.",
        "input_schema": {"type": "object", "properties": {}},
    },
    {
        "name": "get_my_service_requests",
        "description": "Các yêu cầu bảo hành, bảo trì, trả hàng của khách và trạng thái xử lý. Cần đăng nhập.",
        "input_schema": {"type": "object", "properties": {}},
    },
    {
        "name": "propose_add_to_cart",
        "description": "Đề xuất thêm một sản phẩm vào giỏ hàng của khách khi khách nói rõ muốn mua / thêm vào giỏ. "
                       "KHÔNG tự thêm: tạo nút Xác nhận để khách bấm. Truyền product_id nếu đã biết từ kết quả "
                       "search_products, nếu chưa thì truyền product_name (tên khách nói), tool tự tìm. Sản phẩm nhiều "
                       "phiên bản thì cần variant_name (vd. '256 GB - Đen'); thiếu thì tool trả danh sách phiên bản để "
                       "hỏi lại khách. Cần đăng nhập.",
        "input_schema": {
            "type": "object",
            "properties": {
                "product_id": {"type": "integer", "description": "Bỏ trống nếu chưa biết, dùng product_name"},
                "product_name": {"type": "string", "description": "Tên sản phẩm khách nói, vd. 'iPhone 17e 256GB'"},
                "variant_name": {"type": "string", "description": "Tên phiên bản khách chọn, bỏ trống nếu chưa rõ"},
                "quantity": {"type": "integer", "description": "Số lượng, mặc định 1, tối đa 10"},
            },
        },
    },
    {
        "name": "propose_cancel_order",
        "description": "Đề xuất huỷ một đơn hàng của khách khi khách nói rõ muốn huỷ. Chỉ đơn đang Chờ xác nhận "
                       "(PENDING) mới huỷ được. KHÔNG tự huỷ: tạo nút Xác nhận để khách bấm. Cần đăng nhập.",
        "input_schema": {
            "type": "object",
            "properties": {"order": {"type": "string", "description": "Mã đơn (vd. DH00000397) hoặc id"}},
            "required": ["order"],
        },
    },
    {
        "name": "find_stores",
        "description": "Danh sách cửa hàng / chi nhánh POY đang mở (địa chỉ, quận, thành phố, điện thoại).",
        "input_schema": {
            "type": "object",
            "properties": {"city": {"type": "string", "description": "Lọc theo thành phố hoặc quận, có thể bỏ trống"}},
        },
    },
]


# Dòng trạng thái hiện trong khung chat khi Claude đang gọi tool (streaming)
TOOL_STATUS = {
    "search_policies": "Đang tra cứu chính sách...",
    "search_products": "Đang tìm sản phẩm...",
    "get_product_details": "Đang xem chi tiết sản phẩm...",
    "recommend_products": "Đang lấy gợi ý từ hệ thống khuyến nghị...",
    "get_my_orders": "Đang xem đơn hàng của bạn...",
    "get_order_detail": "Đang xem đơn hàng của bạn...",
    "get_my_cart": "Đang xem giỏ hàng của bạn...",
    "get_my_service_requests": "Đang xem yêu cầu bảo hành của bạn...",
    "find_stores": "Đang tìm cửa hàng...",
    "propose_add_to_cart": "Đang chuẩn bị thêm vào giỏ...",
    "propose_cancel_order": "Đang kiểm tra đơn hàng...",
}

WAITING = "WAITING_FOR_CUSTOMER_CONFIRMATION"
CONFIRM_HINT = ("CHƯA thực hiện. Đừng nói là đã thêm / đã huỷ. Hãy nói với khách: bấm nút Xác nhận bên dưới để "
                "thực hiện.")
MAX_CART_QUANTITY = 10


class BackendError(Exception):
    def __init__(self, status: int, code: str):
        super().__init__(code)
        self.status = status
        self.code = code


class ToolRunner:
    """
    Chạy tool cho MỘT lượt hội thoại; ghi lại product_id đã trả về để giao diện hiện thẻ sản phẩm, và các thao tác
    đề xuất (actions) để giao diện hiện nút Xác nhận. Thao tác chỉ được Website thực hiện khi khách bấm nút, bằng
    API thường của khách (POST /cart/items, POST /orders/{id}/cancel), không qua LLM.
    """

    def __init__(self, access_token: str | None):
        self.access_token = access_token
        self.product_names: dict[int, str] = {}
        self.used: list[str] = []
        self.actions: list[dict] = []
        self.trace: list[dict] = []          # (tool, input, output) cho đánh giá / gỡ lỗi

    # ---------- gọi API Spring Boot ----------

    def _call(self, method: str, path: str, *, auth: bool = False, **kwargs) -> Any:
        headers = {}
        if auth:
            headers["Authorization"] = f"Bearer {self.access_token}"
        response = httpx.request(method, BACKEND_URL + path, headers=headers, timeout=10, **kwargs)
        body = response.json() if response.content else {}
        if response.status_code >= 400:
            raise BackendError(response.status_code, (body.get("error") or {}).get("code", "HTTP_ERROR"))
        return body.get("data")

    def _remember(self, products: list[dict]) -> None:
        for p in products:
            if p.get("product_id"):
                self.product_names.setdefault(int(p["product_id"]), p.get("name", ""))

    # ---------- thực thi ----------

    def run(self, name: str, args: dict) -> str:
        self.used.append(name)
        handler = getattr(self, "_" + name, None)
        if handler is None:
            return json.dumps({"error": "UNKNOWN_TOOL"})
        needs_login = name in ("get_my_orders", "get_order_detail", "get_my_cart", "get_my_service_requests",
                               "propose_add_to_cart", "propose_cancel_order")
        if needs_login and not self.access_token:
            return json.dumps(LOGIN_REQUIRED, ensure_ascii=False)
        try:
            result = handler(**args)
        except BackendError as error:
            if error.status in (401, 403):
                result = LOGIN_REQUIRED
            elif error.status == 404:
                result = {"error": "NOT_FOUND", "message": "Không tìm thấy dữ liệu (hoặc không thuộc tài khoản này)."}
            else:
                result = {"error": error.code}
        except (httpx.HTTPError, TypeError, ValueError) as error:
            log.warning("Tool %s failed: %s", name, error)
            result = {"error": "TOOL_FAILED", "message": "Hệ thống tạm thời không lấy được dữ liệu."}
        output = json.dumps(result, ensure_ascii=False, default=str)
        self.trace.append({"tool": name, "input": args, "output": output})
        return output

    def _search_policies(self, query: str):
        hits = rag.search(query, k=config.RAG_POLICY_K, doc_type="POLICY")
        if not hits:
            return {"results": [], "note": "Không tìm thấy chính sách liên quan trong kho tri thức."}
        return {"results": [{"title": h["title"], "content": h["content"], "similarity": h["similarity"]} for h in hits]}

    def _search_products(self, query: str, category: str | None = None, brand: str | None = None,
                         min_price: int | None = None, max_price: int | None = None, limit: int = 5):
        hits = rag.search(query, k=max(1, min(int(limit or 5), 8)), doc_type="PRODUCT", min_similarity=0.78,
                          category=category, brand=brand, min_price=min_price, max_price=max_price)
        products = [{"product_id": h["product_id"], "name": h["title"], "category": h["metadata"].get("category"),
                     "brand": h["metadata"].get("brand"), "price_from": int(h["metadata"].get("price", 0)),
                     "summary": h["content"][:400]} for h in hits]
        self._remember(products)
        return {"products": products} if products else {"products": [], "note": "Không có sản phẩm phù hợp."}

    def _get_product_details(self, product_id: int):
        base = f"/products/{int(product_id)}"
        product = self._call("GET", base)
        variants = self._call("GET", base + "/variants") or []
        specs = self._call("GET", base + "/specifications") or []
        self._remember([{"product_id": product["id"], "name": product["name"]}])
        return {
            "product_id": product["id"], "name": product["name"], "category": product.get("categoryName"),
            "brand": product.get("brandName"), "price": product.get("effectivePrice"),
            "base_price": product.get("basePrice"), "promotion": product.get("activePromotionName"),
            "warranty_months": product.get("warrantyMonths"), "is_active": product.get("isActive"),
            "variants": [{"name": v.get("variantName"), "price": v.get("discountPrice") or v.get("price"),
                          "color": v.get("color"), "storage": v.get("storage")} for v in variants][:12],
            "specifications": {s["specName"]: s["specValue"][:120] for s in specs[:20]},
        }

    def _recommend_products(self, type: str, product_id: int | None = None):
        body = {"type": type, "productId": product_id, "size": 5}
        data = self._call("POST", "/recommendations", auth=bool(self.access_token), json=body)
        products = [{"product_id": item["product"]["id"], "name": item["product"]["name"],
                     "price": item["product"].get("effectivePrice"), "reason": item.get("reason")}
                    for item in data.get("items", [])]
        self._remember(products)
        return {"products": products, "source": data.get("source")}

    def _get_my_orders(self):
        page = self._call("GET", "/orders?page=0&size=10", auth=True)
        return {"orders": [self._order_summary(o) for o in page.get("content", [])]}

    def _get_order_detail(self, order: str):
        key = str(order).strip().lstrip("#")
        if key.isdigit():
            return self._order_summary(self._call("GET", f"/orders/{int(key)}", auth=True), detailed=True)
        page = self._call("GET", "/orders?page=0&size=50", auth=True)
        for o in page.get("content", []):
            if str(o.get("code", "")).lower() == key.lower():
                return self._order_summary(o, detailed=True)
        raise BackendError(404, "ORDER_NOT_FOUND")

    @staticmethod
    def _order_summary(o: dict, detailed: bool = False) -> dict:
        summary = {"id": o["id"], "code": o.get("code"), "status": o.get("status"), "order_date": o.get("orderDate"),
                   "total": o.get("total"), "payment_method": o.get("paymentMethod"),
                   "items": [f"{i.get('productName')} ({i.get('variantName')}) x{i.get('quantity', 1)}"
                             for i in o.get("items", [])]}
        if detailed:
            summary.update({"delivery_type": o.get("deliveryType"), "shipping_fee": o.get("shippingFee"),
                            "shipping_address": o.get("shippingAddress"), "tracking_number": o.get("trackingNumber"),
                            "delivered_at": o.get("deliveredAt"), "cancelled_at": o.get("cancelledAt"),
                            "payment_status": (o.get("payment") or {}).get("status")})
        return summary

    def _get_my_cart(self):
        cart = self._call("GET", "/cart", auth=True)
        return {"items": [{"name": i.get("productName"), "variant": i.get("variantName"), "quantity": i.get("quantity"),
                           "unit_price": i.get("unitPrice"), "line_total": i.get("lineTotal")}
                          for i in cart.get("items", [])],
                "subtotal": cart.get("subtotal"), "shipping_fee": cart.get("shippingFee"), "total": cart.get("total")}

    def _get_my_service_requests(self):
        page = self._call("GET", "/service-requests/mine?page=0&size=10", auth=True)
        rows = page.get("content", []) if isinstance(page, dict) else page
        return {"requests": [{"code": r.get("code"), "type": r.get("type"), "status": r.get("status"),
                              "order_code": r.get("orderCode"), "store": r.get("storeName"),
                              "products": [i.get("productName") for i in r.get("items") or []],
                              "estimated_cost": r.get("estimatedCost"), "notes": r.get("notes"),
                              "created_at": r.get("createdAt")} for r in rows]}

    def _propose_add_to_cart(self, product_id: int | None = None, product_name: str | None = None,
                             variant_name: str | None = None, quantity: int = 1):
        if not product_id:
            if not product_name:
                return {"error": "MISSING_PRODUCT", "message": "Hỏi khách muốn thêm sản phẩm nào."}
            hits = rag.search(product_name, k=1, doc_type="PRODUCT", min_similarity=0.85)
            if not hits:
                return {"error": "NOT_FOUND", "message": f"Không tìm thấy sản phẩm '{product_name}'."}
            product_id = hits[0]["product_id"]
        product = self._call("GET", f"/products/{int(product_id)}")
        if product.get("isActive") is False or not product.get("effectivePrice"):
            return {"error": "NOT_AVAILABLE", "message": "Sản phẩm này hiện không bán."}
        variants = self._call("GET", f"/products/{int(product_id)}/variants") or []
        if not variants:
            return {"error": "NOT_AVAILABLE", "message": "Sản phẩm chưa có phiên bản để đặt mua."}
        chosen = variants[0] if len(variants) == 1 else None
        if chosen is None and variant_name:
            wanted = variant_name.strip().lower()
            matches = [v for v in variants if wanted in str(v.get("variantName", "")).lower()]
            chosen = matches[0] if len(matches) == 1 else None
        if chosen is None:
            return {"status": "NEED_VARIANT", "message": "Hỏi khách chọn một phiên bản.",
                    "variants": [v.get("variantName") for v in variants][:12]}
        amount = max(1, min(int(quantity or 1), MAX_CART_QUANTITY))
        label = f"Thêm {product['name']} ({chosen.get('variantName')}) × {amount} vào giỏ"
        self.actions.append({"type": "ADD_TO_CART", "label": label, "variantId": chosen["id"], "quantity": amount})
        self._remember([{"product_id": product["id"], "name": product["name"]}])
        return {"status": WAITING, "action": label, "message": CONFIRM_HINT}

    def _propose_cancel_order(self, order: str):
        detail = self._get_order_detail(order)
        if detail.get("status") != "PENDING":
            return {"error": "CANNOT_CANCEL", "status": detail.get("status"),
                    "message": "Chỉ huỷ được đơn đang Chờ xác nhận; đơn khác vui lòng liên hệ cửa hàng."}
        label = f"Huỷ đơn {detail.get('code') or detail['id']}"
        self.actions.append({"type": "CANCEL_ORDER", "label": label, "orderId": detail["id"]})
        return {"status": WAITING, "action": label, "order": detail, "message": CONFIRM_HINT}

    def _find_stores(self, city: str | None = None):
        stores = self._call("GET", "/stores") or []
        if city:
            needle = city.lower()
            stores = [s for s in stores if needle in f"{s.get('city', '')} {s.get('district', '')}".lower()] or stores
        return {"stores": [{"name": s.get("name"), "address": s.get("address"), "district": s.get("district"),
                            "city": s.get("city"), "phone": s.get("phone")} for s in stores[:10]]}
