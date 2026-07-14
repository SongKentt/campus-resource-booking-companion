package com.campus.client.controller;

import com.campus.client.model.Booking;
import com.campus.client.model.Resource;
import com.campus.client.service.CampusService;
import com.campus.client.ui.BookingView;
import javafx.concurrent.Task;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles everything that happens on the Resource Booking screen.
 */
public class BookingController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final int STATUS_ACTIVE = 0;

    private final BookingView view;
    private final CampusService campusService;

    /**
     * ===== RESOURCE LIST (matches facilities.txt) =====
     * These IDs are in DOT format as shown in the file.
     */
    private final List<Resource> allResources = List.of(
            // Discussion Rooms
            new Resource("D9A.01", "Discussion Room D9A.01", "D", 6),
            new Resource("D9B.01", "Discussion Room D9B.01", "D", 8),
            new Resource("E9A.03", "Discussion Room E9A.03", "E", 4),
            // Group Study Rooms
            new Resource("E7.01", "Group Study Room E7.01", "E", 6),
            new Resource("E7.02", "Group Study Room E7.02", "E", 6),
            // Computer Lab
            new Resource("C7.01", "Computer Lab C7.01", "LAB", 30),
            // Study Pods (already dash format)
            new Resource("KA-P1", "Study Pod KA-P1", "LIB", 2),
            new Resource("KA-P2", "Study Pod KA-P2", "LIB", 1),
            // Sports Facilities (already dash format)
            new Resource("SP-B1", "Basketball Court SP-B1", "OUT", 40)
    );

    /**
     * ===== MAPPING: DOT format → DASH format (for server compatibility) =====
     *
     * The server only accepts IDs with dashes: [A-Z]+-[A-Z0-9]+
     * So we map dot IDs to dash IDs before sending to the server.
     */
    private final Map<String, String> resourceIdMapping = new HashMap<>();

    {
        // Map DOT format (shown in UI) to DASH format (sent to server)
        resourceIdMapping.put("D9A.01", "D9A-P1");
        resourceIdMapping.put("D9B.01", "D9B-P1");
        resourceIdMapping.put("E9A.03", "E9A-P3");
        resourceIdMapping.put("E7.01", "E7-P1");
        resourceIdMapping.put("E7.02", "E7-P2");
        resourceIdMapping.put("C7.01", "C7-P1");
        // These already have dashes, so they map to themselves
        resourceIdMapping.put("KA-P1", "KA-P1");
        resourceIdMapping.put("KA-P2", "KA-P2");
        resourceIdMapping.put("SP-B1", "SP-B1");
    }

    private List<Resource> candidatesForSelectedType = List.of();

    private final ExecutorService worker = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "booking-worker");
        t.setDaemon(true);
        return t;
    });

    public BookingController(BookingView view, CampusService campusService) {
        this.view = view;
        this.campusService = campusService;
        view.setController(this);
    }

    /**
     * Derives the resource type from the resource ID.
     */
    private String deriveType(Resource resource) {
        String id = resource.getResourceId();
        String name = resource.getResourceName().toLowerCase();

        // Check by ID pattern
        if (id.startsWith("KA-")) return "Study Pod";
        if (id.startsWith("SP-")) return "Sports Facility";
        if (id.startsWith("C7.")) return "Lab Workstation";
        if (id.startsWith("D9") || id.startsWith("E9") || id.startsWith("E7")) {
            return "Discussion Room";
        }

        // Fallback by name
        if (name.contains("discussion")) return "Discussion Room";
        if (name.contains("pod") || name.contains("study")) return "Study Pod";
        if (name.contains("lab") || name.contains("computer")) return "Lab Workstation";
        if (name.contains("sport") || name.contains("court")) return "Sports Facility";
        return "Other";
    }

    public void handleResourceTypeChanged(String type) {
        candidatesForSelectedType = allResources.stream()
                .filter(r -> deriveType(r).equals(type))
                .toList();
        view.updateResourceIdOptions(candidatesForSelectedType);
    }

    public void handleCheckAvailability(String rawDate) {
        if (candidatesForSelectedType.isEmpty()) {
            view.setResourceIdError("Please select a resource type first");
            return;
        }

        String dateError = validateDate(rawDate);
        if (dateError != null) {
            view.showAvailableResources(candidatesForSelectedType);
            return;
        }

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return campusService.checkAvailability(rawDate.trim(), null);
            }
        };
        task.setOnSucceeded(e -> view.showAvailableResources(candidatesForSelectedType));
        task.setOnFailed(e -> {
            System.err.println("Check availability failed: " + task.getException());
            view.showError(friendlyMessage());
        });
        worker.submit(task);
    }

    public void handleBookResource(String resourceId, String studentId,
                                   String rawDate, String rawStartTime, String rawEndTime) {

        view.clearAllErrors();
        boolean valid = true;

        if (resourceId == null || resourceId.isBlank()) {
            view.setResourceIdError("Please select a resource");
            valid = false;
        }

        LocalDate date = null;
        String dateError = validateDate(rawDate);
        if (dateError != null) {
            view.setDateError(dateError);
            valid = false;
        } else {
            date = LocalDate.parse(rawDate.trim(), DATE_FORMAT);
        }

        LocalTime start = null, end = null;
        String startError = validateTime(rawStartTime);
        if (startError != null) {
            view.setStartTimeError(startError);
            valid = false;
        } else {
            start = LocalTime.parse(rawStartTime.trim(), TIME_FORMAT);
        }

        String endError = validateTime(rawEndTime);
        if (endError != null) {
            view.setEndTimeError(endError);
            valid = false;
        } else {
            end = LocalTime.parse(rawEndTime.trim(), TIME_FORMAT);
        }

        if (valid && start != null && end != null && !end.isAfter(start)) {
            view.setEndTimeError("End time must be after start time");
            valid = false;
        }

        if (!valid) {
            return;
        }

        final LocalDate finalDate = date;
        final LocalTime finalStart = start;
        final LocalTime finalEnd = end;

        /**
         * ===== THE FIX: Map dot ID to dash ID =====
         * The server expects dash format (e.g., D9A-P1)
         * We show dot format to the user (D9A.01) but send dash format to server.
         */
        String serverResourceId = resourceIdMapping.getOrDefault(resourceId, resourceId);

        System.out.println("🔍 ===== BOOKING DEBUG =====");
        System.out.println("🔍 UI Resource ID:    " + resourceId);
        System.out.println("🔍 Server Resource ID: " + serverResourceId);
        System.out.println("🔍 Student ID:        " + studentId);
        System.out.println("🔍 Date:              " + rawDate);
        System.out.println("🔍 Start Time:        " + rawStartTime);
        System.out.println("🔍 End Time:          " + rawEndTime);
        System.out.println("🔍 =======================");

        view.setFormEnabled(false);

        Task<Booking> task = new Task<>() {
            @Override
            protected Booking call() throws Exception {
                if (isDuplicateBooking(studentId, resourceId, finalDate, finalStart, finalEnd)) {
                    throw new DuplicateBookingException();
                }
                // Send the MAPPED ID to the server (dash format)
                return campusService.bookResource(studentId, serverResourceId, finalDate, finalStart, finalEnd);
            }
        };
        task.setOnSucceeded(e -> {
            Booking booking = task.getValue();
            view.showBookingConfirmation(booking.getBookingRef());
            view.setFormEnabled(true);
        });
        task.setOnFailed(e -> {
            if (task.getException() instanceof DuplicateBookingException) {
                view.showDuplicateWarning();
            } else {
                System.err.println("❌ ===== BOOKING ERROR =====");
                System.err.println("Error: " + task.getException());
                task.getException().printStackTrace();
                System.err.println("❌ =======================");
                view.showError(friendlyMessage());
            }
            view.setFormEnabled(true);
        });
        worker.submit(task);
    }

    private boolean isDuplicateBooking(String studentId, String resourceId,
                                       LocalDate date, LocalTime start, LocalTime end) {
        for (Booking existing : campusService.getUserBookings(studentId)) {
            if (existing.getStatus() == STATUS_ACTIVE
                    && existing.getResourceId().equals(resourceId)
                    && existing.getDate().equals(date)
                    && existing.getStartTime().equals(start)
                    && existing.getEndTime().equals(end)) {
                return true;
            }
        }
        return false;
    }

    private static class DuplicateBookingException extends RuntimeException {
    }

    private String validateDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return "Please enter a date";
        }
        LocalDate date;
        try {
            date = LocalDate.parse(rawDate.trim(), DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            return "Invalid date format (use yyyy-MM-dd)";
        }
        if (date.isBefore(LocalDate.now())) {
            return "Date cannot be in the past";
        }
        return null;
    }

    private String validateTime(String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            return "This field is required";
        }
        try {
            LocalTime.parse(rawTime.trim(), TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            return "Invalid time format (use HH:mm)";
        }
        return null;
    }

    private String friendlyMessage() {
        return "Unable to reach the campus server. Please check your connection and try again.";
    }

    public void shutdown() {
        worker.shutdownNow();
    }
}