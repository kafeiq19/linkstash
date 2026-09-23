package com.linkstash.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linkstash.backend.entity.BookmarkTag;
import com.linkstash.backend.entity.Tag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookmarkTagMapper extends BaseMapper<BookmarkTag> {

    @Insert("INSERT OR IGNORE INTO bookmark_tag(bookmark_id, tag_id) VALUES(#{bookmarkId}, #{tagId})")
    int insertIgnore(@Param("bookmarkId") Long bookmarkId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM bookmark_tag WHERE bookmark_id = #{bookmarkId}")
    int deleteByBookmarkId(@Param("bookmarkId") Long bookmarkId);

    @Delete("DELETE FROM bookmark_tag WHERE bookmark_id = #{bookmarkId} AND tag_id = #{tagId}")
    int deleteLink(@Param("bookmarkId") Long bookmarkId, @Param("tagId") Long tagId);

    @Select("""
            SELECT t.id AS id, t.user_id AS user_id, t.name AS name, t.created_at AS created_at
            FROM tag t
            JOIN bookmark_tag bt ON bt.tag_id = t.id
            WHERE bt.bookmark_id = #{bookmarkId}
            ORDER BY t.name ASC
            """)
    List<Tag> selectTagsByBookmarkId(@Param("bookmarkId") Long bookmarkId);

    @Select("""
            <script>
            SELECT bt.bookmark_id AS bookmarkId, t.id AS id, t.user_id AS user_id, t.name AS name, t.created_at AS created_at
            FROM bookmark_tag bt
            JOIN tag t ON t.id = bt.tag_id
            WHERE bt.bookmark_id IN
            <foreach collection="bookmarkIds" item="bid" open="(" separator="," close=")">#{bid}</foreach>
            ORDER BY t.name ASC
            </script>
            """)
    List<TagLinkRow> selectTagsByBookmarkIds(@Param("bookmarkIds") List<Long> bookmarkIds);

    class TagLinkRow {
        private Long bookmarkId;
        private Long id;
        private Long userId;
        private String name;
        private String createdAt;

        public Long getBookmarkId() {
            return bookmarkId;
        }

        public void setBookmarkId(Long bookmarkId) {
            this.bookmarkId = bookmarkId;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }
}
