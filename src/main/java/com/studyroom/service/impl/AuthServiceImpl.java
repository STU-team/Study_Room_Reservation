package com.studyroom.service.impl;

import com.studyroom.dao.UserDao;
import com.studyroom.entity.User;
import com.studyroom.service.AuthService;
import com.studyroom.service.BusinessException;

/** 登录实现：用户名/密码校验，成功后持有当前会话用户。 */
public class AuthServiceImpl implements AuthService {
    private final UserDao userDao;
    private User current;

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
        current = user;
        return user;
    }

    @Override
    public void logout() {
        current = null;
    }

    /** 当前登录用户，未登录返回 null；前端菜单按角色渲染时使用。 */
    public User currentUser() {
        return current;
    }
}
