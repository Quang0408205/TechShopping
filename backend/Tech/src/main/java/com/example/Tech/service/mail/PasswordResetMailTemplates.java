package com.example.Tech.service.mail;

import org.springframework.web.util.HtmlUtils;

/** Subject, HTML and plain-text body of the password reset mail. */
final class PasswordResetMailTemplates {

    static final String SUBJECT = "[POY] Đặt lại mật khẩu";

    private static final String INK = "#111111";

    private PasswordResetMailTemplates() {
    }

    static MailMessage build(String fullname, String to, String link, long validMinutes) {
        String text = "POY\n\nXin chào " + fullname + ",\n\nChúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. "
                + "Mở liên kết sau để đặt mật khẩu mới (hiệu lực " + validMinutes + " phút, dùng được một lần):\n\n" + link
                + "\n\nNếu không phải bạn yêu cầu, hãy bỏ qua email này: mật khẩu hiện tại vẫn giữ nguyên.\n";
        String html = "<!doctype html><html lang=\"vi\"><body style=\"margin:0;background:#f4f1ec;font-family:Arial,Helvetica,sans-serif;color:" + INK + "\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\" style=\"padding:24px 12px\">"
                + "<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;width:100%;background:#fff;border-radius:12px;overflow:hidden\">"
                + "<tr><td style=\"background:" + INK + ";padding:22px 28px;color:#fff;font-size:26px;font-weight:700;letter-spacing:6px\">POY</td></tr>"
                + "<tr><td style=\"padding:28px\"><h1 style=\"margin:0 0 12px;font-size:22px\">Đặt lại mật khẩu</h1>"
                + "<p style=\"margin:0 0 20px;line-height:1.55\">Xin chào " + esc(fullname) + ", chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. "
                + "Liên kết có hiệu lực " + validMinutes + " phút và chỉ dùng được một lần.</p>"
                + "<p style=\"text-align:center;margin:28px 0 8px\"><a href=\"" + esc(link) + "\" style=\"display:inline-block;background:" + INK
                + ";color:#fff;text-decoration:none;padding:13px 28px;border-radius:8px;font-weight:600\">Đặt mật khẩu mới</a></p>"
                + "<p style=\"margin:20px 0 0;font-size:12px;color:#777;word-break:break-all\">Nếu nút không mở được, sao chép liên kết này vào trình duyệt:<br>" + esc(link) + "</p></td></tr>"
                + "<tr><td style=\"padding:16px 28px 24px;color:#888;font-size:12px;line-height:1.5;border-top:1px solid #eee\">"
                + "Nếu không phải bạn yêu cầu, hãy bỏ qua email này: mật khẩu hiện tại vẫn giữ nguyên.</td></tr></table></td></tr></table></body></html>";
        return new MailMessage(to, SUBJECT, html, text);
    }

    private static String esc(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
