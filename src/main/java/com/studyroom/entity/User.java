package com.studyroom.entity;

/** 系统登录账户。信用分不属于 User，统一保存在 Student.creditScore。 */
public class User {
    private Long userId;
    private String username;
    private String password;
    private String name;
    private Role role;

    public User() {}

    public User(Long userId, String username, String password, String name, Role role) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
