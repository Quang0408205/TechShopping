package com.example.Tech.service.mail;

import org.springframework.web.util.HtmlUtils;

import java.util.List;

/** Subject, HTML and plain-text body of the "back in stock" mail. Dynamic values are HTML-escaped. */
public final class StockMailTemplates {

    private static final String INK = "#111111";

    private StockMailTemplates() {
    }

    public record Item(Long productId, String productName, String variantName) {

        String title() {
            return variantName == null || variantName.isBlank() ? productName : productName + " - " + variantName;
        }
    }

    static MailMessage build(String fullname, String to, List<Item> items, String siteUrl) {
        String subject = items.size() == 1
                ? "[POY] " + items.get(0).title() + " đã có hàng trở lại"
                : "[POY] " + items.size() + " sản phẩm bạn quan tâm đã có hàng trở lại";
        StringBuilder rows = new StringBuilder();
        StringBuilder text = new StringBuilder("POY\n\nXin chào " + fullname + ", sản phẩm bạn đã đăng ký theo dõi vừa có hàng:\n\n");
        for (Item item : items) {
            String url = siteUrl + "/customer/product-detail.html?id=" + item.productId();
            rows.append("<tr><td style=\"padding:12px 0;border-bottom:1px solid #eee\"><a href=\"").append(esc(url))
                    .append("\" style=\"color:").append(INK).append(";font-weight:600;text-decoration:none\">")
                    .append(esc(item.title())).append("</a></td></tr>");
            text.append("- ").append(item.title()).append("\n  ").append(url).append("\n");
        }
        text.append("\nSố lượng có hạn, mua sớm để không lỡ.\n\nBạn nhận được email này một lần vì đã bấm \"Báo tôi khi có hàng\". "
                + "Đăng ký đã được xoá sau email này.\n");
        String html = "<!doctype html><html lang=\"vi\"><body style=\"margin:0;background:#f4f1ec;font-family:Arial,Helvetica,sans-serif;color:" + INK + "\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\" style=\"padding:24px 12px\">"
                + "<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;width:100%;background:#fff;border-radius:12px;overflow:hidden\">"
                + "<tr><td style=\"background:" + INK + ";padding:22px 28px;color:#fff;font-size:26px;font-weight:700;letter-spacing:6px\">POY</td></tr>"
                + "<tr><td style=\"padding:28px\"><h1 style=\"margin:0 0 12px;font-size:22px\">Sản phẩm bạn chờ đã có hàng</h1>"
                + "<p style=\"margin:0 0 20px;line-height:1.55\">Xin chào " + esc(fullname) + ", những sản phẩm bạn đăng ký theo dõi vừa có hàng trở lại:</p>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"font-size:14px\">" + rows + "</table>"
                + "<p style=\"margin:20px 0 0;color:#555;line-height:1.55\">Số lượng có hạn, mua sớm để không lỡ.</p></td></tr>"
                + "<tr><td style=\"padding:16px 28px 24px;color:#888;font-size:12px;line-height:1.5;border-top:1px solid #eee\">"
                + "Bạn nhận được email này một lần vì đã bấm \"Báo tôi khi có hàng\". Đăng ký đã được xoá sau email này."
                + "</td></tr></table></td></tr></table></body></html>";
        return new MailMessage(to, subject, html, text.toString());
    }

    private static String esc(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
