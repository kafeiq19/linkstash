package com.linkstash.backend.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * Pure HTML → metadata parser (no network). Used by MetadataService after fetch.
 */
@Component
public class MetadataParser {

    public static class PageMetadata {
        private final String title;
        private final String description;
        private final String favicon;
        private final String siteName;

        public PageMetadata(String title, String description, String favicon, String siteName) {
            this.title = title;
            this.description = description;
            this.favicon = favicon;
            this.siteName = siteName;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getFavicon() {
            return favicon;
        }

        public String getSiteName() {
            return siteName;
        }
    }

    /**
     * @param html    raw or parsed HTML text
     * @param pageUrl absolute URL of the page (used to resolve relative favicon / host)
     */
    public PageMetadata parse(String html, String pageUrl) {
        Document doc = Jsoup.parse(html == null ? "" : html, pageUrl == null ? "" : pageUrl);

        String title = firstNonBlank(
                metaContent(doc, "property", "og:title"),
                metaContent(doc, "name", "twitter:title"),
                doc.title()
        );

        // og:description first, then <meta name="description">
        String description = firstNonBlank(
                metaContent(doc, "property", "og:description"),
                metaContent(doc, "name", "description"),
                metaContent(doc, "name", "twitter:description")
        );

        String favicon = resolveFavicon(doc, pageUrl);
        String siteName = firstNonBlank(
                metaContent(doc, "property", "og:site_name"),
                hostOf(pageUrl)
        );

        return new PageMetadata(title, description, favicon, siteName);
    }

    private String resolveFavicon(Document doc, String pageUrl) {
        Elements links = doc.select("link[rel~=(?i)(^|\\s)(shortcut )?icon($|\\s)]");
        if (links.isEmpty()) {
            links = doc.select("link[rel~=icon]");
        }
        for (Element link : links) {
            String abs = link.absUrl("href");
            if (abs != null && !abs.isBlank()) {
                return abs;
            }
            String href = link.attr("href");
            if (href != null && !href.isBlank()) {
                return toAbsolute(pageUrl, href);
            }
        }
        // fallback: /favicon.ico on the origin
        try {
            URI uri = URI.create(pageUrl);
            if (uri.getScheme() != null && uri.getHost() != null) {
                return uri.getScheme() + "://" + uri.getHost()
                        + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
                        + "/favicon.ico";
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private String toAbsolute(String pageUrl, String href) {
        try {
            return URI.create(pageUrl).resolve(href).toString();
        } catch (Exception e) {
            return href;
        }
    }

    private String hostOf(String pageUrl) {
        try {
            URI uri = URI.create(pageUrl);
            return uri.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private String metaContent(Document doc, String attr, String value) {
        Element el = doc.selectFirst("meta[" + attr + "=" + value + "]");
        if (el == null) {
            return null;
        }
        return blankToNull(el.attr("content"));
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
