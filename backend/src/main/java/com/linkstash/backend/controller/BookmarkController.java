package com.linkstash.backend.controller;

import com.linkstash.backend.common.ApiResponse;
import com.linkstash.backend.dto.BookmarkCreateRequest;
import com.linkstash.backend.dto.BookmarkDto;
import com.linkstash.backend.dto.BookmarkUpdateRequest;
import com.linkstash.backend.security.CurrentUser;
import com.linkstash.backend.service.BookmarkService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean favorite) {
        BookmarkService.PageResult result = bookmarkService.list(
                CurrentUser.id(), page, size, status, tag, q, favorite);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", result.total());
        data.put("items", result.items());
        return ApiResponse.ok(data);
    }

    @PostMapping
    public ApiResponse<BookmarkDto> create(@RequestBody BookmarkCreateRequest req) {
        return ApiResponse.ok(bookmarkService.create(CurrentUser.id(), req));
    }

    @GetMapping("/{id}")
    public ApiResponse<BookmarkDto> get(@PathVariable Long id) {
        return ApiResponse.ok(bookmarkService.get(CurrentUser.id(), id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<BookmarkDto> update(@PathVariable Long id,
                                           @RequestBody BookmarkUpdateRequest req) {
        return ApiResponse.ok(bookmarkService.update(CurrentUser.id(), id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Boolean>> delete(@PathVariable Long id) {
        bookmarkService.delete(CurrentUser.id(), id);
        return ApiResponse.ok(Map.of("ok", true));
    }
}
