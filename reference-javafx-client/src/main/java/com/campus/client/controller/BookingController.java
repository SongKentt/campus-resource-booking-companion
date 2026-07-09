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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles everything that happens on the Resource Booking screen: picking a
 * resource, checking if it's free, and submitting the booking.
 */
public class BookingController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    // CampusService writes new bookings with status 0 and cancelled ones with
    // status 1 (see his bookResource()/cancelBooking()), so that's the convention we follow too
    private static final int STATUS_ACTIVE = 0;

    private final BookingView view;
    private final CampusService campusService;

    // Sample resource list for now - CampusService doesn't have a way to list all
    // resources yet (nothing wraps campus://facilities). Swap this out once that's ready.
    private final List<Resource> allResources = List.of(
            new Resource("D9A.01", "Discussion Room D9A.01", "Block D", 6),
            new Resource("D9A.02", "Discussion Room D9A.02", "Block D", 6),
            new Resource("SP.11", "Study Pod 11", "Block E", 2),
            new Resource("LAB.03", "Lab Workstation 03", "Block E", 30),
            new Resource("SPT.01", "Badminton Court 1", "Sports Complex", 20)
    );

    // whichever resources match the currently selected Resource Type
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
        view.setResourceTypes(getResourceTypes());
    }

    // Resource.java has no "type" field, so we guess it from the room's name just
    // for the dropdown. Doesn't affect what actually gets sent to bookResource().
    private String deriveType(Resource resource) {
        String name = resource.getResourceName().toLowerCase();
        if (name.contains("discussion")) return "Discussion Room";
        if (name.contains("pod") || name.contains("study")) return "Study Pod";
        if (name.contains("lab") || name.contains("workstation")) return "Lab Workstation";
        if (name.contains("sport") || name.contains("court") || name.contains("badminton")) return "Sports Facility";
        return "Other";
    }

    private List<String> getResourceTypes() {
        return allResources.stream().map(this::deriveType).distinct().sorted().toList();
    }

    // fired when the Resource Type dropdown changes - just narrows the list
    public void handleResourceTypeChanged(String type) {
        candidatesForSelectedType = allResources.stream()
                .filter(r -> deriveType(r).equals(type))
                .toList();
        view.updateResourceIdOptions(candidatesForSelectedType);
    }

    // fired when the student clicks the Resource ID field. Actually pings the
    // real server through CampusService.checkAvailability() - its response is
    // just raw text though, so right now we use it to confirm the server's
    // reachable before opening the picker, not to filter the table itself.
    public void handleCheckAvailability(String rawDate) {
        if (candidatesForSelectedType.isEmpty()) {
            view.setResourceIdError("Please select a resource type first");
            return;
        }

        String dateError = validateDate(rawDate);
        if (dateError != null) {
            // no date typed yet, just show what we have without bothering the server
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
        task.setOnFailed(e -> view.showError(friendlyMessage(task.getException())));
        worker.submit(task);
    }

    // fired when Submit Booking is clicked - validates everything, checks for
    // duplicates, then actually books it
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

        view.setFormEnabled(false); // lock the form + show the spinner while we wait

        // duplicate check and the actual booking both happen off the UI thread,
        // since the duplicate check reads bookingHistory.txt (real file I/O)
        Task<Booking> task = new Task<>() {
            @Override
            protected Booking call() throws Exception {
                if (isDuplicateBooking(studentId, resourceId, finalDate, finalStart, finalEnd)) {
                    throw new DuplicateBookingException();
                }
                return campusService.bookResource(studentId, resourceId, finalDate, finalStart, finalEnd);
            }
        };
        task.setOnSucceeded(e -> view.showBookingConfirmation(task.getValue().getBookingRef()));
        task.setOnFailed(e -> {
            if (task.getException() instanceof DuplicateBookingException) {
                view.showDuplicateWarning();
            } else {
                view.showError(friendlyMessage(task.getException()));
            }
        });
        worker.submit(task);
    }

    // CampusService doesn't have its own duplicate check, so we do it here by
    // comparing against the student's existing bookings
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

    // just a marker so we can tell "duplicate" apart from "actual server error" below
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
            return "Invalid date format";
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
            return "Invalid time format";
        }
        return null;
    }

    private String friendlyMessage(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        if (cause.getMessage() != null) {
            return "Unable to reach the campus server. " + cause.getMessage();
        }
        return "Unable to reach the campus server. Please check your connection and try again.";
    }

    public void shutdown() {
        worker.shutdownNow();
    }
}

