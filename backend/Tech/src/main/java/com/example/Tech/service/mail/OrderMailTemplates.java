package com.example.Tech.service.mail;

import com.example.Tech.entity.order.DeliveryType;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.mapper.order.OrderMapper;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Builds the subject, HTML and plain-text body of an order mail. Only the order code, the products, the money and the
 * status go in: no phone number and no street address. Every dynamic value is HTML-escaped.
 */
final class OrderMailTemplates {

    private static final String BRAND = "POY";
    private static final String INK = "#111111";
    private static final String GOLD = "#a67c3d";

    private OrderMailTemplates() {
    }

    static MailMessage build(MailKind kind, Order order, String to, String trackUrl) {
        String code = OrderMapper.code(order.getId());
        Copy copy = copyFor(kind, order, code);
        String subject = "[" + BRAND + "] " + copy.subject;
        return new MailMessage(to, subject, html(copy, order, code, trackUrl), text(copy, order, code, trackUrl));
    }

    private record Copy(String subject, String title, String intro, String note) {
    }

    private static Copy copyFor(MailKind kind, Order order, String code) {
        String name = order.getRecipientName();
        boolean pickup = order.getDeliveryType() == DeliveryType.PICKUP;
        return switch (kind) {
            case ORDER_PLACED -> new Copy("Cảm ơn bạn đã đặt hàng – Đơn " + code,
                    "Cảm ơn bạn đã đặt hàng tại POY",
                    "Xin chào " + name + ", POY đã nhận được đơn hàng " + code + " của bạn.",
                    order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER || order.getPaymentMethod() == PaymentMethod.CARD
                            ? "Đơn hàng sẽ được xử lý ngay khi chúng tôi nhận được thanh toán. Bạn có thể xem đơn tại trang theo dõi."
                            : "Chúng tôi sẽ sớm xác nhận đơn và thông báo cho bạn ở các bước tiếp theo.");
            case PAYMENT_RECEIVED -> new Copy("Chúng tôi đã nhận thanh toán đơn " + code,
                    "Cảm ơn bạn đã đặt hàng tại POY",
                    "Xin chào " + name + ", POY đã nhận được thanh toán cho đơn hàng " + code + ". Cảm ơn bạn đã đặt hàng!",
                    "Đơn hàng sẽ được xác nhận và chuẩn bị giao trong thời gian sớm nhất.");
            case ORDER_CONFIRMED -> new Copy("Đơn hàng " + code + " đã được xác nhận",
                    "Đơn hàng đã được xác nhận",
                    "Xin chào " + name + ", đơn hàng " + code + " của bạn đã được xác nhận và đang được chuẩn bị.",
                    "Chúng tôi sẽ gửi email khi đơn hàng " + (pickup ? "sẵn sàng tại cửa hàng." : "được vận chuyển."));
            case ORDER_SHIPPING -> new Copy("Đơn hàng " + code + " của bạn đang được vận chuyển",
                    "Đơn hàng đang được vận chuyển",
                    "Xin chào " + name + ", đơn hàng " + code + " của bạn đã được giao cho đơn vị vận chuyển.",
                    (order.getTrackingNumber() == null ? "" : "Mã vận đơn: " + order.getTrackingNumber() + ". ")
                            + "Vui lòng giữ điện thoại để nhận cuộc gọi của người giao hàng.");
            case READY_FOR_PICKUP -> new Copy("Đơn hàng " + code + " đã sẵn sàng tại cửa hàng",
                    "Đơn hàng đã sẵn sàng tại cửa hàng",
                    "Xin chào " + name + ", đơn hàng " + code + " của bạn đã sẵn sàng"
                            + (order.getStore() != null ? " tại " + order.getStore().getName() : " tại cửa hàng") + ".",
                    "Vui lòng mang theo mã đơn " + code + " và số điện thoại đặt hàng khi đến nhận.");
            case ORDER_DELIVERED -> new Copy("Đơn hàng " + code + " đã giao thành công",
                    "Giao hàng thành công",
                    "Xin chào " + name + ", đơn hàng " + code + " đã được giao thành công. Cảm ơn bạn đã mua sắm tại POY!",
                    order.getPointsEarned() > 0
                            ? "Bạn vừa được cộng " + order.getPointsEarned() + " điểm tích luỹ. Hãy chia sẻ cảm nhận của bạn về sản phẩm nhé."
                            : "Hãy chia sẻ cảm nhận của bạn về sản phẩm nhé.");
            case ORDER_CANCELLED -> new Copy("Đơn hàng " + code + " đã được huỷ",
                    "Đơn hàng đã được huỷ",
                    "Xin chào " + name + ", đơn hàng " + code + " đã được huỷ.",
                    "Voucher và điểm tích luỹ (nếu có) đã được hoàn lại. Nếu bạn đã thanh toán, POY sẽ hoàn tiền theo hình thức thanh toán ban đầu.");
        };
    }

