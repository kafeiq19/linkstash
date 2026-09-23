package com.linkstash.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linkstash.backend.entity.Tag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    @Select("""
            SELECT t.id AS id, t.user_id AS user_id, t.name AS name, t.created_at AS created_at,
                   (SELECT COUNT(*) FROM bookmark_tag bt
                     JOIN bookmark b ON b.id = bt.bookmark_id
                    WHERE bt.tag_id = t.id AND b.user_id = t.user_id) AS count
            FROM tag t
            WHERE t.user_id = #{userId}
            ORDER BY t.name ASC
            """)
    List<Tag> selectWithCount(@Param("userId") Long userId);

    @Select("""
            SELECT t.id AS id, t.user_id AS user_id, t.name AS name, t.created_at AS created_at,
                   (SELECT COUNT(*) FROM bookmark_tag bt
                     JOIN bookmark b ON b.id = bt.bookmark_id
                    WHERE bt.tag_id = t.id AND b.user_id = t.user_id) AS count
            FROM tag t
            WHERE t.user_id = #{userId} AND t.id = #{id}
            """)
    Tag selectWithCountById(@Param("userId") Long userId, @Param("id") Long id);

    @Delete("DELETE FROM bookmark_tag WHERE tag_id = #{tagId}")
    int deleteLinksByTagId(@Param("tagId") Long tagId);
}
