package com.ai.comic.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（文档 6.2.1）。
 * <p>
 * 极简：仅用户名 + BCrypt 密码哈希。
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（唯一） */
    private String username;

    /** BCrypt 哈希，不序列化到前端 */
    @JsonIgnore
    private String passwordHash;

    private LocalDateTime createdAt;
}
