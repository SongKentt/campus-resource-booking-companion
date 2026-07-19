package com.campus.client.service;

import com.campus.client.mcp.CampusMcpClient;
import com.campus.client.model.Booking;
import com.campus.client.model.DataStorage;
import com.campus.client.model.Resource;
import com.campus.client.model.Student;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// this is where all the main business logic lives
// talks to the mcp server and handles the text file storage
// controllers call these methods when they need to do something
public class CampusService {
    private final CampusMcpClient mcpClient;
    private final DataStorage dataStorage;

    // keeps resources in memory so we dont keep asking the server for them
    private List<Resource> allResources = new ArrayList<>();

    // looks for booking references in the server response
    private static final Pattern BOOKING_REF_PATTERN = Pattern.compile("(BK-\\d{4})");

    // status 0 means active 1 means cancelled
    private static final int STATUS_ACTIVE = 0;
    private static final int STATUS_CANCELLED = 1;

    public CampusService(CampusMcpClient mcpClient, DataStorage dataStorage){
        this.mcpClient = mcpClient;
        this.dataStorage = dataStorage;
    }

    // checks if the student id and password match whats in the user file
    public boolean validateStudent(String studentId, String password){
        for(Student s : dataStorage.loadStudents()){
            if(s.getStudentId().equals(studentId) && s.getStudentPassword().equals(password)){
                return true;
            }
        }
        return false;
    }

    // asks the server what rooms are free on a given date
    public String checkAvailability(String date, String building){
        Map<String, Object> argument = new HashMap<>();
        argument.put("date",date);

        if(building != null && !building.isEmpty()){
            argument.put("building",building);
        }

        return mcpClient.callTool("check_room_availability", argument);
    }

    // gets the facilities list from the server as raw text
    public String getFacilities() {
        return mcpClient.readResource("campus://facilities");
    }

    // loads all bookings from the local text file
    public ArrayList<Booking> getAllBookings() {
        return dataStorage.loadBookings();
    }

    // the main booking method. it calls the server to book, then saves it locally
    public Booking bookResource(String studentId, String resourceId, LocalDate date, LocalTime start, LocalTime end){

        Map<String, Object> argument = new HashMap<>();
        argument.put("resourceId", resourceId);
        argument.put("date", date.toString());
        argument.put("startTime", start.toString());
        argument.put("endTime", end.toString());
        argument.put("studentId", studentId);

        String result = mcpClient.callTool("book_resource", argument);

        // if the server returns an error, throw an exception
        if(result.startsWith("ERROR:")){
            throw new IllegalStateException(result);
        }

        String bookingRef = extractBookingRef(result);
        System.out.println("Extracted booking reference: " + bookingRef);
        System.out.println("Full response: " + result);

        Booking booking = new Booking(bookingRef, resourceId, date, start, end, studentId, STATUS_ACTIVE);

        dataStorage.saveBooking(booking);
        return booking;
    }

    // grabs the booking reference from the servers response text
    private String extractBookingRef(String response) {
        Matcher matcher = BOOKING_REF_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        System.err.println("Could not extract booking reference from: " + response);
        return "BK-" + System.currentTimeMillis();
    }

    // gets all bookings for a specific student
    public ArrayList<Booking> getUserBookings(String studentId){
        ArrayList<Booking> result = new ArrayList<>();
        for(Booking b : dataStorage.loadBookings()){
            if(b.getStudentId().equals(studentId)){
                result.add(b);
            }
        }
        return result;
    }

    // cancels a booking by changing its status to cancelled, we keep the record instead of deleting it

    public void cancelBooking(String bookingRef){
        dataStorage.updateBookingStatus(bookingRef, STATUS_CANCELLED);
    }

    public List<Resource> getAllResources() {
        return allResources;
    }

    // loads and parses the facilities data from the server
    public void loadResources() {
        String data = getFacilities();
        allResources = parseFacilities(data);
    }

    // parses the facilities text into actual Resource objects
    // only looks at the bokable resources section
    private List<Resource> parseFacilities(String data) {
        List<Resource> resources = new ArrayList<>();

        String[] lines = data.split("\\r?\\n");
        boolean inBookableSection = false;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // found the bookable resources section
            if (line.startsWith("[Bookable Resources]")) {
                inBookableSection = true;
                continue;
            }

            // if we hit another section header, we're done with bookable resources
            if (line.startsWith("[") && line.endsWith("]") && !line.startsWith("[Bookable Resources]")) {
                inBookableSection = false;
                continue;
            }

            if (line.startsWith("ROOM") || line.startsWith("---") || line.startsWith(":=")) {
                continue;
            }

            if (inBookableSection) {
                String[] parts = line.split("\\s*\\|\\s*");
                if (parts.length >= 6) {
                    String id = parts[0].trim();
                    String typeName = parts[1].trim();
                    int capacity = 0;
                    try {
                        capacity = Integer.parseInt(parts[2].trim());
                    } catch (NumberFormatException e) { /* ignore */ }
                    String building = parts[3].trim();

                    LocalTime openTime = null;
                    LocalTime closeTime = null;
                    try {
                        openTime = LocalTime.parse(parts[4].trim());
                        closeTime = LocalTime.parse(parts[5].trim());
                    } catch (Exception e) { /* ignore */ }

                    // make sure the id looks valid before adding
                    if (id.matches("[A-Z0-9.\\-]+")) {
                        String displayName = typeName + " " + id;
                        Resource resource = new Resource(id, displayName, building, capacity,
                                openTime, closeTime, typeName);
                        resources.add(resource);
                    }
                }
            }
        }

        return resources;
    }
}