package com.ai.comic.account.mapper;

import com.ai.comic.account.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
