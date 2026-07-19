package com.campus.client.controller;

import com.campus.client.model.Booking;
import com.campus.client.service.CampusService;
import com.campus.client.ui.ViewBookingView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/* This is a controller for the booking history screen that load and display the bookings made by the user by seprating
   them in to past and present based on the date and status, and handle cancel booking action
*/
public class ViewBookingController {

    private final ViewBookingView view;
    private final CampusService campusService;
    private String currentStudentId = "";

    // Same as the booking status mentioned in other classes where 0 = Active booking, 1 = Cancelled booking
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

    /* This method is load the bookings of a specific user using the student and filters the user bookings into past and upcoming,
       then updates the view.
     */
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

        view.displayBookings(upcomingBookings, pastBookings);
    }

    // This method is used to determine whether the booking is categorized as past
    private boolean isPastBooking(Booking booking) {
        // If the booking status = 1 (cancelled), it will be always categorized as past
        if (booking.getStatus() == STATUS_CANCELLED) {
            return true;
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // If the booking status = 0 (active), then determine if the booking is past by checking if date has passed
        if (booking.getDate().isBefore(today)) {
            return true;
        }

        if (booking.getDate().isEqual(today) &&
                booking.getEndTime().isBefore(now)) {
            return true;
        }

        return false;
    }

    // Cancel the active booking that is not categorized as past with booking reference number
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

        if (targetBooking.getStatus() == STATUS_CANCELLED) {
            view.showError("This booking has already been cancelled.");
            return;
        }

        if (targetBooking.getDate().isBefore(LocalDate.now())) {
            view.showError("Cannot cancel a past booking.");
            return;
        }

        campusService.cancelBooking(bookingRef);
        view.showSuccess("Booking " + bookingRef + " has been cancelled.");

        loadBookings();
    }

}