package com.linkstash.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("bookmark_tag")
public class BookmarkTag {

    private Long bookmarkId;
    private Long tagId;

    public BookmarkTag() {
    }

    public BookmarkTag(Long bookmarkId, Long tagId) {
        this.bookmarkId = bookmarkId;
        this.tagId = tagId;
    }

    public Long getBookmarkId() {
        return bookmarkId;
    }

    public void setBookmarkId(Long bookmarkId) {
        this.bookmarkId = bookmarkId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }
}
