package com.linkstash.backend.controller;

import com.linkstash.backend.common.ApiResponse;
import com.linkstash.backend.dto.TagDto;
import com.linkstash.backend.dto.TagRequest;
import com.linkstash.backend.security.CurrentUser;
import com.linkstash.backend.service.TagService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ApiResponse<List<TagDto>> list() {
        return ApiResponse.ok(tagService.list(CurrentUser.id()));
    }

    @PostMapping
    public ApiResponse<TagDto> create(@RequestBody TagRequest req) {
        return ApiResponse.ok(tagService.create(CurrentUser.id(), req));
    }

    @PatchMapping("/{id}")
    public ApiResponse<TagDto> rename(@PathVariable Long id, @RequestBody TagRequest req) {
        return ApiResponse.ok(tagService.rename(CurrentUser.id(), id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Boolean>> delete(@PathVariable Long id) {
        tagService.delete(CurrentUser.id(), id);
        return ApiResponse.ok(Map.of("ok", true));
    }
}
