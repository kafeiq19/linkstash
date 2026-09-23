package com.linkstash.backend.service;

import com.linkstash.backend.common.ApiException;
import com.linkstash.backend.dto.BookmarkCreateRequest;
import com.linkstash.backend.dto.BookmarkDto;
import com.linkstash.backend.dto.BookmarkUpdateRequest;
import com.linkstash.backend.dto.TagDto;
import com.linkstash.backend.entity.Bookmark;
import com.linkstash.backend.entity.Tag;
import com.linkstash.backend.mapper.BookmarkMapper;
import com.linkstash.backend.mapper.BookmarkTagMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BookmarkService {

    public static final Set<String> VALID_STATUSES = Set.of("unread", "read", "archived");

    private final BookmarkMapper bookmarkMapper;
    private final BookmarkTagMapper bookmarkTagMapper;
    private final TagService tagService;
    private final MetadataService metadataService;

    public BookmarkService(BookmarkMapper bookmarkMapper,
                           BookmarkTagMapper bookmarkTagMapper,
                           TagService tagService,
                           MetadataService metadataService) {
        this.bookmarkMapper = bookmarkMapper;
        this.bookmarkTagMapper = bookmarkTagMapper;
        this.tagService = tagService;
        this.metadataService = metadataService;
    }

    public record PageResult(long total, List<BookmarkDto> items) {
    }

    public PageResult list(Long userId, int page, int size, String status, String tag, String q, Boolean favorite) {
        validateStatus(status);
        // empty status = home inbox (unread+read, exclude archived)
        if (status == null || status.isBlank()) {
            status = "inbox";
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(Math.min(size, 100), 1);
        int offset = (safePage - 1) * safeSize;

        long total = bookmarkMapper.countFiltered(userId, emptyToNull(status), favorite, escapeLike(emptyToNull(q)), emptyToNull(tag));
        List<Bookmark> bookmarks = bookmarkMapper.selectFiltered(
                userId, emptyToNull(status), favorite, escapeLike(emptyToNull(q)), emptyToNull(tag), safeSize, offset);
        return new PageResult(total, toDtos(bookmarks));
    }

    @Transactional
    public BookmarkDto create(Long userId, BookmarkCreateRequest req) {
        if (req == null || req.getUrl() == null || req.getUrl().isBlank()) {
            throw ApiException.badRequest("url is required");
        }
        String url = req.getUrl().trim();
        validateUrl(url);

        Bookmark bookmark = new Bookmark();
        bookmark.setUserId(userId);
        bookmark.setUrl(url);
        bookmark.setTitle(req.getTitle());
        bookmark.setNote(req.getNote() == null ? "" : req.getNote());
        bookmark.setStatus("unread");
        bookmark.setFavorite(false);
        Instant now = Instant.now();
        bookmark.setCreatedAt(now.toString());
        bookmark.setUpdatedAt(now.toString());
        bookmarkMapper.insert(bookmark);

        List<Tag> tags = tagService.upsertByNames(userId, req.getTagNames());
        for (Tag tag : tags) {
            bookmarkTagMapper.insertIgnore(bookmark.getId(), tag.getId());
        }

        // metadata filled async after commit; never blocks or fails this request
        Long bookmarkId = bookmark.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    metadataService.fetchAndFillAsync(bookmarkId, url);
                }
            });
        } else {
            metadataService.fetchAndFillAsync(bookmarkId, url);
        }

        return toDto(bookmark, tags.stream()
                .map(t -> new TagDto(t.getId(), t.getName()))
                .toList());
    }

    public BookmarkDto get(Long userId, Long id) {
        Bookmark bookmark = requireOwned(userId, id);
        return toDto(bookmark, tagsOf(id));
    }

    @Transactional
    public BookmarkDto update(Long userId, Long id, BookmarkUpdateRequest req) {
        Bookmark bookmark = requireOwned(userId, id);
        if (req == null) {
            return toDto(bookmark, tagsOf(id));
        }
        if (req.getTitle() != null) {
            bookmark.setTitle(req.getTitle());
        }
        if (req.getNote() != null) {
            bookmark.setNote(req.getNote());
        }
        if (req.getStatus() != null) {
            validateStatus(req.getStatus());
            bookmark.setStatus(req.getStatus());
        }
        if (req.getFavorite() != null) {
            bookmark.setFavorite(req.getFavorite());
        }
        List<Tag> tags = null;
        if (req.getTagNames() != null) {
            tags = tagService.upsertByNames(userId, req.getTagNames());
            bookmarkTagMapper.deleteByBookmarkId(id);
            for (Tag tag : tags) {
                bookmarkTagMapper.insertIgnore(id, tag.getId());
            }
        }
        bookmark.setUpdatedAt(Instant.now().toString());
        bookmarkMapper.updateById(bookmark);
        return toDto(bookmark, tags != null
                ? tags.stream().map(t -> new TagDto(t.getId(), t.getName())).toList()
                : tagsOf(id));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        requireOwned(userId, id);
        bookmarkTagMapper.deleteByBookmarkId(id);
        bookmarkMapper.deleteById(id);
    }

    private Bookmark requireOwned(Long userId, Long id) {
        Bookmark bookmark = bookmarkMapper.selectById(id);
        if (bookmark == null || !userId.equals(bookmark.getUserId())) {
            throw ApiException.notFound("bookmark not found");
        }
        return bookmark;
    }

    private List<TagDto> tagsOf(Long bookmarkId) {
        return bookmarkTagMapper.selectTagsByBookmarkId(bookmarkId).stream()
                .map(t -> new TagDto(t.getId(), t.getName()))
                .toList();
    }

    private List<BookmarkDto> toDtos(List<Bookmark> bookmarks) {
        if (bookmarks.isEmpty()) {
            return List.of();
        }
        List<Long> ids = bookmarks.stream().map(Bookmark::getId).toList();
        Map<Long, List<TagDto>> tagMap = new HashMap<>();
        for (BookmarkTagMapper.TagLinkRow row : bookmarkTagMapper.selectTagsByBookmarkIds(ids)) {
            tagMap.computeIfAbsent(row.getBookmarkId(), k -> new ArrayList<>())
                    .add(new TagDto(row.getId(), row.getName()));
        }
        List<BookmarkDto> result = new ArrayList<>(bookmarks.size());
        for (Bookmark b : bookmarks) {
            result.add(toDto(b, tagMap.getOrDefault(b.getId(), List.of())));
        }
        return result;
    }

    private BookmarkDto toDto(Bookmark b, List<TagDto> tags) {
        BookmarkDto dto = new BookmarkDto();
        dto.setId(b.getId());
        dto.setUrl(b.getUrl());
        dto.setTitle(b.getTitle());
        dto.setDescription(b.getDescription());
        dto.setFavicon(b.getFavicon());
        dto.setSiteName(b.getSiteName());
        dto.setStatus(b.getStatus());
        dto.setFavorite(Boolean.TRUE.equals(b.getFavorite()));
        dto.setNote(b.getNote() == null ? "" : b.getNote());
        dto.setTags(tags == null ? List.of() : tags);
        dto.setCreatedAt(b.getCreatedAt());
        dto.setUpdatedAt(b.getUpdatedAt());
        return dto;
    }

    private void validateStatus(String status) {
        if (status != null && !status.isBlank() && !VALID_STATUSES.contains(status)) {
            throw ApiException.badRequest("status must be one of unread|read|archived");
        }
    }

    private void validateUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null || uri.getHost().isBlank()) {
                throw ApiException.badRequest("url must be a valid http(s) URL");
            }
        } catch (URISyntaxException e) {
            throw ApiException.badRequest("url must be a valid http(s) URL");
        }
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String escapeLike(String s) {
        if (s == null) {
            return null;
        }
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
