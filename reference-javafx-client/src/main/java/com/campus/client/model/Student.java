package com.campus.client.model;

public class Student {
    private final String studentId;
    private final String name;
    private final String password;

    // Constructor of Booking, to initialize the attribute values of the student instance
    public Student(String studentId, String name, String password){
        this.studentId = studentId;
        this.name = name;
        this.password = password;
    }

    // Getters methods for the student instance
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
