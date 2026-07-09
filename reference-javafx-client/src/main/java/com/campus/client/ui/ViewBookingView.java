package com.campus.client.ui;

import com.campus.client.controller.ViewBookingController;
import com.campus.client.model.Booking;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * The Booking History screen - shows the student's upcoming and past bookings,
 * and lets them cancel an upcoming one (FR4, FR5).
 *
 * Same as BookingView, this extends BaseView (which extends VBox), so MainView
 * can just drop this straight into its content area.
 *
 * Note: our wireframe originally had a "Submitted On" column, but Booking.java
 * doesn't store when a booking was made, so it's left out here for now.
 */
public class ViewBookingView extends BaseView {

    private ViewBookingController controller;

    // top nav bar pieces - built directly in this class, no separate NavBar class
    private final Label mcpStatusDot = new Label("●");
    private final Label mcpStatusLabel = new Label("MCP Connected");
    private final Button homeButton = new Button("Home");
    private final Button bookingNavButton = new Button("Resource\nBooking");
    private final Button historyNavButton = new Button("Booking\nHistory");
    private final Button policyNavButton = new Button("Policy\nAssistant");
    private final Button logoutButton = new Button("Logout");

    // pill-style switch between Upcoming and Past, instead of a plain TabPane
    private final ToggleGroup switchGroup = new ToggleGroup();
    private final ToggleButton upcomingToggle = new ToggleButton("Upcoming Bookings");
    private final ToggleButton pastToggle = new ToggleButton("Past Bookings");

    private final TableView<Booking> upcomingTable = new TableView<>();
    private final ObservableList<Booking> upcomingData = FXCollections.observableArrayList();

    private final TableView<Booking> pastTable = new TableView<>();
    private final ObservableList<Booking> pastData = FXCollections.observableArrayList();

    private final Label statusLabel = new Label();

    public ViewBookingView() {
        buildLayout();
        wireEvents();
    }

    /** MainView calls this after creating the view, so it can hand us a controller. */
    public void setController(ViewBookingController controller) {
        this.controller = controller;
    }

    /** Hook up what happens when the Resource Booking nav button is clicked. */
    public void setOnBookingButtonClicked(Runnable action) {
        bookingNavButton.setOnAction(e -> action.run());
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

    // refresh the list every time this screen becomes visible, in case a
    // booking was cancelled or added somewhere else since we last looked
    @Override
    public void onShow() {
        viewBookings();
    }

    @Override
    public void onHide() {
    }

    // ---------------------------------------------------------------
    // building the screen
    // ---------------------------------------------------------------

    private void buildLayout() {
        setStyle("-fx-background-color: #f5f5f5;");

        HBox switchBar = buildSwitch();

        buildUpcomingTable();
        buildPastTable();
        pastTable.setVisible(false);
        pastTable.setManaged(false);

        // both tables sit in the same spot, we just show/hide whichever tab is picked
        StackPane tableStack = new StackPane(upcomingTable, pastTable);
        VBox.setVgrow(tableStack, Priority.ALWAYS);

        styleStatusLabel(statusLabel);

        VBox mainContent = new VBox(16, switchBar, tableStack, statusLabel);
        mainContent.setPadding(new Insets(20));
        mainContent.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        getChildren().addAll(buildNavBar(), buildHeroBanner("Booking History"), mainContent);
    }

    /**
     * Top nav bar: Home, MCP status, the screen buttons, Logout. Built directly
     * here (same as in BookingView) since we're not using a shared NavBar class.
     * Styled gray on purpose so it doesn't clash with the navy hero banner below it.
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

        bookingNavButton.setStyle(navButtonStyle(false));
        historyNavButton.setStyle(navButtonStyle(true)); // this screen is the active one
        policyNavButton.setStyle(navButtonStyle(false));
        for (Button b : new Button[]{bookingNavButton, historyNavButton, policyNavButton}) {
            b.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
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

    /** Navy title banner under the nav bar, matching BookingView's. */
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

    /** two touching rounded buttons that act like one switch, not a plain TabPane */
    private HBox buildSwitch() {
        upcomingToggle.setToggleGroup(switchGroup);
        pastToggle.setToggleGroup(switchGroup);
        upcomingToggle.setSelected(true);

        for (ToggleButton btn : List.of(upcomingToggle, pastToggle)) {
            btn.setPrefWidth(180);
            btn.setPrefHeight(34);
            btn.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        }
        applySwitchStyle(upcomingToggle, "8 0 0 8");
        applySwitchStyle(pastToggle, "0 8 8 0");

        HBox switchBar = new HBox(upcomingToggle, pastToggle);
        switchBar.setAlignment(Pos.CENTER);
        return switchBar;
    }

    private void applySwitchStyle(ToggleButton button, String cornerRadius) {
        button.selectedProperty().addListener((obs, was, isNow) ->
                button.setStyle(pillStyle(isNow, cornerRadius)));
        button.setStyle(pillStyle(button.isSelected(), cornerRadius));
    }

    private String pillStyle(boolean selected, String cornerRadius) {
        return selected
                ? "-fx-background-color: #1F3864; -fx-text-fill: white; -fx-background-radius: " + cornerRadius + ";"
                : "-fx-background-color: #E0E0E0; -fx-text-fill: #333333; -fx-background-radius: " + cornerRadius + ";";
    }

    @SuppressWarnings("unchecked")
    private void buildUpcomingTable() {
        // little minus button to cancel a booking, left of each row
        TableColumn<Booking, Void> cancelCol = new TableColumn<>("");
        cancelCol.setPrefWidth(50);
        cancelCol.setResizable(false);
        cancelCol.setCellFactory(col -> new TableCell<>() {
            private final Button cancelBtn = new Button("−");

            {
                cancelBtn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                cancelBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; "
                        + "-fx-background-radius: 50%; -fx-min-width: 28; -fx-min-height: 28; "
                        + "-fx-max-width: 28; -fx-max-height: 28;");
                cancelBtn.setOnAction(e -> {
                    Booking booking = getTableView().getItems().get(getIndex());
                    cancelBooking(booking);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : cancelBtn);
            }
        });

        TableColumn<Booking, String> refCol        = makeColumn("Reference No.",  "bookingRef",  140);
        TableColumn<Booking, String> resourceIdCol = makeColumn("Resource ID",    "resourceId",  120);
        TableColumn<Booking, String> dateCol       = makeColumn("Date",           "date",        120);
        TableColumn<Booking, String> startCol      = makeColumn("Start Time",     "startTime",   100);
        TableColumn<Booking, String> endCol        = makeColumn("End Time",       "endTime",     100);

        upcomingTable.getColumns().addAll(cancelCol, refCol, resourceIdCol, dateCol, startCol, endCol);
        upcomingTable.setItems(upcomingData);
        upcomingTable.setPlaceholder(new Label("No upcoming bookings."));
        upcomingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    @SuppressWarnings("unchecked")
    private void buildPastTable() {
        TableColumn<Booking, String> refCol        = makeColumn("Reference No.",  "bookingRef",  140);
        TableColumn<Booking, String> resourceIdCol = makeColumn("Resource ID",    "resourceId",  120);
        TableColumn<Booking, String> dateCol       = makeColumn("Date",           "date",        120);
        TableColumn<Booking, String> startCol      = makeColumn("Start Time",     "startTime",   100);
        TableColumn<Booking, String> endCol        = makeColumn("End Time",       "endTime",     100);

        pastTable.getColumns().addAll(refCol, resourceIdCol, dateCol, startCol, endCol);
        pastTable.setItems(pastData);
        pastTable.setPlaceholder(new Label("No past bookings."));
        pastTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void wireEvents() {
        switchGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                upcomingToggle.setSelected(true); // don't let both buttons end up unselected
                return;
            }
            boolean showUpcoming = newToggle == upcomingToggle;
            upcomingTable.setVisible(showUpcoming);
            upcomingTable.setManaged(showUpcoming);
            pastTable.setVisible(!showUpcoming);
            pastTable.setManaged(!showUpcoming);

            viewBookings();
        });
    }

