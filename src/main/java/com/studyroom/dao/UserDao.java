package com.studyroom.dao;

import com.studyroom.entity.User;

/** 用户账户数据访问接口（方法签名遵循接口契约 v0.6）。 */
public interface UserDao {
    User findByUsername(String username);
    void save(User user);
}
