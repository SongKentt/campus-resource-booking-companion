package com.campus.client.controller;

import com.campus.client.model.Booking;
import com.campus.client.service.CampusService;
import com.campus.client.ui.ViewBookingView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles everything on the Booking History screen.
 * Shows past and upcoming bookings based on status and date.
 *
 * STATUS (BEFORE CHANGE): 0 = Active, 1 = Cancelled
 */
public class ViewBookingController {

    private final ViewBookingView view;
    private final CampusService campusService;
    private String currentStudentId = "";

    // ===== STATUS CONSTANTS (BEFORE CHANGE) =====
    // 0 = Active, 1 = Cancelled
    private static final int STATUS_ACTIVE = 0;
    private static final int STATUS_CANCELLED = 1;

    public ViewBookingController(ViewBookingView view, CampusService campusService) {
        this.view = view;
        this.campusService = campusService;
        view.setController(this);
    }

    public void setStudentId(String studentId) {
        this.currentStudentId = studentId;
    }

    public void loadBookings() {
        if (currentStudentId == null || currentStudentId.isEmpty()) {
            view.showError("No student logged in.");
            return;
        }

        ArrayList<Booking> allBookings = campusService.getUserBookings(currentStudentId);

        List<Booking> pastBookings = new ArrayList<>();
        List<Booking> upcomingBookings = new ArrayList<>();

        for (Booking b : allBookings) {
            if (isPastBooking(b)) {
                pastBookings.add(b);
            } else {
                upcomingBookings.add(b);
            }
        }

        view.displayBookings(pastBookings, upcomingBookings);
    }

    /**
     * PREVIOUS: status = 0 means ACTIVE, status = 1 means CANCELLED
     */
    private boolean isPastBooking(Booking booking) {
        // ===== If cancelled (status = 1), it's always past =====
        if (booking.getStatus() == STATUS_CANCELLED) {
            return true;
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // ===== If active (status = 0), check if date has passed =====
        if (booking.getDate().isBefore(today)) {
            return true;
        }

        if (booking.getDate().isEqual(today) &&
                booking.getEndTime().isBefore(now)) {
            return true;
        }

        return false;
    }

    public void handleCancelBooking(String bookingRef) {
        if (bookingRef == null || bookingRef.isEmpty()) {
            view.showError("No booking selected to cancel.");
            return;
        }

        ArrayList<Booking> allBookings = campusService.getUserBookings(currentStudentId);
        Booking targetBooking = null;
        for (Booking b : allBookings) {
            if (b.getBookingRef().equals(bookingRef)) {
                targetBooking = b;
                break;
            }
        }

        if (targetBooking == null) {
            view.showError("Booking not found.");
            return;
        }

        // ===== Check if booking is already cancelled =====
        if (targetBooking.getStatus() == STATUS_CANCELLED) {
            view.showError("This booking has already been cancelled.");
            return;
        }

        if (targetBooking.getDate().isBefore(LocalDate.now())) {
            view.showError("Cannot cancel a past booking.");
            return;
        }

        // ===== PREVIOUS: Sets status to 1 (CANCELLED) =====
        campusService.cancelBooking(bookingRef);
        view.showSuccess("Booking " + bookingRef + " has been cancelled.");

        loadBookings();
    }

    public List<Booking> getPastBookings() {
        List<Booking> past = new ArrayList<>();
        for (Booking b : campusService.getUserBookings(currentStudentId)) {
            if (isPastBooking(b)) {
                past.add(b);
            }
        }
        return past;
    }

    public List<Booking> getUpcomingBookings() {
        List<Booking> upcoming = new ArrayList<>();
        for (Booking b : campusService.getUserBookings(currentStudentId)) {
            if (!isPastBooking(b)) {
                upcoming.add(b);
            }
        }
        return upcoming;
    }
}