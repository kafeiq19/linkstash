package com.linkstash.backend;

import com.linkstash.backend.service.MetadataParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T5: HTML metadata parsing unit test on a static fixture (no network).
 */
class MetadataParserTest {

    private final MetadataParser parser = new MetadataParser();

    private static final String FIXTURE = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <title>Fallback Title</title>
              <meta property="og:title" content="OG Title Wins">
              <meta property="og:description" content="OG description text">
              <meta property="og:site_name" content="Example Site">
              <meta name="description" content="Plain meta description">
              <link rel="icon" href="/static/icon-32.png">
            </head>
            <body>
              <h1>Visible heading</h1>
            </body>
            </html>
            """;

    @Test
    void parsesOgTitleDescriptionFaviconAndSiteName() {
        MetadataParser.PageMetadata meta = parser.parse(FIXTURE, "https://example.com/posts/1");

        assertThat(meta.getTitle()).isEqualTo("OG Title Wins");
        assertThat(meta.getDescription()).isEqualTo("OG description text");
        assertThat(meta.getFavicon()).isEqualTo("https://example.com/static/icon-32.png");
        assertThat(meta.getSiteName()).isEqualTo("Example Site");
    }

    @Test
    void fallsBackToTitleMetaDescriptionAndHost() {
        String html = """
                <html><head>
                <title>Plain Page Title</title>
                <meta name="description" content="Only meta description">
                </head><body></body></html>
                """;
        MetadataParser.PageMetadata meta = parser.parse(html, "https://blog.example.org/a");

        assertThat(meta.getTitle()).isEqualTo("Plain Page Title");
        assertThat(meta.getDescription()).isEqualTo("Only meta description");
        assertThat(meta.getSiteName()).isEqualTo("blog.example.org");
        // default /favicon.ico fallback
        assertThat(meta.getFavicon()).isEqualTo("https://blog.example.org/favicon.ico");
    }

    @Test
    void resolvesAbsoluteFaviconAndShortcutIcon() {
        String html = """
                <html><head>
                <link rel="shortcut icon" href="https://cdn.example.com/favicon.png">
                <meta property="og:title" content="With Absolute Icon">
                </head><body></body></html>
                """;
        MetadataParser.PageMetadata meta = parser.parse(html, "https://example.com/x");

        assertThat(meta.getTitle()).isEqualTo("With Absolute Icon");
        assertThat(meta.getFavicon()).isEqualTo("https://cdn.example.com/favicon.png");
        assertThat(meta.getSiteName()).isEqualTo("example.com");
    }

    @Test
    void prefersOgOverMetaDescription() {
        String html = """
                <html><head>
                <meta property="og:description" content="og desc">
                <meta name="description" content="meta desc">
                </head><body></body></html>
                """;
        MetadataParser.PageMetadata meta = parser.parse(html, "https://example.com/");

        assertThat(meta.getDescription()).isEqualTo("og desc");
    }
}
