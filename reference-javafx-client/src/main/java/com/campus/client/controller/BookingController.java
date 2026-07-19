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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BookingController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int STATUS_ACTIVE = 0; // this matches the status code used in Booking

    private final BookingView view;
    private final CampusService campusService;

    // resources pulled from the MCP server, filtered down to only the ones you can actually book
    private List<Resource> allResources = new ArrayList<>();
    private List<String> availableResourceTypes = new ArrayList<>();
    private List<String> bookableRoomIds = new ArrayList<>();

    // background thread for doing stuff that might take a while so the ui doesnt freeze
    private Thread workerThread;

    public BookingController(BookingView view, CampusService campusService) {
        this.view = view;
        this.campusService = campusService;
        view.setController(this);

        // load the resource list as soon as the controller is made

        startWorker(() -> {
            loadResourcesFromServer();
        });
    }

    // helper method to start background threads, makes sure old ones are killed first
    private void startWorker(Runnable task) {
        // Kill any existing thread thats still running
        if (workerThread != null && workerThread.isAlive()) {
            workerThread.interrupt();
        }
        workerThread = new Thread(() -> {
            try {
                task.run();
            } catch (Exception e) {
                System.err.println("Worker thread error: " + e.getMessage());
            }
        });
        workerThread.setDaemon(true);
        workerThread.setName("booking-worker");
        workerThread.start();
    }

    // pulls facilities data from campus service, then figures out which rooms are actually bookable then pushes the resource type dropdown options back to the view
    private void loadResourcesFromServer() {
        try {
            campusService.loadResources();
            allResources = new ArrayList<>(campusService.getAllResources());

            if (allResources.isEmpty()) {
                System.err.println("No resources parsed from server.");
                return;
            }

            fetchBookableRooms();

            if (bookableRoomIds.isEmpty()) {
                System.err.println("No bookable rooms found on server.");
                return;
            }

            filterBookableResources();
            updateViewWithTypes();

        } catch (Exception e) {
            System.err.println("Error loading facilities: " + e.getMessage());
        }
    }

    // asks the server which rooms show up in the availability check using todays date , this is basically how we know which resource ids are real bookable rooms

    private void fetchBookableRooms() {
        try {
            String today = LocalDate.now().toString();
            String result = campusService.checkAvailability(today, null);

            bookableRoomIds = new ArrayList<>();
            // the availability response is a plain text table

            Pattern pattern = Pattern.compile("^\\s{2}([A-Z0-9\\-]+)\\s+");
            for (String line : result.split("\n")) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String roomId = matcher.group(1);
                    if (!bookableRoomIds.contains(roomId)) {
                        bookableRoomIds.add(roomId);
                    }
                }
            }

            System.out.println("Found " + bookableRoomIds.size() + " bookable rooms: " + bookableRoomIds);

        } catch (Exception e) {
            System.err.println("Failed to fetch bookable rooms: " + e.getMessage());
            bookableRoomIds = new ArrayList<>();
        }
    }

    // narrows allResources down to only the ones that showed up in fetchBookableRooms() also collects the distinct type names so the dropdown only shows types that have bookable rooms
    private void filterBookableResources() {
        List<Resource> filtered = new ArrayList<>();
        List<String> filteredTypes = new ArrayList<>();

        for (Resource r : allResources) {
            if (bookableRoomIds.contains(r.getResourceId())) {
                filtered.add(r);
                String type = r.getTypeName();
                if (!filteredTypes.contains(type)) {
                    filteredTypes.add(type);
                }
            }
        }

        allResources = filtered;
        availableResourceTypes = filteredTypes;

        System.out.println("Filtered to " + allResources.size() + " bookable resources");
        System.out.println("Available types: " + availableResourceTypes);
    }

    // sends the resource type list to the view , it has to run on the JavaFX thread since it touches UI components
    private void updateViewWithTypes() {
        Platform.runLater(() -> {
            view.updateResourceTypeOptions(availableResourceTypes);
        });
    }

    // filters allResources by type name, used when checking availability for a specific type
    private List<Resource> getRoomsByType(String type) {
        List<Resource> result = new ArrayList<>();
        for (Resource r : allResources) {
            if (r.getTypeName().equals(type)) {
                result.add(r);
            }
        }
        return result;
    }

    private Resource getResourceById(String roomId) {
        for (Resource r : allResources) {
            if (r.getResourceId().equals(roomId)) {
                return r;
            }
        }
        return null;
    }

    // called from BookingView when the user clicks "Check Availability"
    public void handleCheckAvailability(String date, String resourceType) {
        if (allResources.isEmpty()) {
            Platform.runLater(() -> {
                view.showError("Unable to load resources. Please check your connection.");
                view.setFormEnabled(true);
            });
            return;
        }

        // check the date is valid and not in the past
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

        // run the actual availability check on a background thread
        startWorker(() -> {
            try {
                List<Resource> roomsOfType = getRoomsByType(finalResourceType);
                List<Resource> availableRooms = new ArrayList<>();
                List<Resource> partiallyBookedRooms = new ArrayList<>();

                List<Booking> allBookings = campusService.getAllBookings();

                // for each room of this type, ask the server if its available
                for (Resource room : roomsOfType) {
                    String building = room.getBuilding();
                    if (building == null) continue;

                    String result = campusService.checkAvailability(finalDate, building);

                    boolean hasLocalBooking = false;
                    for (Booking b : allBookings) {
                        if (b.getResourceId().equals(room.getResourceId())
                                && b.getDate().toString().equals(finalDate)
                                && b.getStatus() == STATUS_ACTIVE) {
                            hasLocalBooking = true;
                            break;
                        }
                    }

                    // if server says this room has bookings, or we already have a local record of one
                    if ((result != null && result.contains(room.getResourceId() + ": HAS BOOKING(S)"))
                            || hasLocalBooking) {
                        partiallyBookedRooms.add(room);
                    } else {
                        availableRooms.add(room);
                    }
                }

                final List<Resource> finalAvailable = availableRooms;
                final List<Resource> finalPartiallyBooked = partiallyBookedRooms;

                // back on the UI thread to actually update what the user sees
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

    // called from BookingView when the user clicks "Confirm Booking"
    public void handleBook(String studentId, String roomId, String dateStr,
                           String startTimeStr, String endTimeStr) {

        // basic empty checks first
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

        // parse and validate the date - cant be in the past
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

        // parse start time
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

        // parse end time
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

        // special rule just for sports facilities id starts with SP-
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

        // make sure the chosen time actually falls within the room's opening hours
        Resource room = getResourceById(roomId);
        if (room != null) {
            if (room.getOpenTime() != null && room.getCloseTime() != null) {
                if (startTime.isBefore(room.getOpenTime()) || endTime.isAfter(room.getCloseTime())) {
                    Platform.runLater(() -> {
                        view.showError("Booking must be between " + room.getOpenTime() + " and " + room.getCloseTime());
                        view.setFormEnabled(true);
                    });
                    return;
                }
            }
        }

        // check this student doesnt already have an overlapping booking for the same room
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

        // all validation passed, now actually make the booking on a background thread
        // this calls the MCP book_resource tool through campusService, which can take a moment
        startWorker(() -> {
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

    // checks if the student already has a booking for this room where the time overlaps
    private boolean isDuplicateBooking(String studentId, String roomId,
                                       LocalDate date, LocalTime start, LocalTime end) {
        List<Booking> userBookings = campusService.getUserBookings(studentId);
        for (Booking b : userBookings) {
            if (b.getResourceId().equals(roomId)
                    && b.getDate().equals(date)
                    && b.getStatus() == STATUS_ACTIVE) {
                // classic overlap check: two time ranges overlap if one starts before the other ends
                boolean overlaps = start.isBefore(b.getEndTime()) && end.isAfter(b.getStartTime());
                if (overlaps) {
                    return true;
                }
            }
        }
        return false;
    }

    // stops the background worker thread, should be called when the app closes
    public void shutdown() {
        if (workerThread != null && workerThread.isAlive()) {
            workerThread.interrupt();
        }
    }
}