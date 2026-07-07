package com.campus.client.model;

public class Student {
    private final String name;
    private final String password;
    private final String studentId;

    public Student(String name, String password, String studentId){
        this.name = name;
        this.password = password;
        this.studentId = studentId;
    }

    public String getStudentId(){
        return studentId;
    }

    public String getStudentName(){
        return name;
    }

    public String getStudentPassword(){
        return password;
    }
}
