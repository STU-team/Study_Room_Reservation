package com.studyroom.dao.impl;

import com.studyroom.dao.DataAccessException;
import com.studyroom.dao.UserDao;
import com.studyroom.entity.Role;
import com.studyroom.entity.User;
import com.studyroom.util.CsvMapper;
import com.studyroom.util.CsvStore;
import com.studyroom.util.DataInitializer;

import java.nio.file.Path;
import java.util.List;

/** UserDao 的 CSV 实现：userId 由 username 散列，name 无列时取 username。 */
public class CsvUserDao implements UserDao {
    private static final List<String> HEADER = DataInitializer.headers().get("users.csv");
    private final CsvStore store;

    public CsvUserDao(Path dataDirectory) {
        this.store = new CsvStore(dataDirectory.resolve("users.csv"), HEADER);
    }

    @Override
    public synchronized User findByUsername(String username) {
        List<List<String>> rows = store.all();
        int i = store.indexOf(rows, 0, username);
        if (i < 0) return null;
        String name = rows.get(i).get(0);
        return new User(CsvMapper.id(name), name, rows.get(i).get(1), name, Role.valueOf(rows.get(i).get(2)));
    }

    @Override
    public synchronized void save(User user) {
        List<List<String>> rows = store.all();
        if (store.indexOf(rows, 0, user.getUsername()) >= 0) throw new DataAccessException("用户名已存在：" + user.getUsername());
        rows.add(List.of(user.getUsername(), CsvMapper.text(user.getPassword()), user.getRole().name()));
        store.save(rows);
    }
}