    // ---------- HTML ----------

    private static String html(Copy copy, Order order, String code, String trackUrl) {
        StringBuilder rows = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            rows.append("<tr><td style=\"padding:10px 0;border-bottom:1px solid #eee\">")
                    .append("<div style=\"font-weight:600\">").append(esc(productName(item))).append("</div>")
                    .append("<div style=\"color:#777;font-size:13px\">Số lượng: ").append(item.getQuantity()).append("</div>")
                    .append("</td><td align=\"right\" style=\"padding:10px 0;border-bottom:1px solid #eee;white-space:nowrap\">")
                    .append(money(item.getSubtotal())).append("</td></tr>");
        }
        StringBuilder sums = new StringBuilder();
        sumRow(sums, "Phí vận chuyển", order.getShippingCost().signum() == 0 ? "Miễn phí" : money(order.getShippingCost()), false);
        if (order.getComboDiscount().signum() > 0) {
            sumRow(sums, "Giảm combo", "-" + money(order.getComboDiscount()), false);
        }
        if (order.getVoucherDiscount().signum() > 0) {
            sumRow(sums, "Voucher" + (order.getVoucherCode() == null ? "" : " " + esc(order.getVoucherCode())),
                    "-" + money(order.getVoucherDiscount()), false);
        }
        if (order.getPointsDiscount().signum() > 0) {
            sumRow(sums, "Dùng điểm tích luỹ", "-" + money(order.getPointsDiscount()), false);
        }
        sumRow(sums, "Tổng cộng", money(order.getTotalAmount()), true);

