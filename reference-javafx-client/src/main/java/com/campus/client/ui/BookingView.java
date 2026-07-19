package com.campus.client.ui;

import com.campus.client.controller.BookingController;
import com.campus.client.model.Resource;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

// this is the screen where the student books a resource
// it only handles UI stuff (building the layout, showing/hiding fields, reading input)
// any actual booking logic goes through the controller, this class doesnt talk to the server itself
public class BookingView extends BaseView {

    private BookingController controller;

    // these fields are always shown on the screen
    private ComboBox<String> resourceTypeCombo;
    private TextField dateField;
    private Button checkAvailabilityButton;

    // these are hidden until user clicks Check Availability
    private Label availableRoomsLabel;
    private VBox availableRoomsBox;
    private Label roomLabel;
    private ComboBox<String> roomCombo;
    private Label startLabel;
    private TextField startTimeField;
    private Label endLabel;
    private TextField endTimeField;
    private Button bookButton;

    // shown after a booking succeeds, lets user go back and book something else
    private Button backButton;

    // the big confirmation panel that replaces the form after a successful booking
    private VBox confirmationCard;
    private Label confirmationTitle;
    private Label confirmationRef;
    private VBox mainCard;

    private Label statusLabel;
    private TextField studentIdField;

    public BookingView() {
        buildUI();
    }

    // builds the whole screen layout, this is a long method but its just adding one component after another
    private void buildUI() {
        // put everything inside a scroll pane in case the form gets too tall for the window
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: #F0F4F8; -fx-background: #F0F4F8;");

        VBox mainContainer = new VBox(25);
        mainContainer.setPadding(new Insets(30));
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.setStyle("-fx-background-color: #F0F4F8;");

        // title at the top
        VBox titleSection = new VBox(5);
        titleSection.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Book a Resource");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: #1A2332;");

        Label subtitleLabel = new Label("Select a resource type, date and time to make a booking");
        subtitleLabel.setFont(Font.font("System", 14));
        subtitleLabel.setStyle("-fx-text-fill: #7A8A9E;");

        titleSection.getChildren().addAll(titleLabel, subtitleLabel);

