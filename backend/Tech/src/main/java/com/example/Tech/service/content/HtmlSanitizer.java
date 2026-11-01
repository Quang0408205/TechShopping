package com.example.Tech.service.content;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps only a short list of formatting tags from HTML typed by an ADMIN. Everything else is dropped or shown as
 * text: scripts and styles with their content, event attributes (nothing but {@code href}, {@code src}, {@code alt}
 * survives), links other than http(s) / mailto / tel / same-site paths, and unbalanced tags (closed again at the end).
 */
public final class HtmlSanitizer {

    private static final Set<String> ALLOWED = Set.of("p", "br", "hr", "strong", "b", "em", "i", "u", "ul", "ol", "li",
            "h2", "h3", "h4", "blockquote", "a", "img", "table", "thead", "tbody", "tr", "th", "td");

    private static final Set<String> VOID = Set.of("br", "hr", "img");

    private static final Pattern DROPPED_BLOCKS = Pattern.compile(
            "<(script|style|iframe|object|embed|svg|math|template|noscript)\\b.*?(</\\1\\s*>|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern COMMENTS = Pattern.compile("<!--.*?(-->|$)", Pattern.DOTALL);

    private static final Pattern TAG = Pattern.compile("<(/?)([a-zA-Z][a-zA-Z0-9]*)((?:\"[^\"]*\"|'[^']*'|[^'\">])*)>");

    private static final Pattern ATTRIBUTE = Pattern.compile(
            "([a-zA-Z][a-zA-Z0-9_:-]*)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'>]+))");

    private static final Pattern ENTITY = Pattern.compile("&(?:amp|lt|gt|quot|nbsp|#\\d{1,6});");

    private HtmlSanitizer() {
    }

    public static String clean(String html) {
        if (html == null) {
            return "";
        }
        String source = COMMENTS.matcher(DROPPED_BLOCKS.matcher(html).replaceAll("")).replaceAll("");
        StringBuilder out = new StringBuilder(source.length());
        Deque<String> open = new ArrayDeque<>();
        Matcher matcher = TAG.matcher(source);
        int last = 0;
        while (matcher.find()) {
            appendText(out, source.substring(last, matcher.start()));
            last = matcher.end();
            String name = matcher.group(2).toLowerCase(Locale.ROOT);
            if (!ALLOWED.contains(name)) {
                continue;
            }
            if (!matcher.group(1).isEmpty()) {
                closeTag(out, open, name);
            } else {
                openTag(out, open, name, matcher.group(3));
            }
        }
        appendText(out, source.substring(last));
        while (!open.isEmpty()) {
            out.append("</").append(open.pop()).append('>');
        }
        return out.toString().trim();
    }

    private static void openTag(StringBuilder out, Deque<String> open, String name, String attributes) {
        if ("a".equals(name)) {
            String href = safeLink(attribute(attributes, "href"));
            if (href == null) {
                return;
            }
            out.append("<a href=\"").append(escapeAttribute(href)).append('"');
            if (href.startsWith("http")) {
                out.append(" target=\"_blank\"");
            }
            out.append(" rel=\"noopener noreferrer\">");
            open.push(name);
            return;
        }
        if ("img".equals(name)) {
            String src = attribute(attributes, "src");
            if (src == null || !(src.startsWith("https://") || src.startsWith("http://") || src.startsWith("/uploads/"))) {
                return;
            }
            String alt = attribute(attributes, "alt");
            out.append("<img src=\"").append(escapeAttribute(src.strip())).append("\" alt=\"")
                    .append(escapeAttribute(alt == null ? "" : alt)).append("\" loading=\"lazy\">");
            return;
        }
        if (VOID.contains(name)) {
            out.append('<').append(name).append('>');
            return;
        }
        out.append('<').append(name).append('>');
        open.push(name);
    }

    private static void closeTag(StringBuilder out, Deque<String> open, String name) {
        if (VOID.contains(name) || !open.contains(name)) {
            return;
        }
        while (!open.isEmpty()) {
            String top = open.pop();
            out.append("</").append(top).append('>');
            if (top.equals(name)) {
                return;
            }
        }
    }

    private static String attribute(String attributes, String wanted) {
        Matcher matcher = ATTRIBUTE.matcher(attributes);
        while (matcher.find()) {
            if (matcher.group(1).equalsIgnoreCase(wanted)) {
                String value = matcher.group(2) != null ? matcher.group(2)
                        : matcher.group(3) != null ? matcher.group(3) : matcher.group(4);
                return value.replace("&amp;", "&");
            }
        }
        return null;
    }

    /** http(s), mailto, tel, an in-page anchor or a path of this site; anything else (javascript:, data:, //host) is refused. */
    private static String safeLink(String raw) {
        if (raw == null) {
            return null;
        }
        String href = raw.strip();
        String probe = href.replaceAll("[\\x00-\\x20]", "").toLowerCase(Locale.ROOT);
        boolean ok = probe.startsWith("https://") || probe.startsWith("http://") || probe.startsWith("mailto:")
                || probe.startsWith("tel:") || probe.startsWith("#")
                || (probe.startsWith("/") && !probe.startsWith("//"));
        return ok && probe.equals(href.toLowerCase(Locale.ROOT)) ? href : null;
    }

    private static void appendText(StringBuilder out, String text) {
        int index = 0;
        while (index < text.length()) {
            char c = text.charAt(index);
            if (c == '&') {
                Matcher entity = ENTITY.matcher(text);
                if (entity.find(index) && entity.start() == index) {
                    out.append(entity.group());
                    index = entity.end();
                    continue;
                }
                out.append("&amp;");
            } else if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else {
                out.append(c);
            }
            index++;
        }
    }

    private static String escapeAttribute(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
