package com.campus.client.controller;

import com.campus.client.model.Booking;
import com.campus.client.service.CampusService;
import com.campus.client.ui.ViewBookingView;
import javafx.concurrent.Task;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles everything that happens on the Booking History screen: loading the
 * student's bookings and cancelling one when asked.
 *
 * Heads up: CampusService marks an active booking as status 0 and a cancelled
 * one as status 1 - the opposite of what you'd probably guess, so it's spelled
 * out below instead of just using 0/1 everywhere.
 */
public class ViewBookingController {

    private static final int STATUS_ACTIVE = 0;
    private static final int STATUS_CANCELLED = 1;

    private final ViewBookingView view;
    private final CampusService campusService;
    private String currentStudentId;

    private final ExecutorService worker = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "view-booking-worker");
        t.setDaemon(true);
        return t;
    });

    public ViewBookingController(ViewBookingView view, CampusService campusService) {
        this.view = view;
        this.campusService = campusService;
        view.setController(this);
    }

    /** called after login, or whenever the logged-in student changes */
    public void setStudentId(String studentId) {
        this.currentStudentId = studentId;
    }

    public void handleViewBookings() {
        if (currentStudentId == null) {
            return;
        }
        Task<List<Booking>> task = new Task<>() {
            @Override
            protected List<Booking> call() {
                return campusService.getUserBookings(currentStudentId);
            }
        };
        task.setOnSucceeded(e -> {
            List<Booking> all = task.getValue();
            List<Booking> upcoming = all.stream().filter(this::isUpcoming).toList();
            List<Booking> past = all.stream().filter(this::isPast).toList();
            view.displayBookings(upcoming, past);
        });
        task.setOnFailed(e -> view.showError(friendlyMessage(task.getException())));
        worker.submit(task);
    }

    public void handleCancelBooking(Booking booking) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                campusService.cancelBooking(booking.getBookingRef());
                return null;
            }
        };
        task.setOnSucceeded(e -> view.onBookingCancelled(booking));
        task.setOnFailed(e -> view.showError(friendlyMessage(task.getException())));
        worker.submit(task);
    }

    private boolean isUpcoming(Booking booking) {
        return !booking.getDate().isBefore(LocalDate.now()) && booking.getStatus() == STATUS_ACTIVE;
    }

    private boolean isPast(Booking booking) {
        return booking.getDate().isBefore(LocalDate.now()) || booking.getStatus() == STATUS_CANCELLED;
    }

    private String friendlyMessage(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        if (cause.getMessage() != null) {
            return cause.getMessage();
        }
        return "Unable to reach the campus server. Please check your connection and try again.";
    }

    public void shutdown() {
        worker.shutdownNow();
    }
}
