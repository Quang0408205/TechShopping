package com.example.Tech.service.mail;

import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/** Subject, HTML and plain-text body of the price-drop mail (plan v7 GĐ4). Dynamic values are HTML-escaped. */
public final class WishlistMailTemplates {

    private static final String INK = "#111111";
    private static final String GOLD = "#a67c3d";

    private WishlistMailTemplates() {
    }

    public record Drop(Long productId, String name, BigDecimal oldPrice, BigDecimal newPrice) {

        int percent() {
            if (oldPrice == null || oldPrice.signum() <= 0) {
                return 0;
            }
            return oldPrice.subtract(newPrice).multiply(BigDecimal.valueOf(100))
                    .divide(oldPrice, 0, RoundingMode.HALF_UP).intValue();
        }
    }

    static MailMessage build(String fullname, String to, List<Drop> drops, String siteUrl) {
        String subject = drops.size() == 1
                ? "[POY] " + drops.get(0).name() + " đang giảm giá"
                : "[POY] " + drops.size() + " sản phẩm bạn yêu thích đang giảm giá";
        String wishlistUrl = siteUrl + "/customer/wishlist.html";
        StringBuilder rows = new StringBuilder();
        StringBuilder text = new StringBuilder("POY\n\nXin chào " + fullname
                + ", sản phẩm bạn yêu thích vừa giảm giá:\n\n");
        for (Drop drop : drops) {
            String url = siteUrl + "/customer/product-detail.html?id=" + drop.productId();
            rows.append("<tr><td style=\"padding:12px 0;border-bottom:1px solid #eee\"><a href=\"").append(esc(url))
                    .append("\" style=\"color:").append(INK).append(";font-weight:600;text-decoration:none\">")
                    .append(esc(drop.name())).append("</a><div style=\"font-size:13px;color:#777;margin-top:4px\"><s>")
                    .append(money(drop.oldPrice()))
                    .append("</s></div></td><td align=\"right\" style=\"padding:12px 0;border-bottom:1px solid #eee;white-space:nowrap\">")
                    .append("<div style=\"font-weight:700;color:").append(GOLD).append("\">").append(money(drop.newPrice())).append("</div>")
                    .append("<div style=\"font-size:13px;color:#1b7a3a\">Giảm ").append(drop.percent()).append("%</div></td></tr>");
            text.append("- ").append(drop.name()).append(": ").append(money(drop.oldPrice())).append(" → ")
                    .append(money(drop.newPrice())).append(" (giảm ").append(drop.percent()).append("%)\n  ").append(url).append("\n");
        }
        text.append("\nXem danh sách yêu thích: ").append(wishlistUrl)
                .append("\n\nBạn nhận được email này vì đã lưu sản phẩm vào danh sách yêu thích. Bỏ yêu thích sản phẩm để không nhận thông báo.\n");
        String html = "<!doctype html><html lang=\"vi\"><body style=\"margin:0;background:#f4f1ec;font-family:Arial,Helvetica,sans-serif;color:" + INK + "\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\" style=\"padding:24px 12px\">"
                + "<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;width:100%;background:#fff;border-radius:12px;overflow:hidden\">"
                + "<tr><td style=\"background:" + INK + ";padding:22px 28px;color:#fff;font-size:26px;font-weight:700;letter-spacing:6px\">POY</td></tr>"
                + "<tr><td style=\"padding:28px\"><h1 style=\"margin:0 0 12px;font-size:22px\">Sản phẩm bạn yêu thích đang giảm giá</h1>"
                + "<p style=\"margin:0 0 20px;line-height:1.55\">Xin chào " + esc(fullname) + ", POY vừa giảm giá những sản phẩm bạn đã lưu:</p>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"font-size:14px\">" + rows + "</table>"
                + "<p style=\"text-align:center;margin:28px 0 8px\"><a href=\"" + esc(wishlistUrl) + "\" style=\"display:inline-block;background:" + INK
                + ";color:#fff;text-decoration:none;padding:13px 28px;border-radius:8px;font-weight:600\">Xem danh sách yêu thích</a></p></td></tr>"
                + "<tr><td style=\"padding:16px 28px 24px;color:#888;font-size:12px;line-height:1.5;border-top:1px solid #eee\">"
                + "Bạn nhận được email này vì đã lưu sản phẩm vào danh sách yêu thích. Bỏ yêu thích sản phẩm để không nhận thông báo."
                + "</td></tr></table></td></tr></table></body></html>";
        return new MailMessage(to, subject, html, text.toString());
    }

    private static String money(BigDecimal amount) {
        return new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.GERMANY)).format(amount) + "đ";
    }

    private static String esc(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
