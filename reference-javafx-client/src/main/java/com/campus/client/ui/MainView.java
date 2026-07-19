package com.campus.client.ui;

import com.campus.client.controller.BookingController;
import com.campus.client.controller.FAQController;
import com.campus.client.controller.LoginController;
import com.campus.client.controller.ViewBookingController;
import com.campus.client.mcp.CampusMcpClient;
import com.campus.client.model.DataStorage;
import com.campus.client.model.Resource;
import com.campus.client.model.Student;
import com.campus.client.rag.RagService;
import com.campus.client.service.CampusService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MainView extends BorderPane {

    //UI component for the navigation bar
    private HBox navbar;
    private Circle mcpStatusIndicator;
    private Label mcpStatusLabel;
    private Label userInfoLabel;
    private Button homeBtn;
    private Button bookingBtn;
    private Button historyBtn;
    private Button policyBtn;
    private Button logoutBtn;

    // Multiple content views
    private LoginView loginView;
    private VBox homeContent;
    private String currentStudentId = "";

    private BookingView bookingView;
    private BookingController bookingController;
    private ViewBookingView viewBookingView;
    private ViewBookingController viewBookingController;

    private CampusService campusService;
    private FAQView faqView;
    private VBox availabilityPlaceholder;

    // MCP and Rag references
    private CampusMcpClient mcp;
    private RagService rag;
    private FAQController faqController;



    // status label for logging
    private Label statusLabel;


    private DataStorage dataStorage;
    private LoginController loginController;

    // Constructor for MainView
    public MainView() {
        initDataStorage();
        buildNavbar();
        buildLoginView();
        buildHomeContent();
        buildBookingViews();
        buildFAQView();
        buildPlaceholders();
        buildStatusLabel();

        setTop(navbar);
        showLogin();

    }

    // Locates the data directory related to the project root
    private static File findDataDir() {
        try {
            File classesDir = new File(MainView.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File moduleRoot = classesDir.getParentFile().getParentFile();
            return new File(moduleRoot, "data");
        } catch (Exception e) {
            System.err.println("Could not resolve data folder from classpath: " + e.getMessage());
            return new File("data"); // last resort fallback
        }
    }

    // Initialize the data storage with the correct file path
    private void initDataStorage() {
        File dataDir = findDataDir();
        File userDataFile = new File(dataDir, "userData.txt");
        File bookingFile = new File(dataDir, "bookingRecord.txt");

        try {
            dataStorage = new DataStorage(userDataFile.getPath(), bookingFile.getPath());
            List<Student> students = dataStorage.loadStudents();
            System.out.println("Loaded " + students.size() + " users from: " + userDataFile.getPath());
        } catch (Exception e) {
            System.err.println("Failed to load user data from file: " + e.getMessage());
            dataStorage = new DataStorage(userDataFile.getPath(), bookingFile.getPath());
        }
    }

    /* Creates a callback that is invoked when the login is successful, so that it can store the student id,
       updates the history controller, and switches to the Home view.
    */
    private Consumer<String> createLoginCallback() {
        return studentId -> {
            currentStudentId = studentId;
            if (viewBookingController != null) {
                viewBookingController.setStudentId(studentId);
            }
            showHome();
            showAllNavButtons();
            setStatusMessage("Logged in as: " + studentId);
        };
    }


    // Builds the navigation bar
    private void buildNavbar() {
        navbar = new HBox(10);
        navbar.setPadding(new Insets(8, 15, 8, 15));
        navbar.setStyle("-fx-background-color: #2C3E50;");
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.setId("navbar");

        homeBtn = createHomeLogoutButton("Home");
        bookingBtn = createNavButton("Resource\nBooking");
        historyBtn = createNavButton("Booking\nHistory");
        policyBtn = createNavButton("Policy\nAssistant");
        logoutBtn = createHomeLogoutButton("Logout");

        mcpStatusIndicator = new Circle(8);
        mcpStatusIndicator.setFill(Color.RED);
        mcpStatusIndicator.setId("mcpStatus");

        mcpStatusLabel = new Label("MCP Disconnected");
        mcpStatusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11px;");
        mcpStatusLabel.setId("mcpStatusLabel");
        mcpStatusLabel.setPadding(new Insets(0, 10, 0, 5));

        userInfoLabel = new Label();
        userInfoLabel.setStyle("-fx-text-fill: #95A5A6; -fx-font-size: 11px;");
        userInfoLabel.setId("userInfo");

        hideAllNavButtons();

        homeBtn.setOnAction(e -> showHome());
        bookingBtn.setOnAction(e -> showBooking());
        historyBtn.setOnAction(e -> showHistory());
        policyBtn.setOnAction(e -> showPolicy());
        logoutBtn.setOnAction(e -> handleLogout());

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        navbar.getChildren().addAll(
                homeBtn,
                mcpStatusIndicator,
                mcpStatusLabel,
                bookingBtn,
                historyBtn,
                policyBtn,
                spacer,
                userInfoLabel,
                logoutBtn
        );
    }

    // Create multiple navigation button with hover effect
    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        String base =
                "-fx-background-color: white; " +
                        "-fx-text-fill: #2980B9; " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 3 18; " +
                        "-fx-background-radius: 4; " +
                        "-fx-cursor: hand; " +
                        "-fx-border-color: transparent;";

        String hover =
                "-fx-background-color: #3498DB; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 3 18; " +
                        "-fx-background-radius: 4; " +
                        "-fx-cursor: hand; " +
                        "-fx-border-color: transparent;";

        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));

        btn.setPrefWidth(100);
        btn.setMinWidth(80);

        return btn;
    }

    // Creates a log out button with hover effect
    private Button createHomeLogoutButton(String text) {
        Button btn = new Button(text);
        btn.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        String base =
                "-fx-background-color: white; " +
                        "-fx-text-fill: #2980B9; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 6 14; " +
                        "-fx-background-radius: 4; " +
                        "-fx-cursor: hand; " +
                        "-fx-border-color: transparent;";

        String hover =
                "-fx-background-color: #3498DB; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 6 14; " +
                        "-fx-background-radius: 4; " +
                        "-fx-cursor: hand; " +
                        "-fx-border-color: transparent;";

        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));

        btn.setMinWidth(60);

        return btn;
    }

    private void buildStatusLabel() {
        statusLabel = new Label();
        statusLabel.setVisible(false);
        this.getChildren().add(statusLabel);
    }



    // builds the login screen
    private void buildLoginView() {
        loginView = new LoginView();
    }

    // builds the home screen with welcome message and resource cards
    private void buildHomeContent() {
        homeContent = new VBox(20);
        homeContent.setAlignment(Pos.TOP_CENTER);
        homeContent.setPadding(new Insets(40, 20, 40, 20));
        homeContent.setId("homeContent");
        homeContent.setStyle("-fx-background-color: #F8F9FA;");

        HBox headerRow = new HBox(30);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setId("headerRow");

        // circle with logo
        StackPane logoCircle = new StackPane();
        logoCircle.setPrefSize(70, 70);
        logoCircle.setMaxSize(70, 70);
        logoCircle.setStyle("-fx-background-color: #DCDCDC; -fx-background-radius: 100;");
        Label logoLabel = new Label("CR");
        logoLabel.setFont(Font.font("System", 30));
        logoLabel.setStyle("-fx-text-fill: #2C3E50;");
        logoLabel.setId("logo");
        logoCircle.getChildren().add(logoLabel);

        // welcome text
        VBox textBlock = new VBox(5);
        textBlock.setAlignment(Pos.CENTER_LEFT);

        Label welcomeLabel = new Label("Welcome to Campus Resource Booking Companion");
        welcomeLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        welcomeLabel.setStyle("-fx-text-fill: #2C3E50;");
        welcomeLabel.setId("welcomeLabel");
        welcomeLabel.setWrapText(true);

        Label subtitleLabel = new Label("Campus resources available for booking:");
        subtitleLabel.setFont(Font.font("System", 14));
        subtitleLabel.setStyle("-fx-text-fill: #7F8C8D;");
        subtitleLabel.setId("subtitleLabel");

        textBlock.getChildren().addAll(welcomeLabel, subtitleLabel);
        headerRow.getChildren().addAll(logoCircle, textBlock);

        // row of cards showing each resource type
        HBox cardsRow = new HBox(25);
        cardsRow.setAlignment(Pos.CENTER);
        cardsRow.setId("cardsRow");
        cardsRow.setPadding(new Insets(20, 0, 0, 0));

        // only show cards if campusService has loaded resources
        if (campusService != null) {
            List<Resource> resources = campusService.getAllResources();
            if (!resources.isEmpty()) {
                // group resources by type so we can show one card per type
                Map<String, List<Resource>> grouped = new HashMap<>();
                for (Resource r : resources) {
                    String type = r.getTypeName();
                    grouped.computeIfAbsent(type, k -> new ArrayList<>()).add(r);
                }

                // show cards in this specific order
                String[] typeOrder = {"Discussion Room", "Group Study Room", "Study Pod", "Lab Workstation", "Basketball Court"};
                for (String typeName : typeOrder) {
                    List<Resource> rooms = grouped.get(typeName);
                    if (rooms != null && !rooms.isEmpty()) {
                        StringBuilder roomsStr = new StringBuilder();
                        for (int i = 0; i < rooms.size(); i++) {
                            if (i > 0) roomsStr.append(", ");
                            roomsStr.append(rooms.get(i).getResourceId());
                        }
                        String capacity = "Capacity: " + rooms.get(0).getCapacity();
                        String building = "Building: " + rooms.get(0).getBuilding();
                        String hours = "Open: " + ((rooms.get(0).getOpenTime() != null) ?
                                rooms.get(0).getOpenTime() + " - " + rooms.get(0).getCloseTime() :
                                "Check booking page");
                        cardsRow.getChildren().add(
                                createInfoCard(typeName, roomsStr.toString(), capacity, building, hours)
                        );
                    }
                }
            }
        }

        VBox cardsWrapper = new VBox(cardsRow);
        cardsWrapper.setAlignment(Pos.CENTER);
        cardsWrapper.setPrefWidth(Double.MAX_VALUE);

        VBox spacer = new VBox(10);
        spacer.setPrefHeight(10);

        homeContent.getChildren().addAll(spacer, headerRow, cardsWrapper);
    }

    // creates a card showing resource info
    private VBox createInfoCard(String name, String rooms, String capacity, String building, String hours) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
                "-fx-background-color: #F0F4F8; " +
                        "-fx-border-color: #E8ECF0; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 14; " +
                        "-fx-background-radius: 14; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);"
        );
        card.setPadding(new Insets(18, 16, 18, 16));
        card.setPrefWidth(190);
        card.setPrefHeight(270);

        // icon
        Label iconLabel = new Label(name.substring(0, 1));
        iconLabel.setFont(Font.font("System", FontWeight.BOLD, 36));
        iconLabel.setStyle("-fx-text-fill: #2C3E50;");
        iconLabel.setMaxWidth(Double.MAX_VALUE);
        iconLabel.setAlignment(Pos.CENTER);

        // resource type name
        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        nameLabel.setStyle("-fx-text-fill: #2C3E50;");
        nameLabel.setWrapText(true);
        nameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setMaxWidth(160);

        // decorative line
        Label separator = new Label("─");
        separator.setStyle("-fx-text-fill: #D5D8DC;");
        separator.setFont(Font.font("System", 12));
        separator.setMaxWidth(Double.MAX_VALUE);
        separator.setAlignment(Pos.CENTER);

        // room ids list
        Label roomLabel = new Label(rooms);
        roomLabel.setFont(Font.font("System", 11));
        roomLabel.setStyle("-fx-text-fill: #5D6D7E;");
        roomLabel.setWrapText(true);
        roomLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        roomLabel.setAlignment(Pos.CENTER);
        roomLabel.setMaxWidth(160);

        // capacity info
        Label capacityLabel = new Label(capacity);
        capacityLabel.setFont(Font.font("System", 11));
        capacityLabel.setStyle("-fx-text-fill: #5D6D7E;");
        capacityLabel.setMaxWidth(Double.MAX_VALUE);
        capacityLabel.setAlignment(Pos.CENTER);

        // building info
        Label buildingLabel = new Label(building);
        buildingLabel.setFont(Font.font("System", 11));
        buildingLabel.setStyle("-fx-text-fill: #5D6D7E;");
        buildingLabel.setMaxWidth(Double.MAX_VALUE);
        buildingLabel.setAlignment(Pos.CENTER);

        // opening hours in green
        Label hoursLabel = new Label(hours);
        hoursLabel.setFont(Font.font("System", 11));
        hoursLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
        hoursLabel.setMaxWidth(Double.MAX_VALUE);
        hoursLabel.setAlignment(Pos.CENTER);

        // some resources are not bookable due to mcp server limitations, this is also the bug of mcp server when we doing
        boolean showUnavailable = !name.equals("Study Pod") && !name.equals("Basketball Court");
        Label unavailableLabel = new Label("     Currently unavailable\ndue to MCP server limitation");
        unavailableLabel.setFont(Font.font("System", 8));
        unavailableLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
        unavailableLabel.setAlignment(Pos.CENTER);
        unavailableLabel.setVisible(showUnavailable);
        unavailableLabel.setManaged(showUnavailable);
        unavailableLabel.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(
                iconLabel,
                nameLabel,
                separator,
                roomLabel,
                capacityLabel,
                buildingLabel,
                hoursLabel,
                unavailableLabel
        );
        return card;
    }

    // creates the booking views
    private void buildBookingViews() {
        bookingView = new BookingView();
        viewBookingView = new ViewBookingView();
    }

    // creates the faq view
    private void buildFAQView() {
        faqView = new FAQView();
    }

    // creates the faq controller when rag is available
    private void rebuildFAQView() {
        if (rag != null && faqView != null) {
            faqController = new FAQController(rag, faqView);
            faqView.setController(faqController);
        } else {
            System.out.println("No rag ");
        }
    }

    // placeholder for features that are not implemented yet
    private void buildPlaceholders() {
        availabilityPlaceholder = createPlaceholder("Resource Availability");
    }

    private VBox createPlaceholder(String title) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(100));
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        Label msgLabel = new Label("Coming soon...");
        msgLabel.setStyle("-fx-text-fill: #7F8C8D;");
        box.getChildren().addAll(titleLabel, msgLabel);
        return box;
    }

    // navigation methods
    public void showLogin() {
        setCenter(loginView);
        loginView.onShow();
        hideAllNavButtons();
        userInfoLabel.setText("");
    }

    public void showHome() {
        setCenter(homeContent);
        showAllNavButtons();
        updateMCPStatus(mcp != null);
        if (bookingView != null) bookingView.setStudentId(currentStudentId);
    }

    public void showBooking() {
        if (bookingView != null) {
            setCenter(bookingView);
            bookingView.onShow();
            bookingView.setStudentId(currentStudentId);
        } else {
            setCenter(createPlaceholder("Resource Booking"));
        }
    }

    public void showHistory() {
        if (viewBookingView != null) {
            setCenter(viewBookingView);
            viewBookingView.onShow();
        } else {
            setCenter(createPlaceholder("Booking History"));
        }
    }

    public void showPolicy() {
        if (faqView != null && faqController != null) {
            setCenter(faqView.getRoot());
            faqView.onShow();
            System.out.println("FAQView displayed");
        } else {
            setCenter(createPlaceholder("Policy Assistant"));
        }
    }

    // handles logout with confirmation dialog
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to logout?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                currentStudentId = "";
                if (loginView != null) loginView.clearFields();
                showLogin();
                hideAllNavButtons();
                userInfoLabel.setText("");
            }
        });
    }

    // helper methods for showing and hiding navbar buttons
    public void showAllNavButtons() {
        homeBtn.setVisible(true);
        bookingBtn.setVisible(true);
        historyBtn.setVisible(true);
        policyBtn.setVisible(true);
        logoutBtn.setVisible(true);
    }

    public void hideAllNavButtons() {
        homeBtn.setVisible(false);
        bookingBtn.setVisible(false);
        historyBtn.setVisible(false);
        policyBtn.setVisible(false);
        logoutBtn.setVisible(false);
    }

    public void setStatusMessage(String message) {
        userInfoLabel.setText(message);
    }

    public void updateMCPStatus(boolean connected) {
        Platform.runLater(() -> {
            mcpStatusIndicator.setFill(connected ? Color.GREEN : Color.RED);
            mcpStatusLabel.setText(connected ? "MCP Connected" : "MCP Disconnected");
        });
    }

    public LoginView getLoginView() { return loginView; }

    // binds the mcp client and rag service to the views and creates all controllers
    public void bind(CampusMcpClient mcp, RagService rag) {
        // quick test call to check if server is responding
        if (mcp != null) {
            new Thread(() -> {
                try {
                    String result = mcp.callTool("check_room_availability", Map.of("date", "2026-07-25"));
                } catch (Exception e) { e.printStackTrace(); }
            }).start();
        }

        this.mcp = mcp;
        this.rag = rag;
        updateMCPStatus(mcp != null);

        rebuildFAQView();

        if (mcp != null && bookingView != null && viewBookingView != null) {
            campusService = new CampusService(mcp, dataStorage);
            campusService.loadResources();

            loginController = new LoginController(loginView, campusService, createLoginCallback());

            bookingController = new BookingController(bookingView, campusService);
            bookingView.setController(bookingController);

            // rebuild home content with real resource data now that we have campusService
            buildHomeContent();

            viewBookingController = new ViewBookingController(viewBookingView, campusService);
            viewBookingView.setController(viewBookingController);

            if (!currentStudentId.isEmpty()) {
                viewBookingController.setStudentId(currentStudentId);
            }
        }
    }
}