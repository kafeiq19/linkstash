package com.linkstash.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linkstash.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
