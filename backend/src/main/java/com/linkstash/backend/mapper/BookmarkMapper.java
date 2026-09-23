package com.linkstash.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linkstash.backend.entity.Bookmark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BookmarkMapper extends BaseMapper<Bookmark> {

    @Select("""
            <script>
            SELECT b.* FROM bookmark b
            WHERE b.user_id = #{userId}
            <choose>
              <when test="status != null and status != '' and status != 'inbox'">
                AND b.status = #{status}
              </when>
              <otherwise>
                AND b.status != 'archived'
              </otherwise>
            </choose>
            <if test="favorite != null">
              AND b.favorite = #{favorite}
            </if>
            <if test="q != null and q != ''">
              AND (
                IFNULL(b.title,'') LIKE '%' || #{q} || '%' ESCAPE '\\'
                OR IFNULL(b.description,'') LIKE '%' || #{q} || '%' ESCAPE '\\'
                OR IFNULL(b.url,'') LIKE '%' || #{q} || '%' ESCAPE '\\'
              )
            </if>
            <if test="tagName != null and tagName != ''">
              AND b.id IN (
                SELECT bt.bookmark_id FROM bookmark_tag bt
                JOIN tag t ON t.id = bt.tag_id
                WHERE t.user_id = #{userId} AND t.name = #{tagName}
              )
            </if>
            ORDER BY b.created_at DESC, b.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Bookmark> selectFiltered(@Param("userId") Long userId,
                                  @Param("status") String status,
                                  @Param("favorite") Boolean favorite,
                                  @Param("q") String q,
                                  @Param("tagName") String tagName,
                                  @Param("limit") int limit,
                                  @Param("offset") int offset);

    @Select("""
            <script>
            SELECT COUNT(*) FROM bookmark b
            WHERE b.user_id = #{userId}
            <choose>
              <when test="status != null and status != '' and status != 'inbox'">
                AND b.status = #{status}
              </when>
              <otherwise>
                AND b.status != 'archived'
              </otherwise>
            </choose>
            <if test="favorite != null">
              AND b.favorite = #{favorite}
            </if>
            <if test="q != null and q != ''">
              AND (
                IFNULL(b.title,'') LIKE '%' || #{q} || '%' ESCAPE '\\'
                OR IFNULL(b.description,'') LIKE '%' || #{q} || '%' ESCAPE '\\'
                OR IFNULL(b.url,'') LIKE '%' || #{q} || '%' ESCAPE '\\'
              )
            </if>
            <if test="tagName != null and tagName != ''">
              AND b.id IN (
                SELECT bt.bookmark_id FROM bookmark_tag bt
                JOIN tag t ON t.id = bt.tag_id
                WHERE t.user_id = #{userId} AND t.name = #{tagName}
              )
            </if>
            </script>
            """)
    long countFiltered(@Param("userId") Long userId,
                       @Param("status") String status,
                       @Param("favorite") Boolean favorite,
                       @Param("q") String q,
                       @Param("tagName") String tagName);

    @Update("""
            <script>
            UPDATE bookmark
            <set>
              <if test="title != null">title = #{title},</if>
              <if test="description != null">description = #{description},</if>
              <if test="favicon != null">favicon = #{favicon},</if>
              <if test="siteName != null">site_name = #{siteName},</if>
              <if test="updatedAt != null">updated_at = #{updatedAt},</if>
            </set>
            WHERE id = #{id}
            </script>
            """)
    int updateMetadata(@Param("id") Long id,
                       @Param("title") String title,
                       @Param("description") String description,
                       @Param("favicon") String favicon,
                       @Param("siteName") String siteName,
                       @Param("updatedAt") String updatedAt);
}
