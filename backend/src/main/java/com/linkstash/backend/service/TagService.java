package com.linkstash.backend.service;

import com.linkstash.backend.dto.TagDto;
import com.linkstash.backend.dto.TagRequest;
import com.linkstash.backend.entity.Tag;
import com.linkstash.backend.common.ApiException;
import com.linkstash.backend.mapper.TagMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class TagService {

    private final TagMapper tagMapper;

    public TagService(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    public List<TagDto> list(Long userId) {
        return tagMapper.selectWithCount(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public TagDto create(Long userId, TagRequest req) {
        String name = normalizeName(req == null ? null : req.getName());
        Tag existing = findByName(userId, name);
        if (existing != null) {
            throw ApiException.badRequest("tag name already exists");
        }
        Tag tag = new Tag();
        tag.setUserId(userId);
        tag.setName(name);
        tag.setCreatedAt(Instant.now().toString());
        try {
            tagMapper.insert(tag);
        } catch (DuplicateKeyException e) {
            throw ApiException.badRequest("tag name already exists");
        }
        return new TagDto(tag.getId(), tag.getName(), 0L);
    }

    @Transactional
    public TagDto rename(Long userId, Long tagId, TagRequest req) {
        Tag tag = requireOwned(userId, tagId);
        String name = normalizeName(req == null ? null : req.getName());
        Tag clash = findByName(userId, name);
        if (clash != null && !clash.getId().equals(tagId)) {
            throw ApiException.badRequest("tag name already exists");
        }
        tag.setName(name);
        tagMapper.updateById(tag);
        return new TagDto(tag.getId(), tag.getName(), tag.getCount());
    }

    @Transactional
    public void delete(Long userId, Long tagId) {
        requireOwned(userId, tagId);
        tagMapper.deleteLinksByTagId(tagId);
        tagMapper.deleteById(tagId);
    }

    /**
     * Upsert tags by name for a user; returns entity ids in input order (deduped).
     */
    @Transactional
    public List<Tag> upsertByNames(Long userId, List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        List<Tag> result = new java.util.ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();
        for (String raw : names) {
            String name = normalizeName(raw);
            if (!seen.add(name)) {
                continue;
            }
            Tag existing = findByName(userId, name);
            if (existing == null) {
                existing = new Tag();
                existing.setUserId(userId);
                existing.setName(name);
                existing.setCreatedAt(Instant.now().toString());
                try {
                    tagMapper.insert(existing);
                } catch (DuplicateKeyException e) {
                    existing = findByName(userId, name);
                }
            }
            result.add(existing);
        }
        return result;
    }

    public Tag requireOwned(Long userId, Long tagId) {
        Tag tag = tagMapper.selectWithCountById(userId, tagId);
        if (tag == null) {
            throw ApiException.notFound("tag not found");
        }
        return tag;
    }

    public Tag findByName(Long userId, String name) {
        return tagMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Tag>()
                .eq(Tag::getUserId, userId)
                .eq(Tag::getName, name));
    }

    public String normalizeName(String name) {
        if (name == null) {
            throw ApiException.badRequest("tag name is required");
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 24) {
            throw ApiException.badRequest("tag name must be 1-24 characters");
        }
        return trimmed;
    }

    private TagDto toDto(Tag tag) {
        return new TagDto(tag.getId(), tag.getName(), tag.getCount() == null ? 0L : tag.getCount());
    }
}
