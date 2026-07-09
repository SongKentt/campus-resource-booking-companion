package com.campus.client.ui;

import com.campus.client.controller.BookingController;
import com.campus.client.model.Resource;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;

import java.util.List;

/**
 * The Resource Booking screen - lets a student pick a resource type, pick an
 * actual resource, and submit a booking (FR2, FR3).
 *
 * NAVBAR: This screen does NOT have its own navbar.
 * It relies on MainView's shared navbar for navigation.
 */
public class BookingView extends BaseView {

    private BookingController controller;

    // form fields
    private final ComboBox<String> resourceTypeCombo = new ComboBox<>();
    private final TextField resourceIdField = new TextField();
    private String selectedResourceId;

    private final TextField studentIdField = new TextField();
    private final TextField dateField = new TextField();
    private final TextField startTimeField = new TextField();
    private final TextField endTimeField = new TextField();
    private final Button submitButton = new Button("Submit Booking");

    // inline error messages under each field
    private final Label resourceTypeError = new Label();
    private final Label resourceIdError   = new Label();
    private final Label studentIdError    = new Label();
    private final Label dateError         = new Label();
    private final Label startTimeError    = new Label();
    private final Label endTimeError      = new Label();

    // popup that shows up when you click the resource ID field
    private final Popup resourceIdPopup = new Popup();
    private final TableView<Resource> resourceIdTable = new TableView<>();

    // small floating card that shows the booking result
    private final Label confirmationLabel = new Label();
    private final Button confirmationButton = new Button("OK");
    private final VBox confirmationCard = new VBox(10, confirmationLabel, confirmationButton);

    private final ProgressIndicator loadingIndicator = new ProgressIndicator();

    public BookingView() {
        buildLayout();
        wireEvents();
    }

    public void setController(BookingController controller) {
        this.controller = controller;
    }

    @Override
    public void onShow() {
        clearAllErrors();
        hideConfirmationCard();
    }

    @Override
    public void onHide() {
    }

    // ---------------------------------------------------------------
    // building the screen
    // ---------------------------------------------------------------

    private void buildLayout() {
        setStyle("-fx-background-color: #f5f5f5;");

        VBox formCard = buildFormCard();
        buildResourceIdPopup();
        buildConfirmationCard();

        StackPane formWrapper = new StackPane(formCard);
        formWrapper.setPadding(new Insets(30));

        ScrollPane scrollPane = new ScrollPane(formWrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: #f5f5f5; -fx-background-color: #f5f5f5;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        StackPane contentStack = new StackPane(scrollPane, confirmationCard);
        StackPane.setAlignment(confirmationCard, Pos.CENTER);
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        // ✅ REMOVED: buildNavBar() - uses MainView's shared navbar
        getChildren().addAll(buildHeroBanner("Book a Resource"), contentStack);
    }

    /** Navy title banner under the navbar. */
    private VBox buildHeroBanner(String title) {
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        titleLabel.setTextFill(Color.WHITE);

        VBox banner = new VBox(titleLabel);
        banner.setAlignment(Pos.CENTER);
        banner.setPrefHeight(130);
        banner.setStyle("-fx-background-color: #1F3864;");
        return banner;
    }

    private VBox buildFormCard() {
        resourceTypeCombo.setPromptText("Select resource type");
        resourceTypeCombo.setMaxWidth(Double.MAX_VALUE);
        styleErrorLabel(resourceTypeError);

        resourceIdField.setEditable(false);
        resourceIdField.setPromptText("Click to select a resource");
        resourceIdField.setMaxWidth(Double.MAX_VALUE);
        styleErrorLabel(resourceIdError);

        studentIdField.setPromptText("Student ID");
        studentIdField.setEditable(false);
        studentIdField.setStyle("-fx-background-color: #F0F0F0;");
        styleErrorLabel(studentIdError);

        dateField.setPromptText("yyyy-MM-dd");
        styleErrorLabel(dateError);
        startTimeField.setPromptText("hh:MM");
        styleErrorLabel(startTimeError);
        endTimeField.setPromptText("hh:MM");
        styleErrorLabel(endTimeError);

        submitButton.setPrefWidth(220);
        submitButton.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        submitButton.setPrefHeight(40);
        submitButton.setStyle("-fx-background-color: #1F3864; -fx-text-fill: white; "
                + "-fx-background-radius: 4;");

        loadingIndicator.setMaxSize(28, 28);
        loadingIndicator.setVisible(false);
        HBox submitRow = new HBox(12, submitButton, loadingIndicator);
        submitRow.setAlignment(Pos.CENTER);

        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(4);
        grid.getColumnConstraints().addAll(columnPercent(50), columnPercent(50));
        grid.add(fieldBlock("Resource Type *", resourceTypeCombo, resourceTypeError), 0, 0);
        grid.add(fieldBlock("Resource ID *", resourceIdField, resourceIdError), 1, 0);
        grid.add(fieldBlock("Student ID *", studentIdField, studentIdError), 0, 1);
        grid.add(fieldBlock("Booking Date *", dateField, dateError), 1, 1);
        grid.add(fieldBlock("Start Time *", startTimeField, startTimeError), 0, 2);
        grid.add(fieldBlock("End Time *", endTimeField, endTimeError), 1, 2);

        VBox formCard = new VBox(20, grid, new Separator(), submitRow);
        formCard.setAlignment(Pos.CENTER);
        formCard.setPadding(new Insets(24, 40, 24, 40));
        formCard.setMaxWidth(640);
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);");
        return formCard;
    }

