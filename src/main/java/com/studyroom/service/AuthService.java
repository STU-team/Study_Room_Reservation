package com.studyroom.service;

import com.studyroom.entity.User;

/** 登录入口（契约第六节）。 */
public interface AuthService {

    User login(String username, String password) throws BusinessException;

    void logout();
}
