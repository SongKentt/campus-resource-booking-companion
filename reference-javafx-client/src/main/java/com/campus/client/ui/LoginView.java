package com.campus.client.ui;

import com.campus.client.controller.LoginController;
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

    // Components for the login UI
    private TextField studentIdField;
    private PasswordField passwordField;
    private Label studentIdError;
    private Label passwordError;
    private Button loginButton;

    // Reference to the Controller
    private LoginController controller;

    public LoginView() {
        buildUI();
    }

    // Called by MainView to give the controller reference
    public void setController(LoginController controller) {
        this.controller = controller;
    }

    // Build the login card UI
    private void buildUI() {
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #ECF0F1;");
        setPrefSize(900, 650);

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

        card.getChildren().addAll(buildLogoPlaceholder(), buildForm());
        getChildren().add(card);
    }

    // Creates a CR logo panel on the left
    private StackPane buildLogoPlaceholder() {
        StackPane box = new StackPane();
        box.setPrefSize(260, 380);
        box.setMaxSize(260, 380);
        box.setStyle(
                "-fx-background-color: #D5D5D5; " +
                        "-fx-border-color: #9A9A9A; " +
                        "-fx-background-radius: 10 0 0 10;"
        );

        Label logoLabel = new Label("CR");
        logoLabel.setFont(Font.font("System", FontWeight.BOLD, 48));
        logoLabel.setStyle("-fx-text-fill: #7F8C8D;");
        box.getChildren().add(logoLabel);
        return box;
    }

    // Create a login form panel on the right with multiple login input fields and a login button
    private VBox buildForm() {
        VBox form = new VBox(10);
        form.setPadding(new Insets(30, 40, 30, 40));
        form.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(form, Priority.ALWAYS);

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

        // Event Handling where clicking the login button or press enter in the text field will call the attemptlogin()
        studentIdField.setOnAction(_ -> attemptLogin());
        passwordField.setOnAction(_ -> attemptLogin());
        loginButton.setOnAction(_ -> attemptLogin());

        form.getChildren().addAll(
                appName,
                studentIdLabel, studentIdField, studentIdError,
                passwordLabel, passwordField, passwordError,
                loginButton
        );
        return form;
    }

    // This method will be called when the user attempt to log in. It will pass it to the controller to validate the login.
    private void attemptLogin() {
        if (controller != null) {
            controller.handleLogin();
        } else {
            showError("Login controller is not set.");
        }
    }

    // Getters for the Controller
    public String getStudentId() {
        return studentIdField.getText() == null ? "" : studentIdField.getText().trim();
    }

    public String getPassword() {
        return passwordField.getText() == null ? "" : passwordField.getText();
    }

    // Methods to display error
    public void showStudentIdError(String message) {
        studentIdError.setText(message);
        showFieldError(studentIdError);
        studentIdField.setStyle("-fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #C0392B;");
    }

    public void showPasswordError(String message) {
        passwordError.setText(message);
        showFieldError(passwordError);
        passwordField.setStyle("-fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #C0392B;");
    }

    public void clearFieldErrors() {
        hideFieldError(studentIdError);
        hideFieldError(passwordError);
        studentIdField.setStyle("-fx-background-radius: 14; -fx-border-radius: 14;");
        passwordField.setStyle("-fx-background-radius: 14; -fx-border-radius: 14;");
    }

    private void showFieldError(Label label) {
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideFieldError(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    // Disables the login button and changes its text to indicate progress
    public void setLoginButtonDisabled(boolean disabled) {
        loginButton.setDisable(disabled);
        loginButton.setText(disabled ? "Logging in..." : "Login");
    }

    // Clear all the input fields, reset all the error states, enable the button
    public void clearFields() {
        studentIdField.clear();
        passwordField.clear();
        clearFieldErrors();
        setLoginButtonDisabled(false);
    }

    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Login Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void onShow() {
        clearFields();
        studentIdField.requestFocus();
    }

    @Override
    public void onHide() {
        // Nothing to do
    }
}