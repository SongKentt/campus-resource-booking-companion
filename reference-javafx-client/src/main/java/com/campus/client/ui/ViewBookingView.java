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
 */
public class ViewBookingView extends BaseView {

    private ViewBookingController controller;

    // pill-style switch between Upcoming and Past
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

    public void setController(ViewBookingController controller) {
        this.controller = controller;
    }

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

        StackPane tableStack = new StackPane(upcomingTable, pastTable);
        VBox.setVgrow(tableStack, Priority.ALWAYS);

        styleStatusLabel(statusLabel);

        VBox mainContent = new VBox(16, switchBar, tableStack, statusLabel);
        mainContent.setPadding(new Insets(20));
        mainContent.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        getChildren().addAll(buildHeroBanner("Booking History"), mainContent);
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
                    if (booking != null) {
                        cancelBooking(booking);
                    }
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
                upcomingToggle.setSelected(true);
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

    private void viewBookings() {
        if (controller != null) {
            controller.loadBookings();
        }
    }

    // ===== FIX: Pass booking reference (String) to controller =====
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
                // ===== FIX: Pass the booking reference (String) =====
                controller.handleCancelBooking(booking.getBookingRef());
            }
        });
    }

    // ---------------------------------------------------------------
    // Controller calls these to update the UI
    // ---------------------------------------------------------------

    public void displayBookings(List<Booking> upcoming, List<Booking> past) {
        upcomingData.setAll(upcoming);
        pastData.setAll(past);
        statusLabel.setVisible(false);
    }



    @Override
    public void showError(String message) {
        statusLabel.setText("Error: " + message);
        statusLabel.setStyle("-fx-background-color: #FDECEA; -fx-text-fill: #C0392B; "
                + "-fx-padding: 8 12 8 12; -fx-background-radius: 6; "
                + "-fx-border-color: #E74C3C; -fx-border-width: 1; -fx-border-radius: 6;");
        statusLabel.setVisible(true);
    }

    @Override
    public void showSuccess(String message) {
        statusLabel.setText("Success: " + message);
        statusLabel.setStyle("-fx-background-color: #E8F8E8; -fx-text-fill: #27AE60; "
                + "-fx-padding: 8 12 8 12; -fx-background-radius: 6; "
                + "-fx-border-color: #2ECC71; -fx-border-width: 1; -fx-border-radius: 6;");
        statusLabel.setVisible(true);
    }

    // ---------------------------------------------------------------
    // Helpers
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