    private VBox fieldBlock(String labelText, Control field, Label errorLabel) {
        Label label = new Label(labelText);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        label.setTextFill(Color.web("#333333"));
        return new VBox(4, label, field, errorLabel);
    }

    private ColumnConstraints columnPercent(double percent) {
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(percent);
        return c;
    }

    @SuppressWarnings("unchecked")
    private void buildResourceIdPopup() {
        TableColumn<Resource, String> idCol = new TableColumn<>("Resource ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("resourceId"));

        TableColumn<Resource, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("resourceName"));

        TableColumn<Resource, String> buildingCol = new TableColumn<>("Building");
        buildingCol.setCellValueFactory(new PropertyValueFactory<>("building"));

        TableColumn<Resource, Number> capacityCol = new TableColumn<>("Capacity");
        capacityCol.setCellValueFactory(new PropertyValueFactory<>("capacity"));

        resourceIdTable.getColumns().setAll(List.of(idCol, nameCol, buildingCol, capacityCol));
        resourceIdTable.setPrefSize(400, 160);
        resourceIdTable.setPlaceholder(new Label("No resources available"));
        resourceIdTable.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);");

        resourceIdTable.setRowFactory(tv -> {
            TableRow<Resource> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty()) {
                    selectResourceId(row.getItem());
                }
            });
            return row;
        });

        resourceIdPopup.getContent().add(resourceIdTable);
        resourceIdPopup.setAutoHide(true);
    }

    private void selectResourceId(Resource resource) {
        selectedResourceId = resource.getResourceId();
        resourceIdField.setText(resource.getResourceId() + " - " + resource.getResourceName());
        resourceIdPopup.hide();
    }

    private void checkAvailability() {
        if (controller != null) {
            controller.handleCheckAvailability(dateField.getText().trim());
        }
    }

    private void openResourceIdPopup() {
        if (resourceIdTable.getItems().isEmpty()) {
            resourceIdError.setText("No resources are free on the selected date");
            resourceIdError.setVisible(true);
            return;
        }
        Bounds bounds = resourceIdField.localToScreen(resourceIdField.getBoundsInLocal());
        resourceIdPopup.show(resourceIdField, bounds.getMinX(), bounds.getMaxY());
    }

    private void buildConfirmationCard() {
        confirmationLabel.setWrapText(true);
        confirmationLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        confirmationLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        confirmationButton.setPrefWidth(100);
        confirmationButton.setStyle("-fx-background-color: #1F3864; -fx-text-fill: white; "
                + "-fx-background-radius: 4;");
        confirmationButton.setOnAction(e -> hideConfirmationCard());

        confirmationCard.setAlignment(Pos.CENTER);
        confirmationCard.setMaxWidth(240);
        confirmationCard.setMaxHeight(Region.USE_PREF_SIZE);
        confirmationCard.setPadding(new Insets(16));
        confirmationCard.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 12, 0, 0, 3);");

        confirmationCard.setVisible(false);
        confirmationCard.setManaged(false);
    }

    private void showConfirmationCard() {
        confirmationCard.setVisible(true);
        confirmationCard.setManaged(true);
    }

    private void hideConfirmationCard() {
        confirmationCard.setVisible(false);
        confirmationCard.setManaged(false);
    }

    private void wireEvents() {
        resourceTypeCombo.setOnAction(e -> {
            clearAllErrors();
            selectedResourceId = null;
            resourceIdField.clear();
            if (controller != null) {
                controller.handleResourceTypeChanged(resourceTypeCombo.getValue());
            }
        });

        resourceIdField.setOnMouseClicked(e -> {
            clearAllErrors();
            if (resourceIdPopup.isShowing()) {
                resourceIdPopup.hide();
                return;
            }
            checkAvailability();
        });

        submitButton.setOnAction(e -> bookResource());
    }

    private void bookResource() {
        if (controller != null) {
            controller.handleBookResource(
                    selectedResourceId,
                    studentIdField.getText().trim(),
                    dateField.getText().trim(),
                    startTimeField.getText().trim(),
                    endTimeField.getText().trim()
            );
        }
    }

    // ---------------------------------------------------------------
    // Controller calls these to update the UI
    // ---------------------------------------------------------------

    public void setStudentId(String studentId) {
        studentIdField.setText(studentId);
    }

    public void setResourceTypes(List<String> types) {
        resourceTypeCombo.setItems(FXCollections.observableArrayList(types));
    }

    public void updateResourceIdOptions(List<Resource> resources) {
        resourceIdTable.setItems(FXCollections.observableArrayList(resources));
        resourceIdField.clear();
        selectedResourceId = null;
    }

    public void showAvailableResources(List<Resource> resources) {
        resourceIdTable.setItems(FXCollections.observableArrayList(resources));
        resourceIdField.clear();
        selectedResourceId = null;
        openResourceIdPopup();
    }

    public void showBookingConfirmation(String referenceNumber) {
        confirmationLabel.setText("Booking Successful!\n\nReference No: " + referenceNumber
                + "\n\nAdded to your Upcoming Bookings.");
        confirmationLabel.setTextFill(Color.web("#27AE60"));
        confirmationButton.setText("OK");
        showConfirmationCard();
        setFormEnabled(true);
        resetForm();
    }

    public void showDuplicateWarning() {
        confirmationLabel.setText("Booking Failed.\nDuplicate booking found.");
        confirmationLabel.setTextFill(Color.web("#E67E22"));
        confirmationButton.setText("Try Again");
        showConfirmationCard();
        setFormEnabled(true);
    }

    public void setFormEnabled(boolean enabled) {
        submitButton.setDisable(!enabled);
        resourceTypeCombo.setDisable(!enabled);
        resourceIdField.setDisable(!enabled);
        dateField.setDisable(!enabled);
        startTimeField.setDisable(!enabled);
        endTimeField.setDisable(!enabled);
        loadingIndicator.setVisible(!enabled);
    }

    // ---------------------------------------------------------------
    // Error messages
    // ---------------------------------------------------------------

    public void setResourceTypeError(String message) {
        resourceTypeError.setText(message);
        resourceTypeError.setVisible(true);
        markFieldInvalid(resourceTypeCombo, true);
    }

    public void setResourceIdError(String message) {
        resourceIdError.setText(message);
        resourceIdError.setVisible(true);
        markFieldInvalid(resourceIdField, true);
    }

    public void setDateError(String message) {
        dateError.setText(message);
        dateError.setVisible(true);
        markFieldInvalid(dateField, true);
    }

    public void setStartTimeError(String message) {
        startTimeError.setText(message);
        startTimeError.setVisible(true);
        markFieldInvalid(startTimeField, true);
    }

    public void setEndTimeError(String message) {
        endTimeError.setText(message);
        endTimeError.setVisible(true);
        markFieldInvalid(endTimeField, true);
    }

    public void clearAllErrors() {
        resourceTypeError.setVisible(false);
        resourceIdError.setVisible(false);
        studentIdError.setVisible(false);
        dateError.setVisible(false);
        startTimeError.setVisible(false);
        endTimeError.setVisible(false);

        markFieldInvalid(resourceTypeCombo, false);
        markFieldInvalid(resourceIdField, false);
        markFieldInvalid(dateField, false);
        markFieldInvalid(startTimeField, false);
        markFieldInvalid(endTimeField, false);
    }

    public void resetForm() {
        resourceTypeCombo.getSelectionModel().clearSelection();
        resourceIdTable.getItems().clear();
        resourceIdField.clear();
        selectedResourceId = null;
        dateField.clear();
        startTimeField.clear();
        endTimeField.clear();
        clearAllErrors();
    }

    @Override
    public void showError(String message) {
        confirmationLabel.setText("Booking Failed.\n" + message);
        confirmationLabel.setTextFill(Color.web("#C0392B"));
        confirmationButton.setText("Try Again");
        showConfirmationCard();
        setFormEnabled(true);
    }

    @Override
    public void showSuccess(String message) {
        confirmationLabel.setText(message);
        confirmationLabel.setTextFill(Color.web("#27AE60"));
        confirmationButton.setText("OK");
        showConfirmationCard();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void markFieldInvalid(Control field, boolean invalid) {
        field.setStyle(invalid
                ? "-fx-border-color: #C0392B; -fx-border-width: 2; -fx-border-radius: 4;"
                : "");
    }

    private void styleErrorLabel(Label label) {
        label.setFont(Font.font("Arial", 11));
        label.setTextFill(Color.web("#C0392B"));
        label.setVisible(false);
        label.setWrapText(true);
    }
}