        // the white card that holds the actual form
        mainCard = new VBox(20);
        mainCard.setPadding(new Insets(30));
        mainCard.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 20, 0, 0, 4);"
        );
        mainCard.setMaxWidth(750);
        mainCard.setAlignment(Pos.TOP_CENTER);

        GridPane form = new GridPane();
        form.setHgap(20);
        form.setVgap(14);
        form.setAlignment(Pos.CENTER);

        int row = 0;

        // resource type dropdown, disabled until controller loads the list from server
        Label typeLabel = createLabel("Resource Type");
        resourceTypeCombo = new ComboBox<>();
        resourceTypeCombo.setPromptText("Select type");
        resourceTypeCombo.setPrefWidth(220);
        resourceTypeCombo.setDisable(true);
        resourceTypeCombo.setStyle(
                "-fx-background-color: #F5F7FA; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 8; " +
                        "-fx-padding: 4 8;"
        );

        form.add(typeLabel, 0, row);
        form.add(resourceTypeCombo, 1, row);
        row++;

        // date field and the check availability button this is side by side
        Label dateLabel = createLabel("Date");
        dateField = new TextField();
        dateField.setPromptText("yyyy-MM-dd");
        dateField.setPrefWidth(220);
        dateField.setStyle(
                "-fx-background-color: #F5F7FA; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 8; " +
                        "-fx-padding: 4 8;"
        );

        checkAvailabilityButton = new Button("Check Availability");
        checkAvailabilityButton.setStyle(
                "-fx-background-color: #1A2332; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 13px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 10 24; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;"
        );
        checkAvailabilityButton.setOnAction(e -> handleCheckAvailability());

        HBox dateRow = new HBox(12, dateField, checkAvailabilityButton);
        dateRow.setAlignment(Pos.CENTER_LEFT);

        form.add(dateLabel, 0, row);
        form.add(dateRow, 1, row);
        row++;

        // separator to visually split the form sections
        Separator separator = new Separator();
        separator.setPadding(new Insets(8, 0, 8, 0));
        form.add(separator, 0, row, 2, 1);
        row++;

        // everything below here is hidden at the start, only shows up after checking availability
        availableRoomsLabel = new Label("Available Rooms");
        availableRoomsLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        availableRoomsLabel.setStyle("-fx-text-fill: #1A2332;");
        availableRoomsLabel.setVisible(false);
        availableRoomsLabel.setManaged(false);
        form.add(availableRoomsLabel, 0, row, 2, 1);
        row++;

        availableRoomsBox = new VBox(8);
        availableRoomsBox.setPadding(new Insets(14));
        availableRoomsBox.setStyle(
                "-fx-background-color: #F8FAFC; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 10; " +
                        "-fx-background-radius: 10;"
        );
        availableRoomsBox.setVisible(false);
        availableRoomsBox.setManaged(false);
        form.add(availableRoomsBox, 0, row, 2, 1);
        row++;

        // room dropdown, the label needs to be hidden too not just the combo box

        roomLabel = createLabel("Select Room");
        roomLabel.setVisible(false);
        roomLabel.setManaged(false);

        roomCombo = new ComboBox<>();
        roomCombo.setPromptText("Choose a room");
        roomCombo.setPrefWidth(220);
        roomCombo.setDisable(true);
        roomCombo.setVisible(false);
        roomCombo.setManaged(false);
        roomCombo.setStyle(
                "-fx-background-color: #F5F7FA; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 8; " +
                        "-fx-padding: 4 8;"
        );

        form.add(roomLabel, 0, row);
        form.add(roomCombo, 1, row);
        row++;

        // start time
        startLabel = createLabel("Start Time");
        startLabel.setVisible(false);
        startLabel.setManaged(false);

        startTimeField = new TextField();
        startTimeField.setPromptText("HH:mm");
        startTimeField.setPrefWidth(120);
        startTimeField.setDisable(true);
        startTimeField.setVisible(false);
        startTimeField.setManaged(false);
        startTimeField.setStyle(
                "-fx-background-color: #F5F7FA; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 8; " +
                        "-fx-padding: 4 8;"
        );

        form.add(startLabel, 0, row);
        form.add(startTimeField, 1, row);
        row++;

        // end time
        endLabel = createLabel("End Time");
        endLabel.setVisible(false);
        endLabel.setManaged(false);

        endTimeField = new TextField();
        endTimeField.setPromptText("HH:mm");
        endTimeField.setPrefWidth(120);
        endTimeField.setDisable(true);
        endTimeField.setVisible(false);
        endTimeField.setManaged(false);
        endTimeField.setStyle(
                "-fx-background-color: #F5F7FA; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 8; " +
                        "-fx-padding: 4 8;"
        );

        form.add(endLabel, 0, row);
        form.add(endTimeField, 1, row);
        row++;

        // confirm booking button, stays disabled until a room + valid times are picked
        bookButton = new Button("Confirm Booking");
        bookButton.setStyle(
                "-fx-background-color: #2ECC71; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12 32; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;"
        );
        bookButton.setDisable(true);
        bookButton.setVisible(false);
        bookButton.setManaged(false);
        bookButton.setOnAction(e -> handleBook());

        HBox buttonRow = new HBox(bookButton);
        buttonRow.setAlignment(Pos.CENTER);
        buttonRow.setPadding(new Insets(8, 0, 0, 0));
        form.add(buttonRow, 0, row, 2, 1);
        row++;

        // student id isnt shown on screen, just kept here so we can send it along with the booking
        studentIdField = new TextField();
        studentIdField.setEditable(false);
        studentIdField.setVisible(false);

        // status message at the bottom, used for errors, success msgs, loading text etc
        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 13px; -fx-padding: 4 0 0 0;");
        statusLabel.setVisible(false);
        form.add(statusLabel, 0, row, 2, 1);

        mainCard.getChildren().add(form);

        // confirmation card - only visible after a booking succeeds
        confirmationCard = new VBox(18);
        confirmationCard.setAlignment(Pos.CENTER);
        confirmationCard.setPadding(new Insets(50));
        confirmationCard.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 20, 0, 0, 4);"
        );
        confirmationCard.setMaxWidth(750);
        confirmationCard.setVisible(false);
        confirmationCard.setManaged(false);

        confirmationTitle = new Label("Booking Confirmed");
        confirmationTitle.setFont(Font.font("System", FontWeight.BOLD, 26));
        confirmationTitle.setStyle("-fx-text-fill: #27AE60;");

        confirmationRef = new Label();
        confirmationRef.setFont(Font.font("System", FontWeight.BOLD, 16));
        confirmationRef.setStyle("-fx-text-fill: #1A2332;");

        backButton = new Button("Back");
        backButton.setStyle(
                "-fx-background-color: #1A2332; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 13px; " +
                        "-fx-padding: 10 32; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;"
        );
        backButton.setOnAction(e -> handleBack());

        confirmationCard.getChildren().addAll(confirmationTitle, confirmationRef, backButton);

        // stack both cards on top of each other, only one is visible at a time
        StackPane cardStack = new StackPane(mainCard, confirmationCard);
        cardStack.setAlignment(Pos.TOP_CENTER);

        mainContainer.getChildren().addAll(titleSection, cardStack);
        scrollPane.setContent(mainContainer);
        getChildren().add(scrollPane);
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 13));
        label.setStyle("-fx-text-fill: #2D3748;");
        label.setAlignment(Pos.CENTER_LEFT);
        label.setMinWidth(100);
        return label;
    }

    public void setController(BookingController controller) {
        this.controller = controller;
    }

    public void setStudentId(String studentId) {
        studentIdField.setText(studentId);
    }

    // called by the controller once it fetches the resource types from the server
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

    // shows the room combo, times and book button, called after check availability is clicked
    private void showBelowCheckFields() {
        availableRoomsLabel.setVisible(true);
        availableRoomsLabel.setManaged(true);
        availableRoomsBox.setVisible(true);
        availableRoomsBox.setManaged(true);

        roomLabel.setVisible(true);
        roomLabel.setManaged(true);
        roomCombo.setVisible(true);
        roomCombo.setManaged(true);

        startLabel.setVisible(true);
        startLabel.setManaged(true);
        startTimeField.setVisible(true);
        startTimeField.setManaged(true);

        endLabel.setVisible(true);
        endLabel.setManaged(true);
        endTimeField.setVisible(true);
        endTimeField.setManaged(true);

        bookButton.setVisible(true);
        bookButton.setManaged(true);
    }

    // hides them again, used when clearing the form after a booking is made
    private void hideBelowCheckFields() {
        availableRoomsLabel.setVisible(false);
        availableRoomsLabel.setManaged(false);
        availableRoomsBox.setVisible(false);
        availableRoomsBox.setManaged(false);

        roomLabel.setVisible(false);
        roomLabel.setManaged(false);
        roomCombo.setVisible(false);
        roomCombo.setManaged(false);

        startLabel.setVisible(false);
        startLabel.setManaged(false);
        startTimeField.setVisible(false);
        startTimeField.setManaged(false);

        endLabel.setVisible(false);
        endLabel.setManaged(false);
        endTimeField.setVisible(false);
        endTimeField.setManaged(false);

        bookButton.setVisible(false);
        bookButton.setManaged(false);
    }

    // called by the controller with the result of the availability check
    // it will shows a green list for fully free rooms and an orange list for rooms that have some bookings already
    public void showAvailableRooms(List<Resource> availableRooms,
                                   List<Resource> partiallyBookedRooms,
                                   String date) {

        showBelowCheckFields();

        availableRoomsBox.getChildren().clear();

        int totalAvailable = availableRooms.size() + partiallyBookedRooms.size();

        // no rooms for this type/date so it show a message and stop here
        if (totalAvailable == 0) {
            Label msg = new Label("No rooms available on " + date);
            msg.setStyle("-fx-text-fill: #C0392B; -fx-font-weight: bold; -fx-font-size: 14px;");
            availableRoomsBox.getChildren().add(msg);

            roomCombo.setDisable(true);
            startTimeField.setDisable(true);
            endTimeField.setDisable(true);
            bookButton.setDisable(true);

            statusLabel.setText("No rooms available on " + date);
            statusLabel.setStyle("-fx-text-fill: #C0392B;");
            statusLabel.setVisible(true);
            return;
        }

        // green = fully free rooms
        for (Resource r : availableRooms) {
            VBox item = new VBox(2);
            item.setPadding(new Insets(6, 12, 6, 12));
            item.setStyle(
                    "-fx-background-color: #E8F8E8; " +
                            "-fx-border-radius: 8; " +
                            "-fx-background-radius: 8;"
            );

            Label roomItemLabel = new Label("🟢 " + r.getResourceId() + " : " + r.getResourceName() + " (" + r.getBuilding() + ")");
            roomItemLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
            roomItemLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

            Label statusText = new Label("No bookings today");
            statusText.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 11px;");
            item.getChildren().addAll(roomItemLabel, statusText);
            availableRoomsBox.getChildren().add(item);
        }

        // orange = already has some bookings but the exact slot the user wants might still be free
        for (Resource r : partiallyBookedRooms) {
            VBox item = new VBox(2);
            item.setPadding(new Insets(6, 12, 6, 12));
            item.setStyle(
                    "-fx-background-color: #FEF9E7; " +
                            "-fx-border-radius: 8; " +
                            "-fx-background-radius: 8;"
            );

            Label roomItemLabel = new Label("🟠 " + r.getResourceId() + " : " + r.getResourceName() + " (" + r.getBuilding() + ")");
            roomItemLabel.setStyle("-fx-text-fill: #E67E22; -fx-font-weight: bold;");
            roomItemLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

            Label statusText = new Label("Some bookings exist, time may be free");
            statusText.setStyle("-fx-text-fill: #E67E22; -fx-font-size: 11px;");
            item.getChildren().addAll(roomItemLabel, statusText);
            availableRoomsBox.getChildren().add(item);
        }

        // fill the room dropdown with both green and orange rooms, user can pick either
        roomCombo.getItems().clear();
        for (Resource r : availableRooms) {
            roomCombo.getItems().add(r.getResourceId() + " - " + r.getResourceName());
        }
        for (Resource r : partiallyBookedRooms) {
            roomCombo.getItems().add(r.getResourceId() + " - " + r.getResourceName());
        }
        roomCombo.setDisable(false);
        roomCombo.setPromptText("Select a room");

        startTimeField.setDisable(false);
        endTimeField.setDisable(false);
        startTimeField.clear();
        endTimeField.clear();

        bookButton.setDisable(true);

        statusLabel.setText(totalAvailable + " rooms available on " + date);
        statusLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
        statusLabel.setVisible(true);

        // book button only becomes clickable once a room is picked and both times look filled in
        roomCombo.setOnAction(e -> updateBookButtonState());
        startTimeField.setOnKeyReleased(e -> updateBookButtonState());
        endTimeField.setOnKeyReleased(e -> updateBookButtonState());
    }


    // just a UI convenience check not real validation . only controls whether the book button is clickable. the real validation still happens in the controller
    private void updateBookButtonState() {
        boolean roomSelected = roomCombo.getValue() != null;
        String start = startTimeField.getText().trim();
        String end = endTimeField.getText().trim();

        boolean timeFilled = !start.isEmpty() && !end.isEmpty();

        // check that times actually look like HH:mm before we let the user book
        if (timeFilled) {
            boolean validStart = start.matches("\\d{2}:\\d{2}");
            boolean validEnd = end.matches("\\d{2}:\\d{2}");
            if (!validStart || !validEnd) {
                bookButton.setDisable(true);
                return;
            }
        }

        bookButton.setDisable(!(roomSelected && timeFilled));
    }

    public void showNoRoomsAvailable(String date) {
        showBelowCheckFields();

        availableRoomsBox.getChildren().clear();
        Label msg = new Label("No rooms available on " + date);
        msg.setStyle("-fx-text-fill: #C0392B; -fx-font-weight: bold; -fx-font-size: 14px;");
        availableRoomsBox.getChildren().add(msg);

        roomCombo.setDisable(true);
        roomCombo.setPromptText("No rooms available");
        startTimeField.setDisable(true);
        endTimeField.setDisable(true);
        bookButton.setDisable(true);

        statusLabel.setText("No rooms available on " + date);
        statusLabel.setStyle("-fx-text-fill: #C0392B;");
        statusLabel.setVisible(true);
    }

    @Override
    public void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #C0392B; -fx-font-weight: bold;");
        statusLabel.setVisible(true);
    }

    @Override
    public void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
        statusLabel.setVisible(true);
    }

    public void showStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #3498DB; -fx-font-weight: bold;");
        statusLabel.setVisible(true);
    }

    // swaps the form out for the confirmation card once a booking goes through
    public void showBookingConfirmation(String reference) {
        // instead of just disabling the form, swap it out completely for the confirmation card
        // so the user cant poke at the old fields while looking at the confirmation
        confirmationRef.setText("Reference: " + reference);

        mainCard.setVisible(false);
        mainCard.setManaged(false);

        confirmationCard.setVisible(true);
        confirmationCard.setManaged(true);
    }

    // runs when the user clicks Back after a successful booking, resets everything for a new booking
    private void handleBack() {
        confirmationCard.setVisible(false);
        confirmationCard.setManaged(false);

        mainCard.setVisible(true);
        mainCard.setManaged(true);

        clearFields();
        setFormEnabled(true);
    }

    // locks the whole form while a request is running so the user cant spam click stuff
    public void setFormEnabled(boolean enabled) {
        resourceTypeCombo.setDisable(!enabled);
        dateField.setDisable(!enabled);
        checkAvailabilityButton.setDisable(!enabled);
        roomCombo.setDisable(!enabled || roomCombo.getItems().isEmpty());
        startTimeField.setDisable(!enabled);
        endTimeField.setDisable(!enabled);
        bookButton.setDisable(!enabled);
    }

    public void clearFields() {
        dateField.clear();
        roomCombo.setValue(null);
        roomCombo.getItems().clear();
        startTimeField.clear();
        endTimeField.clear();

        hideBelowCheckFields();

        statusLabel.setText("");
        statusLabel.setVisible(false);
    }

    public String getSelectedResourceType() {
        return resourceTypeCombo.getValue();
    }

    public String getDate() {
        return dateField.getText().trim();
    }

    public String getSelectedRoomId() {
        // the combo box shows "id - name" so we just grab whatever is before the dash
        String selected = roomCombo.getValue();
        if (selected == null || selected.isEmpty()) return null;
        String[] parts = selected.split(" - ");
        if (parts.length > 0) return parts[0].trim();
        return null;
    }

    public String getStartTime() {
        return startTimeField.getText().trim();
    }

    public String getEndTime() {
        return endTimeField.getText().trim();
    }

    public String getStudentId() {
        return studentIdField.getText().trim();
    }

    // called when the check availability button gets pressed
    // does simple field checks here for instant feedback, then hands off to the controller
    // for the actual server call
    private void handleCheckAvailability() {
        if (controller == null) {
            showError("Controller not connected.");
            return;
        }

        String resourceType = getSelectedResourceType();
        String date = getDate();

        if (resourceType == null || resourceType.isEmpty()) {
            showError("Please select a resource type.");
            return;
        }

        if (date.isEmpty()) {
            showError("Please enter a date.");
            return;
        }

        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            showError("Invalid date format. Use yyyy-MM-dd.");
            return;
        }

        setFormEnabled(false);
        showStatus("Checking availability...");

        controller.handleCheckAvailability(date, resourceType);
    }

    // called when confirm booking gets pressed , checks  validation and the actual booking call happens in the controller
    private void handleBook() {
        if (controller == null) {
            showError("Controller not connected.");
            return;
        }

        String studentId = getStudentId();
        String roomId = getSelectedRoomId();
        String date = getDate();
        String startTime = getStartTime();
        String endTime = getEndTime();

        if (studentId == null || studentId.isEmpty()) {
            showError("Please log in first.");
            return;
        }

        if (roomId == null || roomId.isEmpty()) {
            showError("Please select a room.");
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

        if (!startTime.matches("\\d{2}:\\d{2}")) {
            showError("Invalid start time format. Use HH:mm.");
            return;
        }

        if (!endTime.matches("\\d{2}:\\d{2}")) {
            showError("Invalid end time format. Use HH:mm.");
            return;
        }

        setFormEnabled(false);
        showStatus("Booking...");

        controller.handleBook(studentId, roomId, date, startTime, endTime);
    }

    @Override
    public void onShow() {}

    @Override
    public void onHide() {}
}