package com.campus.client.service;

import com.campus.client.mcp.CampusMcpClient;
import com.campus.client.model.Booking;
import com.campus.client.model.DataStorage;
import com.campus.client.model.Student;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CampusService {
    private final CampusMcpClient mcpClient;
    private final DataStorage dataStorage;

    // Pattern to extract booking reference like BK-1010
    private static final Pattern BOOKING_REF_PATTERN = Pattern.compile("(BK-\\d{4})");

    // ===== STATUS CONSTANTS (BEFORE CHANGE) =====
    // 0 = Active, 1 = Cancelled
    private static final int STATUS_ACTIVE = 0;
    private static final int STATUS_CANCELLED = 1;

    public CampusService(CampusMcpClient mcpClient, DataStorage dataStorage){
        this.mcpClient = mcpClient;
        this.dataStorage = dataStorage;
    }

    // ================================================================
    // LOGIN
    // ================================================================

    public boolean validateStudent(String studentId, String password){
        for(Student s : dataStorage.loadStudents()){
            if(s.getStudentId().equals(studentId) && s.getStudentPassword().equals(password)){
                return true;
            }
        }
        return false;
    }

    // ================================================================
    // CHECK RESOURCE AVAILABILITY
    // ================================================================

    public String checkAvailability(String date, String building){
        Map<String, Object> argument = new HashMap<>();
        argument.put("date",date);

        if(building != null && !building.isEmpty()){
            argument.put("building",building);
        }

        return mcpClient.callTool("check_room_availability", argument);
    }

    // ================================================================
    // GET FACILITIES FROM SERVER
    // ================================================================

    public String getFacilities() {
        return mcpClient.readResource("campus://facilities");
    }

    // ================================================================
    // GET ALL BOOKINGS
    // ================================================================

    public ArrayList<Booking> getAllBookings() {
        return dataStorage.loadBookings();
    }

    // ================================================================
    // BOOK RESOURCE
    // ================================================================

    public Booking bookResource(String studentId, String resourceId, LocalDate date, LocalTime start, LocalTime end){

        Map<String, Object> argument = new HashMap<>();
        argument.put("resourceId", resourceId);
        argument.put("date", date.toString());
        argument.put("startTime", start.toString());
        argument.put("endTime", end.toString());
        argument.put("studentId", studentId);

        String result = mcpClient.callTool("book_resource", argument);

        if(result.startsWith("ERROR:")){
            throw new IllegalStateException(result);
        }

        String bookingRef = extractBookingRef(result);
        System.out.println("Extracted booking reference: " + bookingRef);
        System.out.println("Full response: " + result);

        // ===== PREVIOUS: status = 0 means ACTIVE =====
        Booking booking = new Booking(bookingRef, resourceId, date, start, end, studentId, STATUS_ACTIVE);

        dataStorage.saveBooking(booking);
        return booking;
    }

    private String extractBookingRef(String response) {
        Matcher matcher = BOOKING_REF_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        System.err.println("Could not extract booking reference from: " + response);
        return "BK-" + System.currentTimeMillis();
    }

    // ================================================================
    // VIEW BOOKINGS
    // ================================================================

    public ArrayList<Booking> getUserBookings(String studentId){
        ArrayList<Booking> result = new ArrayList<>();
        for(Booking b : dataStorage.loadBookings()){
            if(b.getStudentId().equals(studentId)){
                result.add(b);
            }
        }
        return result;
    }

    // ================================================================
    // CANCEL BOOKING
    // ================================================================

    public void cancelBooking(String bookingRef){
        // ===== PREVIOUS: 1 = CANCELLED =====
        dataStorage.updateBookingStatus(bookingRef, STATUS_CANCELLED);
    }
}