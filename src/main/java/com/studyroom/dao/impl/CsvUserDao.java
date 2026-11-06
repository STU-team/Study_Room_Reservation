package com.studyroom.dao.impl;

import com.studyroom.dao.DataAccessException;
import com.studyroom.dao.UserDao;
import com.studyroom.entity.Role;
import com.studyroom.entity.User;
import com.studyroom.util.CsvMapper;
import com.studyroom.util.CsvUtil;
import com.studyroom.util.DataInitializer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * UserDao 的 CSV 实现。
 *
 * <p>契约 v0.6 的 users.csv 只有 username、password、role，没有 userId 与 name。
 * userId 由 username 稳定散列得到；name 无独立列，读入时取 username 作为显示名。</p>
 */
public class CsvUserDao implements UserDao {
    private static final List<String> HEADER = DataInitializer.headers().get("users.csv");
    private static final int COL_USERNAME = 0;
    private static final int COL_PASSWORD = 1;
    private static final int COL_ROLE = 2;

    private final Path file;

    public CsvUserDao(Path dataDirectory) {
        Objects.requireNonNull(dataDirectory, "dataDirectory 不能为空");
        this.file = dataDirectory.resolve("users.csv");
        try {
            CsvUtil.ensureHeader(file, HEADER);
        } catch (IOException ex) {
            throw new DataAccessException("初始化 users.csv 失败", ex);
        }
    }

    @Override
    public synchronized User findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        List<List<String>> rows = readRows();
        for (int i = 1; i < rows.size(); i++) {
            if (username.equals(rows.get(i).get(COL_USERNAME))) {
                return toUser(rows.get(i), i);
            }
        }
        return null;
    }

    @Override
    public synchronized void save(User user) {
        Objects.requireNonNull(user, "user 不能为空");
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("username 不能为空");
        }
        if (user.getRole() == null) {
            throw new IllegalArgumentException("role 不能为空");
        }
        List<List<String>> rows = readRows();
        for (int i = 1; i < rows.size(); i++) {
            if (user.getUsername().equals(rows.get(i).get(COL_USERNAME))) {
                throw new DataAccessException("用户名已存在：" + user.getUsername());
            }
        }
        String password = user.getPassword() == null ? "" : user.getPassword();
        rows.add(List.of(user.getUsername(), password, user.getRole().name()));
        writeRows(rows);
    }

    private User toUser(List<String> row, int rowIndex) {
        if (row.size() != HEADER.size()) {
            throw new DataAccessException("users.csv 第 " + (rowIndex + 1) + " 行字段数量错误");
        }
        Role role;
        try {
            role = Role.valueOf(row.get(COL_ROLE).trim());
        } catch (IllegalArgumentException ex) {
            throw new DataAccessException("users.csv 第 " + (rowIndex + 1) + " 行角色非法：" + row.get(COL_ROLE), ex);
        }
        String username = row.get(COL_USERNAME);
        User user = new User(CsvMapper.stableId(username), username, row.get(COL_PASSWORD), username, role);
        return user;
    }

    private List<List<String>> readRows() {
        try {
            List<List<String>> rows = CsvUtil.readAll(file);
            if (rows.isEmpty() || !HEADER.equals(rows.get(0))) {
                throw new DataAccessException("users.csv 表头缺失或与契约不一致");
            }
            return rows;
        } catch (IOException ex) {
            throw new DataAccessException("读取 users.csv 失败", ex);
        }
    }

    private void writeRows(List<List<String>> rows) {
        try {
            CsvUtil.writeAll(file, rows);
        } catch (IOException ex) {
            throw new DataAccessException("写入 users.csv 失败", ex);
        }
    }
}
