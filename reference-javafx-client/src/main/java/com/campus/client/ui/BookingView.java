package com.campus.client.ui;

import com.campus.client.controller.BookingController;
import com.campus.client.model.Resource;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class BookingView extends BaseView {

    // ================================================================
    // UI COMPONENTS
    // ================================================================

    private ComboBox<String> resourceTypeCombo;
    private ComboBox<String> resourceIdCombo;
    private TextField studentIdField;
    private TextField dateField;
    private TextField startTimeField;
    private TextField endTimeField;

    private Label resourceIdError;
    private Label dateError;
    private Label startTimeError;
    private Label endTimeError;

    private Button checkAvailabilityButton;
    private Button submitButton;

    private BookingController controller;

    // ================================================================
    // CONSTRUCTOR
    // ================================================================

    public BookingView() {
        buildUI();
    }

    // ================================================================
    // UI BUILDER
    // ================================================================

    private void buildUI() {
        setPadding(new Insets(30));
        setSpacing(20);
        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-background-color: #F8F9FA;");

        // ---- Title ----
        Label titleLabel = new Label("Book a Resource");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        titleLabel.setStyle("-fx-text-fill: #2C3E50;");
        VBox.setMargin(titleLabel, new Insets(0, 0, 20, 0));

        // ---- Form Grid ----
        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(10);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 4);");

        int row = 0;

        // ---- Resource Type ----
        Label typeLabel = createLabel("Resource Type *");
        resourceTypeCombo = new ComboBox<>();
        // ===== REMOVED HARDCODED ITEMS - populated by controller =====
        resourceTypeCombo.setPromptText("Loading resources...");
        resourceTypeCombo.setPrefWidth(250);
        resourceTypeCombo.setDisable(true);
        resourceTypeCombo.setOnAction(evt -> {
            if (controller != null && resourceTypeCombo.getValue() != null) {
                controller.handleResourceTypeChanged(resourceTypeCombo.getValue());
            }
        });

        VBox typeBox = new VBox(2);
        typeBox.setAlignment(Pos.CENTER_LEFT);
        typeBox.getChildren().add(resourceTypeCombo);

        form.add(typeLabel, 0, row);
        form.add(typeBox, 1, row);
        GridPane.setValignment(typeBox, javafx.geometry.VPos.CENTER);
        row++;

        // ---- Resource ID ----
        Label idLabel = createLabel("Resource ID *");
        resourceIdCombo = new ComboBox<>();
        resourceIdCombo.setPromptText("Select a resource");
        resourceIdCombo.setPrefWidth(250);
        resourceIdCombo.setDisable(true);

        resourceIdError = new Label();
        resourceIdError.setStyle("-fx-text-fill: #C0392B; -fx-font-size: 11px;");
        resourceIdError.setVisible(false);

        VBox idBox = new VBox(2);
        idBox.setAlignment(Pos.CENTER_LEFT);
        idBox.getChildren().addAll(resourceIdCombo, resourceIdError);

        form.add(idLabel, 0, row);
        form.add(idBox, 1, row);
        GridPane.setValignment(idBox, javafx.geometry.VPos.CENTER);
        row++;

        // ---- Student ID ----
        Label studentLabel = createLabel("Student ID *");
        studentIdField = new TextField();
        studentIdField.setPromptText("Enter your Student ID");
        studentIdField.setPrefWidth(250);
        studentIdField.setEditable(false);
        studentIdField.setStyle("-fx-background-color: #f0f0f0;");

        form.add(studentLabel, 0, row);
        form.add(studentIdField, 1, row);
        GridPane.setValignment(studentIdField, javafx.geometry.VPos.CENTER);
        row++;

        // ---- Booking Date ----
        Label dateLabel = createLabel("Booking Date *");
        dateField = new TextField();
        dateField.setPromptText("yyyy-MM-dd");
        dateField.setPrefWidth(250);

        dateError = new Label();
        dateError.setStyle("-fx-text-fill: #C0392B; -fx-font-size: 11px;");
        dateError.setVisible(false);

        VBox dateBox = new VBox(2);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        dateBox.getChildren().addAll(dateField, dateError);

        form.add(dateLabel, 0, row);
        form.add(dateBox, 1, row);
        GridPane.setValignment(dateBox, javafx.geometry.VPos.CENTER);
        row++;

        // ---- Start Time ----
        Label startLabel = createLabel("Start Time *");
        startTimeField = new TextField();
        startTimeField.setPromptText("HH:mm");
        startTimeField.setPrefWidth(250);

        startTimeError = new Label();
        startTimeError.setStyle("-fx-text-fill: #C0392B; -fx-font-size: 11px;");
        startTimeError.setVisible(false);

        VBox startBox = new VBox(2);
        startBox.setAlignment(Pos.CENTER_LEFT);
        startBox.getChildren().addAll(startTimeField, startTimeError);

        form.add(startLabel, 0, row);
        form.add(startBox, 1, row);
        GridPane.setValignment(startBox, javafx.geometry.VPos.CENTER);
        row++;

        // ---- End Time ----
        Label endLabel = createLabel("End Time *");
        endTimeField = new TextField();
        endTimeField.setPromptText("HH:mm");
        endTimeField.setPrefWidth(250);

        endTimeError = new Label();
        endTimeError.setStyle("-fx-text-fill: #C0392B; -fx-font-size: 11px;");
        endTimeError.setVisible(false);

        VBox endBox = new VBox(2);
        endBox.setAlignment(Pos.CENTER_LEFT);
        endBox.getChildren().addAll(endTimeField, endTimeError);

        form.add(endLabel, 0, row);
        form.add(endBox, 1, row);
        GridPane.setValignment(endBox, javafx.geometry.VPos.CENTER);
        row++;

        // ---- Buttons Row ----
        HBox buttonRow = new HBox(15);
        buttonRow.setAlignment(Pos.CENTER);
        buttonRow.setPadding(new Insets(15, 0, 0, 0));

        checkAvailabilityButton = new Button("Check Availability");
        checkAvailabilityButton.setStyle(
                "-fx-background-color: #3498DB; -fx-text-fill: white; " +
                        "-fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6; " +
                        "-fx-cursor: hand;"
        );
        checkAvailabilityButton.setOnMouseEntered(evt -> checkAvailabilityButton.setStyle(
                "-fx-background-color: #2980B9; -fx-text-fill: white; " +
                        "-fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6; " +
                        "-fx-cursor: hand;"
        ));
        checkAvailabilityButton.setOnMouseExited(evt -> checkAvailabilityButton.setStyle(
                "-fx-background-color: #3498DB; -fx-text-fill: white; " +
                        "-fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6; " +
                        "-fx-cursor: hand;"
        ));
        checkAvailabilityButton.setOnAction(evt -> handleCheckAvailability());

        submitButton = new Button("Submit Booking");
        submitButton.setStyle(
                "-fx-background-color: #2ECC71; -fx-text-fill: white; " +
                        "-fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6; " +
                        "-fx-cursor: hand;"
        );
        submitButton.setOnMouseEntered(evt -> submitButton.setStyle(
                "-fx-background-color: #27AE60; -fx-text-fill: white; " +
                        "-fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6; " +
                        "-fx-cursor: hand;"
        ));
        submitButton.setOnMouseExited(evt -> submitButton.setStyle(
                "-fx-background-color: #2ECC71; -fx-text-fill: white; " +
                        "-fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6; " +
                        "-fx-cursor: hand;"
        ));
        submitButton.setOnAction(evt -> handleSubmitBooking());

        buttonRow.getChildren().addAll(checkAvailabilityButton, submitButton);
        form.add(buttonRow, 0, row, 2, 1);
        GridPane.setValignment(buttonRow, javafx.geometry.VPos.CENTER);
        row++;

        // ---- Assemble ----
        VBox contentBox = new VBox(titleLabel, form);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.setMaxWidth(700);
        getChildren().add(contentBox);
    }

    // ================================================================
    // HELPER METHODS
    // ================================================================

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 13));
        label.setAlignment(Pos.CENTER_LEFT);
        return label;
    }

    // ================================================================
    // PUBLIC METHODS FOR CONTROLLER
    // ================================================================

    /**
     * ===== ADD THIS METHOD =====
     * Updates the resource type dropdown with available types from the controller.
     * This makes the dropdown dynamic based on what's loaded from facilities.txt.
     */
    public void updateResourceTypeOptions(List<String> types) {
        resourceTypeCombo.getItems().clear();
        if (types != null && !types.isEmpty()) {
            resourceTypeCombo.getItems().addAll(types);
            resourceTypeCombo.setPromptText("Select a resource type");
            resourceTypeCombo.setDisable(false);
        } else {
            resourceTypeCombo.setPromptText("No resources available");
            resourceTypeCombo.setDisable(true);
        }
    }

    public void setController(BookingController controller) {
        this.controller = controller;
    }

    public void setStudentId(String studentId) {
        studentIdField.setText(studentId);
    }

    public void preselectResourceType(String resourceType) {
        if (resourceTypeCombo.getItems().contains(resourceType)) {
            resourceTypeCombo.setValue(resourceType);
            if (controller != null) {
                controller.handleResourceTypeChanged(resourceType);
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Unavailable");
            alert.setHeaderText("Resource Type Unavailable");
            alert.setContentText("This resource type is currently unavailable. Please try another.");

            ButtonType returnHomeButton = new ButtonType("Return to Home");
            alert.getButtonTypes().setAll(returnHomeButton, ButtonType.CANCEL);

            alert.showAndWait().ifPresent(response -> {
                if (response == returnHomeButton) {
                    navigateToHome();
                }
            });
        }
    }

    private void navigateToHome() {
        if (getScene() != null && getScene().getRoot() instanceof MainView) {
            MainView mainView = (MainView) getScene().getRoot();
            mainView.showHome();
            return;
        }

        Node parent = this;
        while (parent != null) {
            if (parent instanceof MainView) {
                ((MainView) parent).showHome();
                return;
            }
            parent = parent.getParent();
        }

        if (getScene() != null) {
            Node root = getScene().getRoot();
            if (root instanceof MainView) {
                ((MainView) root).showHome();
                return;
            }
        }

        System.err.println("Could not find MainView to navigate home.");
    }

    public void updateResourceIdOptions(List<Resource> resources) {
        resourceIdCombo.getItems().clear();
        resourceIdCombo.setDisable(resources.isEmpty());

        if (resources.isEmpty()) {
            resourceIdCombo.setPromptText("No resources available for this type");
            return;
        }

        for (Resource r : resources) {
            resourceIdCombo.getItems().add(r.getResourceId());
        }
        resourceIdCombo.setPromptText("Select a resource");
        resourceIdCombo.setDisable(false);
    }

    public void showAvailableResources(List<Resource> resources) {
        updateResourceIdOptions(resources);
        if (!resources.isEmpty()) {
            showSuccess("Found " + resources.size() + " available resource(s).");
        }
    }

    public void showAvailabilityResult(String result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Availability Check");
        alert.setHeaderText("Resource Availability");
        alert.setContentText(result);
        alert.showAndWait();
    }

    public void setFormEnabled(boolean enabled) {
        resourceTypeCombo.setDisable(!enabled);
        resourceIdCombo.setDisable(!enabled || resourceIdCombo.getItems().isEmpty());
        dateField.setDisable(!enabled);
        startTimeField.setDisable(!enabled);
        endTimeField.setDisable(!enabled);
        submitButton.setDisable(!enabled);
        checkAvailabilityButton.setDisable(!enabled);
    }

    public void showBookingConfirmation(String reference) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Booking Confirmed");
        alert.setHeaderText("Booking Successful!");
        alert.setContentText("Reference No: " + reference + "\nYour booking has been confirmed.");
        alert.showAndWait();
        clearFields();
    }

    public void showDuplicateWarning() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Duplicate Booking");
        alert.setHeaderText("Booking Conflict");
        alert.setContentText("This resource is already booked for the selected time slot. Please choose a different time or resource.");
        alert.showAndWait();
    }

    // ================================================================
    // EVENT HANDLERS
    // ================================================================

    private void handleCheckAvailability() {
        if (controller == null) {
            showError("Controller not connected.");
            return;
        }

        String resourceId = resourceIdCombo.getValue();
        String date = dateField.getText().trim();
        String startTime = startTimeField.getText().trim();
        String endTime = endTimeField.getText().trim();

        if (resourceId == null || resourceId.isBlank()) {
            showError("Please select a resource first.");
            return;
        }

        if (date.isEmpty()) {
            showError("Please enter a date.");
            return;
        }

        if (startTime.isEmpty()) {
            showError("Please enter a start time.");
            return;
        }

        if (endTime.isEmpty()) {
            showError("Please enter an end time.");
            return;
        }

        controller.handleCheckAvailability(resourceId, date, startTime, endTime);
    }

    private void handleSubmitBooking() {
        if (controller == null) {
            showError("Controller not connected.");
            return;
        }

        String resourceId = resourceIdCombo.getValue();
        String studentId = studentIdField.getText().trim();
        String date = dateField.getText().trim();
        String startTime = startTimeField.getText().trim();
        String endTime = endTimeField.getText().trim();

        controller.handleBookResource(resourceId, studentId, date, startTime, endTime);
    }

    // ================================================================
    // ERROR HANDLING
    // ================================================================

    public void clearAllErrors() {
        hideError(resourceIdError);
        hideError(dateError);
        hideError(startTimeError);
        hideError(endTimeError);
        resourceIdCombo.setStyle("-fx-background-radius: 14; -fx-border-radius: 14;");
        dateField.setStyle("-fx-background-radius: 14; -fx-border-radius: 14;");
        startTimeField.setStyle("-fx-background-radius: 14; -fx-border-radius: 14;");
        endTimeField.setStyle("-fx-background-radius: 14; -fx-border-radius: 14;");
    }

    public void setResourceIdError(String message) {
        resourceIdError.setText(message);
        resourceIdError.setVisible(true);
        resourceIdCombo.setStyle("-fx-border-color: #C0392B; -fx-border-radius: 14;");
    }

    public void setDateError(String message) {
        dateError.setText(message);
        dateError.setVisible(true);
        dateField.setStyle("-fx-border-color: #C0392B; -fx-border-radius: 14;");
    }

    public void setStartTimeError(String message) {
        startTimeError.setText(message);
        startTimeError.setVisible(true);
        startTimeField.setStyle("-fx-border-color: #C0392B; -fx-border-radius: 14;");
    }

    public void setEndTimeError(String message) {
        endTimeError.setText(message);
        endTimeError.setVisible(true);
        endTimeField.setStyle("-fx-border-color: #C0392B; -fx-border-radius: 14;");
    }

    private void hideError(Label label) {
        label.setText("");
        label.setVisible(false);
    }

    public void clearFields() {
        resourceIdCombo.getItems().clear();
        resourceIdCombo.setPromptText("Select a resource");
        resourceIdCombo.setDisable(true);
        dateField.clear();
        startTimeField.clear();
        endTimeField.clear();
        clearAllErrors();
        setFormEnabled(true);
    }

    @Override
    public void onShow() {
        // Nothing needed when view is shown
    }

    @Override
    public void onHide() {
        // Nothing needed when view is hidden
    }
}