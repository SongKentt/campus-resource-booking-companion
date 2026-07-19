package com.campus.client.controller;

import com.campus.client.service.CampusService;
import com.campus.client.ui.LoginView;
import javafx.application.Platform;

import java.util.function.Consumer;


public class LoginController {

    private final LoginView view;
    private final CampusService service;
    private final Consumer<String> onLoginSuccess; // callback to pass the student id

    public LoginController(LoginView view, CampusService service, Consumer<String> onLoginSuccess) {
        this.view = view;
        this.service = service;
        this.onLoginSuccess = onLoginSuccess;
        this.view.setController(this);
    }

    /* Method that is called when the login button was clicked by the user and perform validation on the login fields
       input then update the UI
     */
    public void handleLogin() {
        view.clearFieldErrors();

        // Received input from the view
        String studentId = view.getStudentId();
        String password = view.getPassword();

        // First check if the input is totally empty or not, assume it is not empty
        boolean valid = true;
        if (studentId.isEmpty()) {
            view.showStudentIdError("Please enter a valid Student ID");
            valid = false;
        }
        if (password.isEmpty()) {
            view.showPasswordError("Please enter a valid password");
            valid = false;
        }
        if (!valid) {
            return;
        }

        // Disable the button to prevent the user to double-click it
        view.setLoginButtonDisabled(true);

        // A background Thread is created
        Thread loginValidationThread = new Thread(() -> {
            try {
                // Call the campus service method to validate the student id and password entered
                boolean isValid = service.validateStudent(studentId, password);

                // UI update done by the UI thread
                Platform.runLater(() -> {
                    view.setLoginButtonDisabled(false);
                    if (isValid) {
                        view.clearFields();
                        // Send the Student ID back to MainView
                        onLoginSuccess.accept(studentId);
                    } else {
                        view.showPasswordError("Invalid Student ID or password.");
                    }
                });

            } catch (Exception ex) {
                // UI update done by the UI thread if there is any error
                Platform.runLater(() -> {
                    view.setLoginButtonDisabled(false);
                    view.showError("Could not reach the server. Please try again.\n");
                });
            }
        });

        // Execute the background thread
        loginValidationThread.start();
    }
}