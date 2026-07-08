package com.campus.client.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Booking {
    private final String bookRef;
    private final String resourceId;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String studentId;
    private int status;

    public Booking(String bookRef, String resourceId, LocalDate date, LocalTime startTime,
                   LocalTime endTime, String studentId, int status){
        this.bookRef = bookRef;
        this.resourceId = resourceId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.studentId = studentId;
        this.status = status;
    }

    public String getBookingRef(){
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
