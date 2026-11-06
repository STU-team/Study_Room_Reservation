package com.studyroom.service.impl;

import com.studyroom.dao.UserDao;
import com.studyroom.entity.User;
import com.studyroom.service.AuthService;
import com.studyroom.service.BusinessException;

/** 登录实现：只校验用户名/密码；当前登录用户由界面保存 login 返回值，Service 不持有会话。 */
public class AuthServiceImpl implements AuthService {
    private final UserDao userDao;

    public AuthServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException("PARAM", "用户名和密码不能为空");
        }
        User user = userDao.findByUsername(username);
        if (user == null || !password.equals(user.getPassword())) {
            throw new BusinessException("AUTH_FAILED", "用户名或密码错误");
        }
        return user;
    }

    @Override
    public void logout() {
        // 会话由界面持有，此处无需清理状态
    }
}
