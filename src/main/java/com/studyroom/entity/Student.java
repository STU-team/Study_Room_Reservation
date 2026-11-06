package com.studyroom.entity;

/** 学生档案；creditScore 是当前权威信用分。 */
public class Student {
    private String studentId;
    private String name;
    private String phone;
    private Integer creditScore;

    public Student() {}

    public Student(String studentId, String name, String phone, Integer creditScore) {
        this.studentId = studentId;
        this.name = name;
        this.phone = phone;
        this.creditScore = creditScore;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getCreditScore() { return creditScore; }
    public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }
}
