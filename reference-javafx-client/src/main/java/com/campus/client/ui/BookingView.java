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
import javafx.scene.text.TextAlignment;
import javafx.stage.Popup;

import java.util.List;

/**
 * The Resource Booking screen - lets a student pick a resource type, pick an
 * actual resource, and submit a booking (FR2, FR3).
 *
 * This extends BaseView, which extends VBox, so this class IS the screen -
 * MainView can just add it straight into its content area, no wrapper needed.
 *
 * Note about Resource Type: Resource.java doesn't actually have a "type" field
 * (only id, name, building, capacity), so BookingController guesses the type
 * from the resource's name just for this dropdown. It doesn't affect what
 * actually gets booked.
 */
public class BookingView extends BaseView {

    private BookingController controller;

    // top nav bar pieces - built directly in this class, no separate NavBar class
    private final Label mcpStatusDot = new Label("●");
    private final Label mcpStatusLabel = new Label("MCP Connected");
    private final Button homeButton = new Button("Home");
    private final Button bookingNavButton = new Button("Resource\nBooking");
    private final Button historyNavButton = new Button("Booking\nHistory");
    private final Button policyNavButton = new Button("Policy\nAssistant");
    private final Button logoutButton = new Button("Logout");

    // form fields
    private final ComboBox<String> resourceTypeCombo = new ComboBox<>();
    private final TextField resourceIdField = new TextField(); // read-only, click opens the picker popup
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

    // small floating card that shows the booking result (success/fail/duplicate)
    private final Label confirmationLabel = new Label();
    private final Button confirmationButton = new Button("OK");
    private final VBox confirmationCard = new VBox(10, confirmationLabel, confirmationButton);

    private final ProgressIndicator loadingIndicator = new ProgressIndicator();

    public BookingView() {
        buildLayout();
        wireEvents();
    }

    /** MainView calls this after creating the view, so it can hand us a controller. */
    public void setController(BookingController controller) {
        this.controller = controller;
    }

    /** Hook up what happens when the Booking History nav button is clicked. */
    public void setOnHistoryButtonClicked(Runnable action) {
        historyNavButton.setOnAction(e -> action.run());
    }

    public void setOnHomeButtonClicked(Runnable action) {
        homeButton.setOnAction(e -> action.run());
    }

    public void setOnPolicyButtonClicked(Runnable action) {
        policyNavButton.setOnAction(e -> action.run());
    }

    public void setOnLogoutButtonClicked(Runnable action) {
        logoutButton.setOnAction(e -> action.run());
    }

    /** Updates the green/red MCP status dot in the nav bar. */
    public void setMcpConnected(boolean connected) {
        mcpStatusDot.setTextFill(connected ? Color.web("#2ECC71") : Color.web("#E74C3C"));
        mcpStatusLabel.setText(connected ? "MCP Connected" : "MCP Disconnected");
    }

    // called by MainView every time this screen becomes visible again
    @Override
    public void onShow() {
        clearAllErrors();
        hideConfirmationCard();
    }

    // nothing to clean up when leaving this screen for now
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

        // centers the form both ways in whatever space is left below our own bars
        StackPane formWrapper = new StackPane(formCard);
        formWrapper.setPadding(new Insets(30));

        ScrollPane scrollPane = new ScrollPane(formWrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: #f5f5f5; -fx-background-color: #f5f5f5;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // confirmation card floats on top of the form when we need to show it
        StackPane contentStack = new StackPane(scrollPane, confirmationCard);
        StackPane.setAlignment(confirmationCard, Pos.CENTER);
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        getChildren().addAll(buildNavBar(), buildHeroBanner("Book a Resource"), contentStack);
    }

    /**
     * Top nav bar: Home, MCP status, the screen buttons, Logout. Built directly
     * here (and duplicated in ViewBookingView) since we're not using a shared
     * NavBar class. Styled gray on purpose so it doesn't clash with the navy
     * hero banner underneath it.
     */
    private HBox buildNavBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 16, 8, 16));
        bar.setStyle("-fx-background-color: #7F8C8D;");

        homeButton.setStyle(navButtonStyle(false));

        mcpStatusDot.setFont(Font.font(14));
        mcpStatusLabel.setTextFill(Color.WHITE);
        mcpStatusLabel.setFont(Font.font("Arial", 11));
        setMcpConnected(true);
        HBox statusBox = new HBox(4, mcpStatusDot, mcpStatusLabel);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        bookingNavButton.setStyle(navButtonStyle(true)); // this screen is the active one
        historyNavButton.setStyle(navButtonStyle(false));
        policyNavButton.setStyle(navButtonStyle(false));
        for (Button b : new Button[]{bookingNavButton, historyNavButton, policyNavButton}) {
            b.setTextAlignment(TextAlignment.CENTER);
            b.setFont(Font.font("Arial", 11));
            b.setPrefWidth(105);
        }

        logoutButton.setStyle(navButtonStyle(false));

        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        bar.getChildren().addAll(homeButton, statusBox,
                bookingNavButton, historyNavButton, policyNavButton,
                rightSpacer, logoutButton);
        return bar;
    }

    private String navButtonStyle(boolean active) {
        return active
                ? "-fx-background-color: #2C3E50; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-weight: bold;"
                : "-fx-background-color: #D9D9D9; -fx-text-fill: #2C2C2C; -fx-background-radius: 4;";
    }

    /** Navy title banner under the nav bar, matching our original wireframe. */
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

        // 2-column layout: Type | ID, Student ID | Date, Start | End - matches our wireframe
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

    /** one label + field + error message, stacked - used for every field in the grid above */
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

    /** small popup table (ID / Name / Building / Capacity) that opens when clicking the resource field */
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

        // clicking a row picks that resource and closes the popup
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

    // this is what checkAvailability() looks like from the class diagram - only called
    // from the resource field click below, so it's kept private
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

    /** small floating card shown after submitting - success, duplicate, or error */
    private void buildConfirmationCard() {
        confirmationLabel.setWrapText(true);
        confirmationLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        confirmationLabel.setTextAlignment(TextAlignment.CENTER);

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

    // this is bookResource() from the class diagram - only triggered by the submit button
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
    // stuff the controller calls to update what's on screen
    // ---------------------------------------------------------------

    public void setStudentId(String studentId) {
        studentIdField.setText(studentId);
    }

    /** called once when the screen is built, to fill in the resource type dropdown */
    public void setResourceTypes(List<String> types) {
        resourceTypeCombo.setItems(FXCollections.observableArrayList(types));
    }

    /** called when the resource type changes - just refreshes the popup list quietly */
    public void updateResourceIdOptions(List<Resource> resources) {
        resourceIdTable.setItems(FXCollections.observableArrayList(resources));
        resourceIdField.clear();
        selectedResourceId = null;
    }

    /** called once checkAvailability() gets a response back - refreshes the popup and opens it */
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

    /** locks the form and shows a spinner while a background call is running */
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
    // per-field error messages (red text + red border)
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

    /** wipes the form back to empty after a booking goes through */
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

    // ---------------------------------------------------------------
    // overriding BaseView's default Alert-style error/success popups with our
    // own floating card instead, since it looks nicer and matches our wireframe
    // ---------------------------------------------------------------

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
    // little helpers
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

