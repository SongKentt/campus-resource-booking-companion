package com.campus.client.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Booking {
    //Booking reference number such as BK-1001
    private final String bookRef;
    // Resource ID such as KP-01
    private final String resourceId;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String studentId;
    // Status of the booking (0 - Active, 1 - Cancelled)
    private int status;

    // Constructor of Booking, to initialize the attribute values of the booking instance
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

    // Getters method for the student instance
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
