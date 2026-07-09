package com.campus.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;        // ← ADD THIS IMPORT
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;

/**
 * Login view for Campus Resource Booking Companion.
 */
public class LoginView extends BaseView {

    private TextField studentIdField;
    private PasswordField passwordField;
    private Label studentIdError;
    private Label passwordError;
    private Button loginButton;

    private Runnable loginAction;

    public LoginView() {
        buildUI();
    }

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

        card.getChildren().addAll(buildImagePlaceholder(), buildForm());
        getChildren().add(card);
    }

    private StackPane buildImagePlaceholder() {
        StackPane box = new StackPane();
        box.setPrefSize(260, 380);
        box.setMaxSize(260, 380);
        box.setStyle(
                "-fx-background-color: #D5D5D5; " +
                        "-fx-border-color: #9A9A9A; " +
                        "-fx-background-radius: 10 0 0 10;"
        );

        Line diagonal1 = new Line(20, 20, 240, 360);
        Line diagonal2 = new Line(240, 20, 20, 360);
        diagonal1.setStroke(javafx.scene.paint.Color.web("#9A9A9A"));
        diagonal2.setStroke(javafx.scene.paint.Color.web("#9A9A9A"));

        box.getChildren().addAll(diagonal1, diagonal2);
        return box;
    }

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

        loginButton = new Button("Login");
        loginButton.setId("loginButton");
        loginButton.setPrefWidth(150);
        loginButton.setStyle(
                "-fx-background-color: #D9D9D9; -fx-text-fill: #2C2C2C; -fx-font-size: 13px; " +
                        "-fx-padding: 8 0; -fx-background-radius: 6; -fx-cursor: hand;"
        );
        VBox.setMargin(loginButton, new Insets(24, 0, 0, 0));

        // Fix: Use underscore for unused parameters to suppress warnings
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

    private void attemptLogin() {
        if (loginAction != null) {
            loginAction.run();
        } else {
            showError("Login is not connected to the service yet.");
        }
    }

    public void setLoginAction(Runnable loginAction) {
        this.loginAction = loginAction;
    }

    public String getStudentId() {
        return studentIdField.getText() == null ? "" : studentIdField.getText().trim();
    }

    public String getPassword() {
        return passwordField.getText() == null ? "" : passwordField.getText();
    }

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

    public void setLoginButtonDisabled(boolean disabled) {
        loginButton.setDisable(disabled);
        loginButton.setText(disabled ? "Logging in..." : "Login");
    }

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
        // Nothing to release yet
    }
}