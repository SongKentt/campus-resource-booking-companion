package com.campus.client.model;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

public class DataStorage {
    // File path for a local file that store the student data
    private final String userDataFilePath;
    // File path for a local file that store the booking record
    private final String bookingRecordFilePath;

    // Constructor to create a DataStorage instance
    public DataStorage(String userDataFilePath, String bookingHistoryFilePath) {
        this.userDataFilePath = userDataFilePath;
        this.bookingRecordFilePath = bookingHistoryFilePath;
    }

    /* Read the local file that store the user data line by line, create student object, add them to a list and
        return the list. An empty list is returned if file doesn't exist.
     */
    public ArrayList<Student> loadStudents() {
        ArrayList<Student> studentsList = new ArrayList<>();
        Path path = Paths.get(userDataFilePath);
        if (!Files.exists(path)) {
            return studentsList;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(userDataFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length == 3) {
                    studentsList.add(new Student(parts[0].trim(), parts[1].trim(), parts[2].trim()));
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading student file: " + e.getMessage());
        }
        return studentsList;
    }

    /* Read the local file that store booking records line by line, create booking objects and add them into a list, then return the list.
       An empty list is returned if file doesn't exist.
     */
    public ArrayList<Booking> loadBookings() {
        ArrayList<Booking> bookingRecordList = new ArrayList<>();
        Path path = Paths.get(bookingRecordFilePath);
        if (!Files.exists(path)) {
            return bookingRecordList;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(bookingRecordFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\|");
                if (parts.length == 7) {
                    bookingRecordList.add(new Booking(
                            parts[0].trim(), parts[1].trim(), LocalDate.parse(parts[2].trim()),
                            LocalTime.parse(parts[3].trim()), LocalTime.parse(parts[4].trim()),
                            parts[5].trim(), Integer.parseInt(parts[6].trim())
                    ));
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading booking file: " + e.getMessage());
        }

        return bookingRecordList;
    }

    // Accept a booking object as parameter, convert it into a string, and write it into a local booking record file
    public void saveBooking(Booking booking) {
        Path path = Paths.get(bookingRecordFilePath);

        String line = String.format("%s | %s | %s | %s | %s | %s | %d",
                booking.getBookingRef(), booking.getResourceId(), booking.getDate(),
                booking.getStartTime(), booking.getEndTime(), booking.getStudentId(),
                booking.getStatus());

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(bookingRecordFilePath, true))) {
            // if the file is empty, write a header
            if (Files.size(path) == 0){
                bw.write("# bookingRef | resourceId | date | start | end | studentId | status");
                bw.newLine();
            }

            bw.write(line);
            bw.newLine();
        } catch (IOException e) {
            System.err.println("Failed to save booking: " + e.getMessage());
        }
    }

    /*  Accept the booking Reference number and new status as parameter, update the status of the booking with the
        corresponding booking reference number, then rewrite the local booking record file
    */
    public void updateBookingStatus(String bookingRef, int newStatus) {
        ArrayList<Booking> allBookingRecord = loadBookings();
        boolean bookingRecordExist = false;


        for (Booking bookingRecord : allBookingRecord) {
            if (bookingRecord.getBookingRef().equals(bookingRef)) {
                bookingRecordExist = true;
                bookingRecord.setStatus(newStatus);
                break;
            }
        }

        if (!bookingRecordExist) {
            System.err.println("Booking doesn't exist: " + bookingRef);
            return;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(bookingRecordFilePath))) {

            bw.write("# bookingRef | resourceId | date | start | end | studentId | status");
            bw.newLine();

            for (Booking bookingRecord : allBookingRecord) {
                String line = String.format("%s | %s | %s | %s | %s | %s | %d",
                        bookingRecord.getBookingRef(), bookingRecord.getResourceId(),
                        bookingRecord.getDate(), bookingRecord.getStartTime(),
                        bookingRecord.getEndTime(), bookingRecord.getStudentId(),
                        bookingRecord.getStatus()
                );
                bw.write(line);
                bw.newLine();
            }

        } catch (IOException e) {
            System.err.println("Failed to update booking: " + e.getMessage());
        }
    }
}

