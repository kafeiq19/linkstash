package com.linkstash.backend.service;

import com.linkstash.backend.entity.Bookmark;
import com.linkstash.backend.mapper.BookmarkMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Fetches page metadata asynchronously after bookmark creation.
 * Never throws to callers; failures leave placeholder metadata as-is.
 */
@Service
public class MetadataService {

    private static final Logger log = LoggerFactory.getLogger(MetadataService.class);

    private final BookmarkMapper bookmarkMapper;
    private final MetadataParser metadataParser;
    private final int timeoutMs;

    public MetadataService(BookmarkMapper bookmarkMapper,
                           MetadataParser metadataParser,
                           @Value("${linkstash.metadata.timeout-ms:5000}") int timeoutMs) {
        this.bookmarkMapper = bookmarkMapper;
        this.metadataParser = metadataParser;
        this.timeoutMs = timeoutMs;
    }

    @Async
    public void fetchAndFillAsync(Long bookmarkId, String url) {
        try {
            fetchAndFill(bookmarkId, url);
        } catch (Exception e) {
            log.warn("metadata fetch failed for bookmark {}: {}", bookmarkId, e.toString());
        }
    }

    void fetchAndFill(Long bookmarkId, String url) throws Exception {
        Document doc = Jsoup.connect(url)
                .userAgent("Linkstash/0.1 (+https://github.com/kafeiq19/linkstash)")
                .timeout(timeoutMs)
                .maxBodySize(1024 * 1024)
                .followRedirects(true)
                .get();

        MetadataParser.PageMetadata meta = metadataParser.parse(doc.html(), url);

        Bookmark existing = bookmarkMapper.selectById(bookmarkId);
        if (existing == null) {
            return;
        }

        String title = isBlank(existing.getTitle()) ? meta.getTitle() : existing.getTitle();
        String description = isBlank(existing.getDescription()) ? meta.getDescription() : existing.getDescription();
        String favicon = isBlank(existing.getFavicon()) ? meta.getFavicon() : existing.getFavicon();
        String siteName = isBlank(existing.getSiteName()) ? meta.getSiteName() : existing.getSiteName();

        bookmarkMapper.updateMetadata(
                bookmarkId,
                title,
                description,
                favicon,
                siteName,
                Instant.now().toString());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
