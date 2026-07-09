package com.campus.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LoginView extends BaseView {
    // UI Components
    private TextField studentIdField;
    private PasswordField passwordField;
    private Label studentIdError;
    private Label passwordError;
    private Button loginButton;

    private Runnable loginAction;

    // Constructor
    public LoginView() {
        buildUI(); // Builds the entire login screen
    }

    //UI Builder
    private void buildUI() {
        // Main container setup
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #ECF0F1;");
        setPrefSize(900, 650);

        // Main card
        HBox card = new HBox(0);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxSize(720, 380);
        card.setPrefSize(720, 380);
        card.setId("loginCard");
        card.setStyle(
                "-fx-background-color: #B0B0B0; " +
                        "-fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 8);"
        );

        // Add left and right sides to card
        card.getChildren().addAll(buildLogoPlaceholder(), buildForm());
        getChildren().add(card);  // Add card to main container
    }


    // Logo Placeholder
    private StackPane buildLogoPlaceholder() {
        StackPane box = new StackPane();
        box.setPrefSize(260, 380);
        box.setMaxSize(260, 380);
        box.setStyle(
                "-fx-background-color: #D5D5D5; " +
                        "-fx-border-color: #9A9A9A; " +
                        "-fx-background-radius: 10 0 0 10;"
        );

        // "CR" text logo (Campus Resource)
        Label logoLabel = new Label("CR");
        logoLabel.setFont(Font.font("System", FontWeight.BOLD, 48));
        logoLabel.setStyle("-fx-text-fill: #7F8C8D;");
        box.getChildren().add(logoLabel);

        return box;
    }

    // Login Form
    private VBox buildForm() {
        VBox form = new VBox(10);
        form.setPadding(new Insets(30, 40, 30, 40));
        form.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(form, Priority.ALWAYS);

        // App Name
        Label appName = new Label("Campus Resource Booking Companion");
        appName.setFont(Font.font(16));
        appName.setStyle("-fx-font-weight: bold;");
        appName.setWrapText(true);
        VBox.setMargin(appName, new Insets(0, 0, 20, 0));

        // Student ID Field
        Label studentIdLabel = new Label("Student ID");
        studentIdField = new TextField();
        studentIdField.setId("studentIdField");
        studentIdField.setMaxWidth(260);
        studentIdField.setStyle("-fx-background-radius: 14; -fx-border-radius: 14;");

        // Student ID Error
        studentIdError = new Label("");
        studentIdError.setId("studentIdError");
        studentIdError.setStyle("-fx-text-fill: #C0392B; -fx-font-size: 11px;");
        studentIdError.setWrapText(true);
        studentIdError.setMaxWidth(260);
        hideFieldError(studentIdError);

        // Password Field
        Label passwordLabel = new Label("Password");
        VBox.setMargin(passwordLabel, new Insets(8, 0, 0, 0));
        passwordField = new PasswordField();
        passwordField.setId("passwordField");
        passwordField.setMaxWidth(260);
        passwordField.setStyle("-fx-background-radius: 14; -fx-border-radius: 14;");

        // Password Error
        passwordError = new Label("");
        passwordError.setId("passwordError");
        passwordError.setStyle("-fx-text-fill: #C0392B; -fx-font-size: 11px;");
        passwordError.setWrapText(true);
        passwordError.setMaxWidth(260);
        hideFieldError(passwordError);

        // Login Button
        loginButton = new Button("Login");
        loginButton.setId("loginButton");
        loginButton.setPrefWidth(150);
        loginButton.setStyle(
                "-fx-background-color: #D9D9D9; -fx-text-fill: #2C2C2C; -fx-font-size: 13px; " +
                        "-fx-padding: 8 0; -fx-background-radius: 6; -fx-cursor: hand;"
        );
        VBox.setMargin(loginButton, new Insets(24, 0, 0, 0));

        // Event Handlers
        studentIdField.setOnAction(_ -> attemptLogin()); // Enter key on Student ID
        passwordField.setOnAction(_ -> attemptLogin());  // Enter key on Password
        loginButton.setOnAction(_ -> attemptLogin());    // Button click

        // Add everything to form
        form.getChildren().addAll(
                appName,
                studentIdLabel, studentIdField, studentIdError,
                passwordLabel, passwordField, passwordError,
                loginButton
        );
        return form;
    }

    // Login Action
    private void attemptLogin() {
        if (loginAction != null) {
            loginAction.run();                    // Run the login logic
        } else {
            showError("Login is not connected to the service yet.");
        }
    }

    /**
     * Sets the login action to run when Login is clicked.
     *
     * <p>Called by MainView to connect the login logic.</p>
     *
     * @param loginAction The action to run (usually in MainView)
     */
    public void setLoginAction(Runnable loginAction) {
        this.loginAction = loginAction;
    }


    //Getters
    public String getStudentId() {
        return studentIdField.getText() == null ? "" : studentIdField.getText().trim();
    }

    public String getPassword() {
        return passwordField.getText() == null ? "" : passwordField.getText();
    }

    //Error display
    public void showStudentIdError(String message) {
        studentIdError.setText(message);
        showFieldError(studentIdError);
        // Add red border around the field
        studentIdField.setStyle("-fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #C0392B;");
    }

    //Shows an error message under the Password field (FR7).
    public void showPasswordError(String message) {
        passwordError.setText(message);
        showFieldError(passwordError);
        // Add red border around the field
        passwordField.setStyle("-fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #C0392B;");
    }


     //Clears all error messages and removes red borders.
    public void clearFieldErrors() {
        hideFieldError(studentIdError);
        hideFieldError(passwordError);
        studentIdField.setStyle("-fx-background-radius: 14; -fx-border-radius: 14;");
        passwordField.setStyle("-fx-background-radius: 14; -fx-border-radius: 14;");
    }


    //Error Helpers

     //Shows a specific error label.

    private void showFieldError(Label label) {
        label.setVisible(true);
        label.setManaged(true);
    }

     //Hides a specific error label
    private void hideFieldError(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    // Button State
     //Enables or disables the Login button
    public void setLoginButtonDisabled(boolean disabled) {
        loginButton.setDisable(disabled);
        loginButton.setText(disabled ? "Logging in..." : "Login");
    }


    // Field Clearing


     //Clears all input fields and errors.
    public void clearFields() {
        studentIdField.clear();
        passwordField.clear();
        clearFieldErrors();
        setLoginButtonDisabled(false);
    }

    // General Error Dialog

     // Shows a general error popup dialog.
    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Login Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // BaseView Lifecycle


      //Called when the login screen becomes visible.
    @Override
    public void onShow() {
        clearFields();
        studentIdField.requestFocus();
    }


      //Called when the login screen becomes hidden.
    @Override
    public void onHide() {
        // Nothing to release yet
    }
}