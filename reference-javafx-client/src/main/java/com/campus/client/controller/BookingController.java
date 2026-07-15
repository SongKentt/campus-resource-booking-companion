package com.campus.client.controller;

import com.campus.client.model.Booking;
import com.campus.client.model.Resource;
import com.campus.client.service.CampusService;
import com.campus.client.ui.BookingView;
import javafx.concurrent.Task;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookingController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int STATUS_ACTIVE = 0;

    private final BookingView view;
    private final CampusService campusService;

    // ===== RESOURCES (loaded from server) =====
    private List<Resource> allResources = new ArrayList<>();
    private List<String> availableResourceTypes = new ArrayList<>();

    private List<Resource> candidatesForSelectedType = List.of();
    private final ExecutorService worker = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "booking-worker");
        t.setDaemon(true);
        return t;
    });

    // Operating hours for each building
    private static final Map<String, String[]> OPERATING_HOURS = Map.of(
            "D", new String[]{"08:00", "21:00"},
            "E", new String[]{"08:00", "21:00"},
            "LAB", new String[]{"08:00", "22:00"},
            "LIB", new String[]{"07:00", "23:00"},
            "OUT", new String[]{"07:00", "22:00"}
    );

    // ===== RESOURCES THAT ACTUALLY WORK ON THE SERVER =====
    // Only these can be booked - the server rejects dot-format IDs
    private static final List<String> WORKING_RESOURCES = List.of(
            "KA-P1", "KA-P2", "SP-B1"
    );

    public BookingController(BookingView view, CampusService campusService) {
        this.view = view;
        this.campusService = campusService;
        view.setController(this);

        // ===== FIX 2: Load facilities in background to prevent UI freeze =====
        worker.submit(() -> {
            loadFacilitiesFromServer();
        });
    }

    // ================================================================
    // LOAD FACILITIES FROM SERVER
    // ================================================================

    private void loadFacilitiesFromServer() {
        try {
            String facilitiesData = campusService.getFacilities();
            if (facilitiesData == null || facilitiesData.isEmpty()) {
                System.err.println("Failed to load facilities from server. Using hardcoded resources.");
                loadHardcodedResources();
                // Update view on UI thread
                javafx.application.Platform.runLater(() -> {
                    view.updateResourceTypeOptions(availableResourceTypes);
                });
                return;
            }

            System.out.println("=== FACILITIES DATA FROM SERVER ===");
            System.out.println(facilitiesData);
            System.out.println("====================================");

            parseFacilities(facilitiesData);

            if (allResources.isEmpty()) {
                System.err.println("No resources loaded from facilities data. Using hardcoded resources.");
                loadHardcodedResources();
            }

            // Update view on UI thread
            javafx.application.Platform.runLater(() -> {
                view.updateResourceTypeOptions(availableResourceTypes);
            });

            System.out.println("Loaded " + allResources.size() + " resources from server");
            System.out.println("Available types: " + availableResourceTypes);

        } catch (Exception e) {
            System.err.println("Error loading facilities from server: " + e.getMessage());
            e.printStackTrace();
            loadHardcodedResources();
            javafx.application.Platform.runLater(() -> {
                view.updateResourceTypeOptions(availableResourceTypes);
            });
        }
    }

    /**
     * Parses the facilities.txt format.
     * Shows ALL resources in the dropdown, but only allows booking for working ones.
     */
    private void parseFacilities(String data) {
        allResources.clear();
        availableResourceTypes.clear();

        String[] lines = data.split("\\r?\\n");
        boolean inBookableSection = false;

        for (String line : lines) {
            line = line.trim();

            if (line.isEmpty()) continue;

            if (line.startsWith("[Bookable Resources]")) {
                inBookableSection = true;
                continue;
            }

            if (line.startsWith("[") && line.endsWith("]") && !line.startsWith("[Bookable Resources]")) {
                inBookableSection = false;
                continue;
            }

            if (line.startsWith("ROOM") || line.startsWith("---") || line.startsWith(":=")) {
                continue;
            }

            if (inBookableSection) {
                String[] parts = line.split("\\s*\\|\\s*");
                if (parts.length >= 4) {
                    String id = parts[0].trim();
                    String type = parts[1].trim();
                    int capacity = 0;
                    try {
                        capacity = Integer.parseInt(parts[2].trim());
                    } catch (NumberFormatException e) { /* ignore */ }
                    String building = parts[3].trim();

                    // ===== ADD ALL RESOURCES (BOTH DOT AND DASH) =====
                    // This ensures ALL 4 types show in the dropdown
                    if (id.matches("[A-Z0-9.\\-]+")) {
                        String typeName = getTypeName(type);
                        Resource resource = new Resource(id, typeName + " " + id, building, capacity);
                        allResources.add(resource);
                        String displayType = getDisplayType(id, typeName);
                        if (!availableResourceTypes.contains(displayType)) {
                            availableResourceTypes.add(displayType);
                        }
                        System.out.println("Loaded resource: " + id + " (" + displayType + ")");
                    }
                }
            }
        }
    }

    private String getTypeName(String type) {
        switch (type.toLowerCase()) {
            case "discussion_room": return "Discussion Room";
            case "group_study_room": return "Group Study";
            case "computer_lab": return "Lab Workstation";
            case "study_pod": return "Study Pod";
            case "basketball court": return "Sports Facility";
            default: return type.substring(0, 1).toUpperCase() + type.substring(1);
        }
    }

    private String getDisplayType(String id, String typeName) {
        if (id.startsWith("KA-")) return "Study Pod";
        if (id.startsWith("SP-")) return "Sports Facility";
        if (id.startsWith("C7")) return "Lab Workstation";
        if (id.startsWith("D9") || id.startsWith("E9") || id.startsWith("E7")) return "Discussion Room";
        return typeName;
    }

    private void loadHardcodedResources() {
        allResources.clear();
        availableResourceTypes.clear();

        allResources.addAll(List.of(
                new Resource("KA-P1", "Study Pod KA-P1", "LIB", 2),
                new Resource("KA-P2", "Study Pod KA-P2", "LIB", 1),
                new Resource("SP-B1", "Basketball Court SP-B1", "OUT", 40)
        ));

        availableResourceTypes.addAll(List.of("Study Pod", "Sports Facility"));
        System.out.println("Loaded " + allResources.size() + " hardcoded resources");
    }

    // ================================================================
    // RESOURCE TYPE HANDLING
    // ================================================================

    private String deriveType(Resource resource) {
        String id = resource.getResourceId();
        String name = resource.getResourceName().toLowerCase();

        if (id.startsWith("KA-")) return "Study Pod";
        if (id.startsWith("SP-")) return "Sports Facility";
        if (id.startsWith("C7")) return "Lab Workstation";
        if (id.startsWith("D9") || id.startsWith("E9") || id.startsWith("E7")) return "Discussion Room";
        if (name.contains("discussion") || name.contains("group")) return "Discussion Room";
        if (name.contains("lab") || name.contains("computer")) return "Lab Workstation";
        if (name.contains("pod") || name.contains("study")) return "Study Pod";
        if (name.contains("sport") || name.contains("court") || name.contains("basketball")) return "Sports Facility";
        return "Other";
    }

    /**
     * Checks if a resource can actually be booked (server accepts it).
     * Only dash-format resources work: KA-P1, KA-P2, SP-B1
     */
    private boolean isBookableResource(String resourceId) {
        return WORKING_RESOURCES.contains(resourceId);
    }

    public void handleResourceTypeChanged(String type) {
        if (type == null || type.isBlank()) return;
        candidatesForSelectedType = allResources.stream()
                .filter(r -> deriveType(r).equals(type))
                .toList();
        view.updateResourceIdOptions(candidatesForSelectedType);
    }

    public List<String> getAvailableResourceTypes() {
        return availableResourceTypes;
    }

    // ================================================================
    // CHECK AVAILABILITY
    // ================================================================

    public void handleCheckAvailability(String resourceId, String date, String startTime, String endTime) {
        if (resourceId == null || resourceId.isBlank()) {
            view.showError("Please select a resource first");
            return;
        }

        // ===== Check if the resource is bookable =====
        if (!isBookableResource(resourceId)) {
            view.showError("The resource is unavailable. Please try again.");
            return;
        }

        String dateError = validateDate(date);
        if (dateError != null) {
            view.showError(dateError);
            return;
        }

        String startError = validateTime(startTime);
        if (startError != null) {
            view.showError(startError);
            return;
        }

        String endError = validateTime(endTime);
        if (endError != null) {
            view.showError(endError);
            return;
        }

        LocalTime start = LocalTime.parse(startTime.trim(), TIME_FORMAT);
        LocalTime end = LocalTime.parse(endTime.trim(), TIME_FORMAT);

        if (!end.isAfter(start)) {
            view.showError("End time must be after start time");
            return;
        }

        if (resourceId.startsWith("SP-")) {
            long minutes = Duration.between(start, end).toMinutes();
            if (minutes != 60) {
                view.showError("Sports facilities must be booked in 1-hour blocks (e.g., 10:00-11:00).");
                return;
            }
        }

        final LocalDate finalDate = LocalDate.parse(date.trim(), DATE_FORMAT);
        final LocalTime finalStart = start;
        final LocalTime finalEnd = end;

        view.setFormEnabled(false);

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                String hoursCheck = checkOperatingHours(resourceId, finalStart, finalEnd);
                if (hoursCheck != null) {
                    return "Not Available: " + hoursCheck;
                }

                boolean isAvailable = isTimeSlotAvailable(resourceId, finalDate, finalStart, finalEnd);
                if (isAvailable) {
                    return "Available: " + resourceId + " is free from " + finalStart + " to " + finalEnd + " on " + finalDate;
                } else {
                    return "Not Available: " + resourceId + " is already booked from " + finalStart + " to " + finalEnd + " on " + finalDate;
                }
            }
        };
        task.setOnSucceeded(evt -> {
            String result = task.getValue();
            view.showAvailabilityResult(result);
            view.setFormEnabled(true);
        });
        task.setOnFailed(evt -> {
            System.err.println("Availability check failed: " + task.getException());
            view.showError("Unable to check availability. Please try again.");
            view.setFormEnabled(true);
        });
        worker.submit(task);
    }

    private String checkOperatingHours(String resourceId, LocalTime start, LocalTime end) {
        String building = getBuildingForResource(resourceId);
        if (building == null) return "Unknown resource type.";

        String[] hours = OPERATING_HOURS.get(building);
        if (hours == null) return "Operating hours not available for this resource.";

        LocalTime open = LocalTime.parse(hours[0]);
        LocalTime close = LocalTime.parse(hours[1]);

        if (start.isBefore(open) || end.isAfter(close)) {
            return "Booking must be between " + open + " and " + close + ".";
        }

        if (resourceId.startsWith("SP-")) {
            if (start.getMinute() != 0 || end.getMinute() != 0) {
                return "Sports facilities must be booked on the hour (e.g., 10:00-11:00).";
            }
        }

        return null;
    }

    private String getBuildingForResource(String resourceId) {
        if (resourceId.startsWith("D9")) return "D";
        if (resourceId.startsWith("E9") || resourceId.startsWith("E7")) return "E";
        if (resourceId.startsWith("C7")) return "LAB";
        if (resourceId.startsWith("KA-")) return "LIB";
        if (resourceId.startsWith("SP-")) return "OUT";
        return null;
    }

    private boolean isTimeSlotAvailable(String resourceId, LocalDate date, LocalTime start, LocalTime end) {
        List<Booking> allBookings = campusService.getAllBookings();
        for (Booking b : allBookings) {
            if (b.getResourceId().equals(resourceId)
                    && b.getDate().equals(date)
                    && b.getStatus() == STATUS_ACTIVE) {
                boolean overlaps = start.isBefore(b.getEndTime()) && end.isAfter(b.getStartTime());
                if (overlaps) {
                    return false;
                }
            }
        }
        return true;
    }

    // ================================================================
    // BOOK RESOURCE
    // ================================================================

    public void handleBookResource(String resourceId, String studentId,
                                   String rawDate, String rawStartTime, String rawEndTime) {

        view.clearAllErrors();
        boolean valid = true;

        if (resourceId == null || resourceId.isBlank()) {
            view.setResourceIdError("Please select a resource");
            valid = false;
        }

        // ===== FIX 3: Check if resource exists =====
        if (valid && resourceId != null) {
            boolean exists = allResources.stream().anyMatch(r -> r.getResourceId().equals(resourceId));
            if (!exists) {
                view.showError("Invalid resource selected. Please try again.");
                return;
            }
        }

        // ===== CHECK IF RESOURCE IS BOOKABLE =====
        if (valid && resourceId != null && !isBookableResource(resourceId)) {
            view.showError("The resource is unavailable. Please try again.");
            return;
        }

        LocalDate date = null;
        String dateError = validateDate(rawDate);
        if (dateError != null) {
            view.setDateError(dateError);
            valid = false;
        } else {
            date = LocalDate.parse(rawDate.trim(), DATE_FORMAT);
        }

        LocalTime start = null;
        String startError = validateTime(rawStartTime);
        if (startError != null) {
            view.setStartTimeError(startError);
            valid = false;
        } else {
            start = LocalTime.parse(rawStartTime.trim(), TIME_FORMAT);
        }

        LocalTime end = null;
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

        if (valid && resourceId != null && start != null && end != null) {
            String hoursCheck = checkOperatingHours(resourceId, start, end);
            if (hoursCheck != null) {
                view.showError(hoursCheck);
                valid = false;
            }
        }

        if (!valid) {
            return;
        }

        final LocalDate finalDate = date;
        final LocalTime finalStart = start;
        final LocalTime finalEnd = end;

        view.setFormEnabled(false);

        Task<Booking> task = new Task<>() {
            @Override
            protected Booking call() throws Exception {
                // ===== FIX 1: Use getAllBookings() for duplicate check =====
                if (isDuplicateBooking(studentId, resourceId, finalDate, finalStart, finalEnd)) {
                    throw new DuplicateBookingException();
                }
                return campusService.bookResource(studentId, resourceId, finalDate, finalStart, finalEnd);
            }
        };
        task.setOnSucceeded(evt -> {
            Booking booking = task.getValue();
            view.showBookingConfirmation(booking.getBookingRef());
            view.setFormEnabled(true);
        });
        task.setOnFailed(evt -> {
            if (task.getException() instanceof DuplicateBookingException) {
                view.showDuplicateWarning();
            } else {
                System.err.println("Booking failed: " + task.getException());
                task.getException().printStackTrace();
                view.showError("Unable to book. Please try again.");
            }
            view.setFormEnabled(true);
        });
        worker.submit(task);
    }

    // ===== FIX 1: Updated to use getAllBookings() =====
    private boolean isDuplicateBooking(String studentId, String resourceId,
                                       LocalDate date, LocalTime start, LocalTime end) {
        // Check ALL bookings, not just the student's
        for (Booking existing : campusService.getAllBookings()) {
            if (existing.getResourceId().equals(resourceId)
                    && existing.getDate().equals(date)
                    && existing.getStatus() == STATUS_ACTIVE) {
                boolean overlaps = start.isBefore(existing.getEndTime()) && end.isAfter(existing.getStartTime());
                if (overlaps) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class DuplicateBookingException extends RuntimeException {
    }

    // ================================================================
    // VALIDATION HELPERS
    // ================================================================

    private String validateDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return "Please enter a date";
        }
        try {
            LocalDate date = LocalDate.parse(rawDate.trim(), DATE_FORMAT);
            if (date.isBefore(LocalDate.now())) {
                return "Date cannot be in the past";
            }
        } catch (DateTimeParseException ex) {
            return "Invalid date format (use yyyy-MM-dd)";
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

    public void shutdown() {
        worker.shutdownNow();
    }
}