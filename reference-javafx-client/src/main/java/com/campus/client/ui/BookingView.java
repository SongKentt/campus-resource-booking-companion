package com.campus.client.controller;

import com.campus.client.model.Booking;
import com.campus.client.model.Resource;
import com.campus.client.service.CampusService;
import com.campus.client.ui.BookingView;
import javafx.application.Platform;

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

    // ================================================================
    // CONSTANTS
    // ================================================================

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int STATUS_ACTIVE = 0;

    // ================================================================
    // DEPENDENCIES
    // ================================================================

    private final BookingView view;
    private final CampusService campusService;

    // ================================================================
    // DATA
    // ================================================================

    private List<Resource> allResources = new ArrayList<>();
    private List<String> availableResourceTypes = new ArrayList<>();

    // ================================================================
    // OPERATING HOURS PER BUILDING
    // ================================================================

    private static final Map<String, String[]> OPERATING_HOURS = Map.of(
            "D", new String[]{"08:00", "21:00"},
            "E", new String[]{"08:00", "21:00"},
            "LAB", new String[]{"08:00", "22:00"},
            "LIB", new String[]{"07:00", "23:00"},
            "OUT", new String[]{"07:00", "22:00"}
    );

    // ================================================================
    // BACKGROUND THREAD
    // ================================================================

    private final ExecutorService worker = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "booking-worker");
        t.setDaemon(true);
        return t;
    });

    // ================================================================
    // CONSTRUCTOR
    // ================================================================

    public BookingController(BookingView view, CampusService campusService) {
        this.view = view;
        this.campusService = campusService;
        view.setController(this);

        worker.submit(() -> {
            loadResourcesFromServer();
        });
    }

    // ================================================================
    // LOAD RESOURCES FROM MCP SERVER
    // ================================================================

    private void loadResourcesFromServer() {
        try {
            String facilitiesData = campusService.getFacilities();

            if (facilitiesData == null || facilitiesData.isEmpty()) {
                System.err.println("No facilities data from server. Using hardcoded resources.");
                loadHardcodedResources();
                updateViewWithTypes();
                return;
            }

            parseFacilities(facilitiesData);

            if (allResources.isEmpty()) {
                System.err.println("No resources parsed. Using hardcoded resources.");
                loadHardcodedResources();
            }

            updateViewWithTypes();

            System.out.println("Loaded " + allResources.size() + " resources from server");
            System.out.println("Available types: " + availableResourceTypes);

        } catch (Exception e) {
            System.err.println("Error loading facilities: " + e.getMessage());
            loadHardcodedResources();
            updateViewWithTypes();
        }
    }

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

                    if (id.matches("[A-Z0-9.\\-]+")) {
                        String typeName = getTypeName(type);
                        Resource resource = new Resource(id, typeName + " " + id, building, capacity);
                        allResources.add(resource);

                        String displayType = getDisplayType(id, typeName);
                        if (!availableResourceTypes.contains(displayType)) {
                            availableResourceTypes.add(displayType);
                        }
                    }
                }
            }
        }
    }

    private String getTypeName(String type) {
        switch (type) {
            case "discussion_room": return "Discussion Room";
            case "group_study_room": return "Group Study Room";
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
        if (id.startsWith("D9")) return "Discussion Room";
        if (id.startsWith("E9") || id.startsWith("E7")) return "Group Study Room";
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

    private void updateViewWithTypes() {
        Platform.runLater(() -> {
            view.updateResourceTypeOptions(availableResourceTypes);
        });
    }

    // ================================================================
    // DERIVE RESOURCE TYPE — SPLIT DISCUSSION AND GROUP STUDY
    // ================================================================

    private String deriveType(Resource resource) {
        String id = resource.getResourceId();
        String name = resource.getResourceName().toLowerCase();

        if (id.startsWith("KA-")) return "Study Pod";
        if (id.startsWith("SP-")) return "Sports Facility";
        if (id.startsWith("C7")) return "Lab Workstation";
        if (id.startsWith("D9")) return "Discussion Room";
        if (id.startsWith("E9") || id.startsWith("E7")) return "Group Study Room";
        if (name.contains("discussion")) return "Discussion Room";
        if (name.contains("group") || name.contains("study")) return "Group Study Room";
        if (name.contains("lab") || name.contains("computer")) return "Lab Workstation";
        if (name.contains("pod")) return "Study Pod";
        if (name.contains("sport") || name.contains("court") || name.contains("basketball")) return "Sports Facility";
        return "Other";
    }

    private String getBuildingForResource(String resourceId) {
        if (resourceId.startsWith("D9")) return "D";
        if (resourceId.startsWith("E9") || resourceId.startsWith("E7")) return "E";
        if (resourceId.startsWith("C7")) return "LAB";
        if (resourceId.startsWith("KA-")) return "LIB";
        if (resourceId.startsWith("SP-")) return "OUT";
        return null;
    }

    // ================================================================
    // GET ROOMS BY TYPE
    // ================================================================

    private List<Resource> getRoomsByType(String type) {
        List<Resource> result = new ArrayList<>();
        for (Resource r : allResources) {
            if (deriveType(r).equals(type)) {
                result.add(r);
            }
        }
        return result;
    }

    // ================================================================
    // HANDLE CHECK AVAILABILITY
    // ================================================================

    public void handleCheckAvailability(String date, String resourceType) {
        LocalDate localDate;
        try {
            localDate = LocalDate.parse(date, DATE_FORMAT);
            if (localDate.isBefore(LocalDate.now())) {
                Platform.runLater(() -> {
                    view.showError("Cannot check availability for past dates.");
                    view.setFormEnabled(true);
                });
                return;
            }
        } catch (DateTimeParseException e) {
            Platform.runLater(() -> {
                view.showError("Invalid date format. Use yyyy-MM-dd.");
                view.setFormEnabled(true);
            });
            return;
        }

        final String finalDate = date;
        final String finalResourceType = resourceType;

        worker.submit(() -> {
            try {
                List<Resource> roomsOfType = getRoomsByType(finalResourceType);
                List<Resource> availableRooms = new ArrayList<>();
                List<Resource> partiallyBookedRooms = new ArrayList<>();

                for (Resource room : roomsOfType) {
                    String building = getBuildingForResource(room.getResourceId());
                    if (building == null) continue;

                    // Call MCP to check availability
                    String result = campusService.checkAvailability(finalDate, building);

                    // If room has any booking that day → ORANGE, else → GREEN
                    if (result != null && result.contains(room.getResourceId() + ": BOOKED")) {
                        partiallyBookedRooms.add(room);
                    } else {
                        availableRooms.add(room);
                    }
                }

                final List<Resource> finalAvailable = availableRooms;
                final List<Resource> finalPartiallyBooked = partiallyBookedRooms;

                Platform.runLater(() -> {
                    if (finalAvailable.isEmpty() && finalPartiallyBooked.isEmpty()) {
                        view.showNoRoomsAvailable(finalDate);
                    } else {
                        view.showAvailableRooms(finalAvailable, finalPartiallyBooked, finalDate);
                    }
                    view.setFormEnabled(true);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    view.showError("Failed to check availability: " + e.getMessage());
                    view.setFormEnabled(true);
                });
            }
        });
    }

    // ================================================================
    // HANDLE BOOK RESOURCE
    // ================================================================

    public void handleBook(String studentId, String roomId, String dateStr,
                           String startTimeStr, String endTimeStr) {

        if (studentId == null || studentId.isEmpty()) {
            Platform.runLater(() -> {
                view.showError("Please log in first.");
                view.setFormEnabled(true);
            });
            return;
        }

        if (roomId == null || roomId.isEmpty()) {
            Platform.runLater(() -> {
                view.showError("Please select a room.");
                view.setFormEnabled(true);
            });
            return;
        }

        if (startTimeStr == null || startTimeStr.isEmpty()) {
            Platform.runLater(() -> {
                view.showError("Please enter a start time.");
                view.setFormEnabled(true);
            });
            return;
        }

        if (endTimeStr == null || endTimeStr.isEmpty()) {
            Platform.runLater(() -> {
                view.showError("Please enter an end time.");
                view.setFormEnabled(true);
            });
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr, DATE_FORMAT);
            if (date.isBefore(LocalDate.now())) {
                Platform.runLater(() -> {
                    view.showError("Cannot book a past date.");
                    view.setFormEnabled(true);
                });
                return;
            }
        } catch (DateTimeParseException e) {
            Platform.runLater(() -> {
                view.showError("Invalid date format. Use yyyy-MM-dd.");
                view.setFormEnabled(true);
            });
            return;
        }

        LocalTime startTime;
        try {
            startTime = LocalTime.parse(startTimeStr);
        } catch (DateTimeParseException e) {
            Platform.runLater(() -> {
                view.showError("Invalid start time format. Use HH:mm.");
                view.setFormEnabled(true);
            });
            return;
        }

        LocalTime endTime;
        try {
            endTime = LocalTime.parse(endTimeStr);
        } catch (DateTimeParseException e) {
            Platform.runLater(() -> {
                view.showError("Invalid end time format. Use HH:mm.");
                view.setFormEnabled(true);
            });
            return;
        }

        if (!endTime.isAfter(startTime)) {
            Platform.runLater(() -> {
                view.showError("End time must be after start time.");
                view.setFormEnabled(true);
            });
            return;
        }

        // ─── SPORTS FACILITY 1-HOUR RULE ──────────────────────────────
        if (roomId.startsWith("SP-")) {
            long minutes = Duration.between(startTime, endTime).toMinutes();
            if (minutes != 60) {
                Platform.runLater(() -> {
                    view.showError("Sports facilities must be booked in 1-hour blocks (e.g., 10:00-11:00).");
                    view.setFormEnabled(true);
                });
                return;
            }
        }

        // ─── OPERATING HOURS ───────────────────────────────────────────
        String building = getBuildingForResource(roomId);
        if (building != null) {
            String[] hours = OPERATING_HOURS.get(building);
            if (hours != null) {
                LocalTime open = LocalTime.parse(hours[0]);
                LocalTime close = LocalTime.parse(hours[1]);
                if (startTime.isBefore(open) || endTime.isAfter(close)) {
                    Platform.runLater(() -> {
                        view.showError("Booking must be between " + open + " and " + close + ".");
                        view.setFormEnabled(true);
                    });
                    return;
                }
            }
        }

        // ─── DUPLICATE CHECK ───────────────────────────────────────────
        if (isDuplicateBooking(studentId, roomId, date, startTime, endTime)) {
            Platform.runLater(() -> {
                view.showError("You already have a booking for this room at this time.");
                view.setFormEnabled(true);
            });
            return;
        }

        final LocalDate finalDate = date;
        final LocalTime finalStart = startTime;
        final LocalTime finalEnd = endTime;
        final String finalRoomId = roomId;
        final String finalStudentId = studentId;

        worker.submit(() -> {
            try {
                Booking booking = campusService.bookResource(
                        finalStudentId, finalRoomId, finalDate, finalStart, finalEnd
                );

                Platform.runLater(() -> {
                    view.showBookingConfirmation(booking.getBookingRef());
                    view.setFormEnabled(true);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    view.showError("Failed to book: " + e.getMessage());
                    view.setFormEnabled(true);
                });
            }
        });
    }

    // ================================================================
    // DUPLICATE CHECK (LOCAL FILE ONLY)
    // ================================================================

    private boolean isDuplicateBooking(String studentId, String roomId,
                                       LocalDate date, LocalTime start, LocalTime end) {
        List<Booking> userBookings = campusService.getUserBookings(studentId);
        for (Booking b : userBookings) {
            if (b.getResourceId().equals(roomId)
                    && b.getDate().equals(date)
                    && b.getStatus() == STATUS_ACTIVE) {
                boolean overlaps = start.isBefore(b.getEndTime()) && end.isAfter(b.getStartTime());
                if (overlaps) {
                    return true;
                }
            }
        }
        return false;
    }

    // ================================================================
    // SHUTDOWN
    // ================================================================

    public void shutdown() {
        worker.shutdownNow();
    }
}