    // this is viewBookings() from the class diagram
    private void viewBookings() {
        if (controller != null) {
            controller.handleViewBookings();
        }
    }

    // this is cancelBooking() from the class diagram - shows the confirm popup first
    private void cancelBooking(Booking booking) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Booking");
        confirm.setHeaderText(null);
        confirm.setContentText("Confirm to cancel the booking " + booking.getBookingRef() + "?");

        ButtonType yesBtn = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noBtn  = new ButtonType("No",  ButtonBar.ButtonData.NO);
        confirm.getButtonTypes().setAll(yesBtn, noBtn);

        confirm.showAndWait().ifPresent(result -> {
            if (result == yesBtn && controller != null) {
                controller.handleCancelBooking(booking);
            }
        });
    }

    // ---------------------------------------------------------------
    // stuff the controller calls to update what's on screen
    // ---------------------------------------------------------------

    public void displayBookings(List<Booking> upcoming, List<Booking> past) {
        upcomingData.setAll(upcoming);
        pastData.setAll(past);
        statusLabel.setVisible(false);
    }

    public void onBookingCancelled(Booking booking) {
        upcomingData.remove(booking);
        showSuccess("Booking is cancelled!");
    }

    // ---------------------------------------------------------------
    // overriding BaseView's default Alert popups with a status banner instead -
    // feels less disruptive than a popup every time you view your bookings
    // ---------------------------------------------------------------

    @Override
    public void showError(String message) {
        statusLabel.setText("❌ " + message);
        statusLabel.setStyle("-fx-background-color: #FDECEA; -fx-text-fill: #C0392B; "
                + "-fx-padding: 8 12 8 12; -fx-background-radius: 6; "
                + "-fx-border-color: #E74C3C; -fx-border-width: 1; -fx-border-radius: 6;");
        statusLabel.setVisible(true);
    }

    @Override
    public void showSuccess(String message) {
        statusLabel.setText("✅ " + message);
        statusLabel.setStyle("-fx-background-color: #E8F8E8; -fx-text-fill: #27AE60; "
                + "-fx-padding: 8 12 8 12; -fx-background-radius: 6; "
                + "-fx-border-color: #2ECC71; -fx-border-width: 1; -fx-border-radius: 6;");
        statusLabel.setVisible(true);
    }

    // ---------------------------------------------------------------
    // little helpers
    // ---------------------------------------------------------------

    private TableColumn<Booking, String> makeColumn(String title, String property, double prefWidth) {
        TableColumn<Booking, String> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setPrefWidth(prefWidth);
        return col;
    }

    private void styleStatusLabel(Label label) {
        label.setWrapText(true);
        label.setVisible(false);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
    }
}
