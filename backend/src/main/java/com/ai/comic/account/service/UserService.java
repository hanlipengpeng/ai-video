package com.ai.comic.account.service;

import com.ai.comic.account.entity.User;
import com.ai.comic.account.mapper.UserMapper;
import com.ai.comic.common.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务：注册、登录、查询。
 */
@Slf4j
@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    private final PasswordEncoder passwordEncoder;

    public UserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 注册：用户名 + 密码，密码 BCrypt 哈希存储。
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 创建的用户（不含密码）
     */
    public User register(String username, String password) {
        if (username == null || username.trim().length() < 2 || username.trim().length() > 64) {
            throw new BusinessException(400, "用户名长度需在 2~64 之间");
        }
        if (password == null || password.length() < 6 || password.length() > 64) {
            throw new BusinessException(400, "密码长度需在 6~64 之间");
        }
        // 校验用户名唯一
        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count != null && count > 0) {
            throw new BusinessException(409, "用户名已存在");
        }
        User user = new User();
        user.setUsername(username.trim());
        user.setPasswordHash(passwordEncoder.encode(password));
        baseMapper.insert(user);
        log.info("用户注册成功: id={}, username={}", user.getId(), user.getUsername());
        return user;
    }

    /**
     * 登录校验：用户名 + 密码。
     *
     * @return 校验通过的用户实体
     */
    public User login(String username, String password) {
        User user = baseMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        return user;
    }

    /**
     * 根据用户名查询。
     */
    public User getByUsername(String username) {
        return baseMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }
}