        return "<!doctype html><html lang=\"vi\"><body style=\"margin:0;background:#f4f1ec;font-family:Arial,Helvetica,sans-serif;color:" + INK + "\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\" style=\"padding:24px 12px\">"
                + "<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;width:100%;background:#fff;border-radius:12px;overflow:hidden\">"
                + "<tr><td style=\"background:" + INK + ";padding:22px 28px;color:#fff;font-size:26px;font-weight:700;letter-spacing:6px\">" + BRAND + "</td></tr>"
                + "<tr><td style=\"padding:28px\">"
                + "<h1 style=\"margin:0 0 12px;font-size:22px\">" + esc(copy.title) + "</h1>"
                + "<p style=\"margin:0 0 8px;line-height:1.55\">" + esc(copy.intro) + "</p>"
                + "<p style=\"margin:0 0 20px;line-height:1.55;color:#555\">" + esc(copy.note) + "</p>"
                + "<div style=\"background:#faf7f2;border-radius:8px;padding:12px 16px;margin-bottom:20px;font-size:14px\">"
                + "<div><b>Mã đơn:</b> " + esc(code) + "</div>"
                + "<div><b>Thanh toán:</b> " + esc(paymentLabel(order.getPaymentMethod())) + "</div>"
                + "<div><b>Nhận hàng:</b> " + esc(receiving(order)) + "</div></div>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"font-size:14px\">" + rows + "</table>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"font-size:14px;margin-top:8px\">" + sums + "</table>"
                + "<p style=\"text-align:center;margin:28px 0 8px\"><a href=\"" + esc(trackUrl) + "\" style=\"display:inline-block;background:" + INK
                + ";color:#fff;text-decoration:none;padding:13px 28px;border-radius:8px;font-weight:600\">Xem đơn hàng</a></p>"
                + "</td></tr>"
                + "<tr><td style=\"padding:16px 28px 24px;color:#888;font-size:12px;line-height:1.5;border-top:1px solid #eee\">"
                + "Bạn nhận được email này vì có đơn hàng đặt tại POY bằng địa chỉ email này. Nếu không phải bạn, vui lòng bỏ qua email."
                + "</td></tr></table></td></tr></table></body></html>";
    }

    private static void sumRow(StringBuilder out, String label, String value, boolean strong) {
        String style = strong ? "padding:10px 0 0;font-size:17px;font-weight:700;color:" + GOLD : "padding:4px 0;color:#555";
        out.append("<tr><td style=\"").append(style).append("\">").append(label).append("</td><td align=\"right\" style=\"")
                .append(style).append("\">").append(value).append("</td></tr>");
    }

    // ---------- plain text ----------

    private static String text(Copy copy, Order order, String code, String trackUrl) {
        StringBuilder out = new StringBuilder();
        out.append(BRAND).append("\n\n").append(copy.title).append("\n\n").append(copy.intro).append("\n").append(copy.note)
                .append("\n\nMã đơn: ").append(code)
                .append("\nThanh toán: ").append(paymentLabel(order.getPaymentMethod()))
                .append("\nNhận hàng: ").append(receiving(order)).append("\n\n");
        for (OrderItem item : order.getItems()) {
            out.append("- ").append(productName(item)).append(" x").append(item.getQuantity()).append(": ")
                    .append(money(item.getSubtotal())).append("\n");
        }
        out.append("\nPhí vận chuyển: ").append(order.getShippingCost().signum() == 0 ? "Miễn phí" : money(order.getShippingCost()));
        if (order.getComboDiscount().signum() > 0) {
            out.append("\nGiảm combo: -").append(money(order.getComboDiscount()));
        }
        if (order.getVoucherDiscount().signum() > 0) {
            out.append("\nVoucher: -").append(money(order.getVoucherDiscount()));
        }
        if (order.getPointsDiscount().signum() > 0) {
            out.append("\nDùng điểm tích luỹ: -").append(money(order.getPointsDiscount()));
        }
        out.append("\nTổng cộng: ").append(money(order.getTotalAmount()))
                .append("\n\nXem đơn hàng: ").append(trackUrl)
                .append("\n\nBạn nhận được email này vì có đơn hàng đặt tại POY bằng địa chỉ email này. Nếu không phải bạn, vui lòng bỏ qua email.\n");
        return out.toString();
    }

    // ---------- helpers ----------

    private static String productName(OrderItem item) {
        String product = item.getVariant().getProduct().getName();
        String variant = item.getVariant().getVariantName();
        return variant == null || variant.isBlank() ? product : product + " - " + variant;
    }

    /** Only the area (ward, province) or the store: never the street or the phone. */
    private static String receiving(Order order) {
        if (order.getDeliveryType() == DeliveryType.PICKUP) {
            return "Nhận tại cửa hàng" + (order.getStore() != null ? " " + order.getStore().getName() : "");
        }
        String area = String.join(", ", Stream.of(order.getShippingWard(), order.getShippingProvince())
                .filter(part -> part != null && !part.isBlank()).toList());
        return area.isEmpty() ? "Giao tận nhà" : "Giao tận nhà – " + area;
    }

    private static String paymentLabel(PaymentMethod method) {
        if (method == null) {
            return "";
        }
        return switch (method) {
            case COD -> "Thanh toán khi nhận hàng (COD)";
            case BANK_TRANSFER -> "Chuyển khoản ngân hàng (QR)";
            case INSTALLMENT -> "Trả góp";
            case CARD -> "Thẻ quốc tế";
        };
    }

    static String money(BigDecimal amount) {
        return new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.GERMANY)).format(amount) + "đ";
    }

    private static String esc(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
