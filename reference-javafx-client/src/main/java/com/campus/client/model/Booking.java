package com.campus.client.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Booking {
    private final String bookRef;
    private final String studentId;
    private final String resourceId;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private int status;

    public Booking(String bookRef, String studentId, String resourceId, LocalDate date, LocalTime startTime,
                   LocalTime endTime, int status){
        this.bookRef = bookRef;
        this.studentId = studentId;
        this.resourceId = resourceId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public String getBookRef(){
        return bookRef;
    }

    public String getStudentId(){
        return studentId;
    }

    public String getResourceId(){
        return resourceId;
    }

    public LocalDate getDate(){
        return date;
    }

    public LocalTime getStartTime(){
        return startTime;
    }

    public LocalTime getEndTime(){
        return endTime;
    }

    public int getStatus(){
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
