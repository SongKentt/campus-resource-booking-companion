package com.campus.client.controller;

import com.campus.client.model.Student;
import com.campus.client.service.CampusService;
import com.campus.client.ui.LoginView;
import javafx.application.Platform;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles the Login button's onClick event (FR1).
 */
public class LoginController {

    private final LoginView view;
    private final CampusService service;
    private final Runnable onLoginSuccess;

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "login-worker");
        t.setDaemon(true);
        return t;
    });

    public LoginController(LoginView view, CampusService service, Runnable onLoginSuccess) {
        this.view = view;
        this.service = service;
        this.onLoginSuccess = onLoginSuccess;
        this.view.setLoginAction(this::handleLogin);
    }

    public void handleLogin() {
        view.clearFieldErrors();

        String studentId = view.getStudentId();
        String password = view.getPassword();

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

        view.setLoginButtonDisabled(true);

        worker.submit(() -> {
            try {
                // UPDATED: Use validateStudent() instead of login()
                boolean isValid = service.validateStudent(studentId, password);

                Platform.runLater(() -> {
                    view.setLoginButtonDisabled(false);
                    if (isValid) {
                        view.clearFields();
                        onLoginSuccess.run();
                    } else {
                        view.showPasswordError("Invalid Student ID or password.");
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    view.setLoginButtonDisabled(false);
                    view.showError("Could not reach the server. Please try again.\n(" + ex.getMessage() + ")");
                });
            }
        });
    }
}