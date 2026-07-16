package com.campus.client.ui;

import com.campus.client.controller.BookingController;
import com.campus.client.controller.FAQController;
import com.campus.client.controller.ViewBookingController;
import com.campus.client.mcp.CampusMcpClient;
import com.campus.client.model.DataStorage;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Main navigation container for the Campus Resource Booking Companion.
 * Home screen shows a welcome message and 4 informational resource cards
 * with operating hours. Cards are for display only - users navigate via navbar.
 *
 * MCP status is independent of user login - it shows the actual connection
 * state to the MCP server, not the user's session state.
 */
public class MainView extends BorderPane {

    // ================================================================
    // NAVBAR COMPONENTS
    // ================================================================

    private HBox navbar;
    private Circle mcpStatusIndicator;
    private Label mcpStatusLabel;
    private Label userInfoLabel;

    private Button homeBtn;
    private Button bookingBtn;
    private Button historyBtn;
    private Button policyBtn;
    private Button logoutBtn;

    // ================================================================
    // VIEWS (Content areas)
    // ================================================================

    private LoginView loginView;
    private VBox homeContent;
    private String currentStudentId = "";

    // ResourceBooking View
    private BookingView bookingView;
    private BookingController bookingController;
    // BookingHistory View
    private ViewBookingView viewBookingView;
    private ViewBookingController viewBookingController;

    // Shared service
    private CampusService campusService;

    // PolicyAssistant View
    private FAQView faqView;

    // Placeholders
    private VBox availabilityPlaceholder;

    // ================================================================
    // MCP/RAG REFERENCES
    // ================================================================

    private CampusMcpClient mcp;
    private RagService rag;
    private FAQController faqController;

    // ================================================================
    // BACKGROUND THREAD
    // ================================================================

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ui-worker");
        t.setDaemon(true);
        return t;
    });

    // ================================================================
    // STATUS LABEL
    // ================================================================

    private Label statusLabel;

    // ================================================================
    // DATA STORAGE
    // ================================================================

    private DataStorage dataStorage;
    private final Map<String, String> userCredentials = new HashMap<>();

    // ================================================================
    // CONSTRUCTOR
    // ================================================================

    public MainView() {
        initDataStorage();

        buildNavbar();
        buildLoginView();
        buildHomeContent();
        buildBookingViews();
        buildFAQView();
        buildPlaceholders();
        buildStatusLabel();

        setupLoginAction();

        setTop(navbar);
        showLogin();

        System.out.println("DEBUG: FAQView is " + (faqView == null ? "NULL" : "CREATED"));
        System.out.println("DEBUG: RAG is " + (rag == null ? "NULL" : "NOT NULL"));
    }

    // ================================================================
    // INIT DATA STORAGE
    // ================================================================

    private void initDataStorage() {
        try {
            dataStorage = new DataStorage(
                    "reference-javafx-client/data/userData.txt",
                    "reference-javafx-client/data/bookingRecord.txt"
            );

            List<Student> students = dataStorage.loadStudents();

            for (Student student : students) {
                userCredentials.put(student.getStudentId(), student.getStudentPassword());
                System.out.println("Loaded user: " + student.getStudentId() + " -> " + student.getStudentName());
            }

            System.out.println("Total users loaded from file: " + userCredentials.size());

            if (userCredentials.isEmpty()) {
                System.err.println("No users found in userData.txt. Using hardcoded users.");
                initHardcodedTestUsers();
            }

        } catch (Exception e) {
            System.err.println("Failed to load user data from file: " + e.getMessage());
            System.err.println("Falling back to hardcoded test users...");
            initHardcodedTestUsers();
        }
    }

    private void initHardcodedTestUsers() {
        userCredentials.put("0375421", "password123");
        userCredentials.put("0377465", "password123");
        userCredentials.put("0387332", "password123");
        userCredentials.put("0376612", "password123");
        userCredentials.put("0387409", "password123");
        userCredentials.put("1234567", "test123");
        System.out.println("Loaded " + userCredentials.size() + " hardcoded test users");
    }

    // ================================================================
    // SETUP LOGIN ACTION
    // ================================================================

    private void setupLoginAction() {
        loginView.setLoginAction(() -> {
            loginView.clearFieldErrors();

            String id = loginView.getStudentId();
            String password = loginView.getPassword();

            if (id.isEmpty()) {
                loginView.showStudentIdError("Please enter a Student ID");
                return;
            }
            if (password.isEmpty()) {
                loginView.showPasswordError("Please enter a password");
                return;
            }

            if (isValidUser(id, password)) {
                currentStudentId = id;
                System.out.println("Login successful for: " + id);
                if (viewBookingController != null) {
                    viewBookingController.setStudentId(id);
                }
                showHome();
                showAllNavButtons();
                setStatusMessage("Logged in as: " + id);
                loginView.clearFields();
            } else {
                loginView.showPasswordError("Invalid Student ID or password.");
                System.out.println("Login failed for: " + id);
            }
        });
    }

    private boolean isValidUser(String id, String password) {
        String expectedPassword = userCredentials.get(id);
        return expectedPassword != null && expectedPassword.equals(password);
    }

    // ================================================================
    // SHARED NAVBAR BUILDER
    // ================================================================

    private void buildNavbar() {
        navbar = new HBox(10);
        navbar.setPadding(new Insets(8, 15, 8, 15));
        navbar.setStyle("-fx-background-color: #2C3E50;");
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.setId("navbar");

        // Navigation Buttons
        homeBtn = createHomeLogoutButton("Home");
        bookingBtn = createNavButton("Resource\nBooking");
        historyBtn = createNavButton("Booking\nHistory");
        policyBtn = createNavButton("Policy\nAssistant");
        logoutBtn = createHomeLogoutButton("Logout");

        // MCP Status indicator
        mcpStatusIndicator = new Circle(8);
        mcpStatusIndicator.setFill(Color.RED);
        mcpStatusIndicator.setId("mcpStatus");

        mcpStatusLabel = new Label("MCP Disconnected");
        mcpStatusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11px;");
        mcpStatusLabel.setId("mcpStatusLabel");
        mcpStatusLabel.setPadding(new Insets(0, 10, 0, 5));

        // User Info
        userInfoLabel = new Label();
        userInfoLabel.setStyle("-fx-text-fill: #95A5A6; -fx-font-size: 11px;");
        userInfoLabel.setId("userInfo");

        // Hide buttons until login
        hideAllNavButtons();

        // Event Handlers
        homeBtn.setOnAction(e -> showHome());
        bookingBtn.setOnAction(e -> showBooking());
        historyBtn.setOnAction(e -> showHistory());
        policyBtn.setOnAction(e -> showPolicy());
        logoutBtn.setOnAction(e -> handleLogout());

        // Spacer
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Add all to navbar
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

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        String base =
                "-fx-background-color: #D9D9D9; " +
                        "-fx-text-fill: #2C2C2C; " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-weight: normal; " +
                        "-fx-padding: 3 18; " +
                        "-fx-background-radius: 4; " +
                        "-fx-cursor: hand; " +
                        "-fx-border-color: transparent;";

        String hover =
                "-fx-background-color: #B0B0B0; " +
                        "-fx-text-fill: #2C2C2C; " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-weight: normal; " +
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

    private Button createHomeLogoutButton(String text) {
        Button btn = new Button(text);
        btn.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        String base =
                "-fx-background-color: #D9D9D9; " +
                        "-fx-text-fill: #2C2C2C; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: normal; " +
                        "-fx-padding: 6 14; " +
                        "-fx-background-radius: 4; " +
                        "-fx-cursor: hand; " +
                        "-fx-border-color: transparent;";

        String hover =
                "-fx-background-color: #B0B0B0; " +
                        "-fx-text-fill: #2C2C2C; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: normal; " +
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

    // ================================================================
    // STATUS LABEL
    // ================================================================

    private void buildStatusLabel() {
        statusLabel = new Label();
        statusLabel.setVisible(false);
        this.getChildren().add(statusLabel);
    }

    public void setStatus(String message) {
        Platform.runLater(() -> {
            userInfoLabel.setText(message);
        });
    }

    // ================================================================
    // VIEW BUILDERS
    // ================================================================

    private void buildLoginView() {
        loginView = new LoginView();
    }

    /**
     * ===== HOME CONTENT - WELCOME PAGE WITH 4 INFO CARDS =====
     * Shows a welcome message and 4 non-clickable resource cards.
     * Cards display room IDs, capacity, building, and operating hours.
     */
    private void buildHomeContent() {
        homeContent = new VBox(40);
        homeContent.setAlignment(Pos.TOP_LEFT);
        homeContent.setPadding(new Insets(40, 50, 40, 50));
        homeContent.setId("homeContent");
        homeContent.setStyle("-fx-background-color: #F8F9FA;");

        // ---- Top row: logo circle + heading/subtitle ----
        HBox headerRow = new HBox(30);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setId("headerRow");

        StackPane logoCircle = new StackPane();
        logoCircle.setPrefSize(100, 100);
        logoCircle.setMaxSize(100, 100);
        logoCircle.setStyle("-fx-background-color: #DCDCDC; -fx-background-radius: 100;");
        Label logoLabel = new Label("CR");
        logoLabel.setFont(Font.font("System", 40));
        logoLabel.setStyle("-fx-text-fill: #2C3E50;");
        logoLabel.setId("logo");
        logoCircle.getChildren().add(logoLabel);

        VBox textBlock = new VBox(10);
        textBlock.setAlignment(Pos.CENTER_LEFT);

        Label welcomeLabel = new Label("Welcome to Campus Resource Booking Companion");
        welcomeLabel.setFont(Font.font("System", FontWeight.BOLD, 26));
        welcomeLabel.setStyle("-fx-text-fill: #2C3E50;");
        welcomeLabel.setId("welcomeLabel");
        welcomeLabel.setWrapText(true);

        Label subtitleLabel = new Label("Campus resources available for booking:");
        subtitleLabel.setFont(Font.font("System", 16));
        subtitleLabel.setStyle("-fx-text-fill: #7F8C8D;");
        subtitleLabel.setId("subtitleLabel");

        textBlock.getChildren().addAll(welcomeLabel, subtitleLabel);
        headerRow.getChildren().addAll(logoCircle, textBlock);

        // ===== 4 INFORMATION CARDS WITH HOURS =====
        HBox cardsRow = new HBox(25);
        cardsRow.setAlignment(Pos.CENTER);
        cardsRow.setId("cardsRow");
        cardsRow.setPadding(new Insets(15, 0, 0, 0));

        cardsRow.getChildren().addAll(
                createInfoCard("Discussion Room", "D9A.01, D9B.01, E9A.03", "Capacity: 4-8", "Building: D, E", "Open: 08:00 - 21:00"),
                createInfoCard("Study Pod", "KA-P1, KA-P2", "Capacity: 1-2", "Building: LIB", "Open: 07:00 - 23:00"),
                createInfoCard("Lab Workstation", "C7.01", "Capacity: 30", "Building: LAB", "Open: 08:00 - 22:00"),
                createInfoCard("Sports Facility", "SP-B1", "Capacity: 40", "Building: OUT", "Open: 07:00 - 22:00")
        );

        VBox cardsWrapper = new VBox(cardsRow);
        cardsWrapper.setAlignment(Pos.CENTER);
        cardsWrapper.setPrefWidth(Double.MAX_VALUE);

        homeContent.getChildren().addAll(headerRow, cardsWrapper);
    }

    /**
     * ===== BIGGER INFORMATION CARD WITH OPERATING HOURS =====
     * Displays resource information including operating hours.
     * No click handler - just shows information about campus resources.
     */
    private VBox createInfoCard(String name, String rooms, String capacity, String building, String hours) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #E8ECF0; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 14; " +
                        "-fx-background-radius: 14; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 4);"
        );
        card.setPadding(new Insets(25, 30, 25, 30));
        card.setPrefWidth(180);
        card.setPrefHeight(220);

        // ---- Hover effect (just visual, no click) ----
        card.setOnMouseEntered(e -> {
            card.setStyle(
                    "-fx-background-color: #F8F9FA; " +
                            "-fx-border-color: #B0B0B0; " +
                            "-fx-border-width: 1.5; " +
                            "-fx-border-radius: 14; " +
                            "-fx-background-radius: 14; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 6);"
            );
            card.setScaleX(1.02);
            card.setScaleY(1.02);
        });

        card.setOnMouseExited(e -> {
            card.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-border-color: #E8ECF0; " +
                            "-fx-border-width: 1; " +
                            "-fx-border-radius: 14; " +
                            "-fx-background-radius: 14; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 4);"
            );
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        // ---- Icon ----
        Label iconLabel = new Label(name.substring(0, 1));
        iconLabel.setFont(Font.font("System", FontWeight.BOLD, 38));
        iconLabel.setStyle("-fx-text-fill: #2C3E50;");
        iconLabel.setId("cardIcon");

        // ---- Resource Name ----
        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        nameLabel.setStyle("-fx-text-fill: #2C3E50;");
        nameLabel.setId("cardName");

        // ---- Separator ----
        Label separator = new Label("─");
        separator.setStyle("-fx-text-fill: #D5D8DC;");
        separator.setFont(Font.font("System", 12));

        // ---- Room IDs ----
        Label roomLabel = new Label(rooms);
        roomLabel.setFont(Font.font("System", 11));
        roomLabel.setStyle("-fx-text-fill: #5D6D7E;");
        roomLabel.setId("cardRooms");

        // ---- Capacity ----
        Label capacityLabel = new Label(capacity);
        capacityLabel.setFont(Font.font("System", 11));
        capacityLabel.setStyle("-fx-text-fill: #5D6D7E;");
        capacityLabel.setId("cardCapacity");

        // ---- Building ----
        Label buildingLabel = new Label(building);
        buildingLabel.setFont(Font.font("System", 11));
        buildingLabel.setStyle("-fx-text-fill: #5D6D7E;");
        buildingLabel.setId("cardBuilding");

        // ---- Operating Hours ----
        Label hoursLabel = new Label(hours);
        hoursLabel.setFont(Font.font("System", 11));
        hoursLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
        hoursLabel.setId("cardHours");

        card.getChildren().addAll(iconLabel, nameLabel, separator, roomLabel, capacityLabel, buildingLabel, hoursLabel);
        return card;
    }

    private void buildBookingViews() {
        bookingView = new BookingView();
        viewBookingView = new ViewBookingView();

        CampusMcpClient bookingMcpClient = (mcp != null) ? mcp : new CampusMcpClient("http://localhost:8080");
        campusService = new CampusService(bookingMcpClient, dataStorage);

        bookingController = new BookingController(bookingView, campusService);
        viewBookingController = new ViewBookingController(viewBookingView, campusService);
    }

    private void buildFAQView() {
        faqView = new FAQView();

        if (rag != null) {
            faqController = new FAQController(rag, faqView);
            faqView.setController(faqController);
            System.out.println("FAQController created and connected to FAQView");
        } else {
            System.out.println("RAG service not available - FAQ will show error");
        }
    }

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

    // ================================================================
    // NAVIGATION METHODS
    // ================================================================

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

        if (bookingView != null) {
            bookingView.setStudentId(currentStudentId);
        }
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
        System.out.println("showPolicy() called");
        System.out.println("faqView is: " + (faqView == null ? "NULL" : "NOT NULL"));

        if (faqView != null) {
            setCenter(faqView.getRoot());
            faqView.onShow();
            System.out.println("FAQView displayed");
        } else {
            System.out.println("faqView is NULL - showing placeholder");
            setCenter(createPlaceholder("Policy Assistant"));
        }
    }

    // ================================================================
    // LOGOUT (FR10) - FIXED: MCP Status Stays Connected
    // ================================================================

    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to logout?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                currentStudentId = "";
                if (loginView != null) {
                    loginView.clearFields();
                }
                showLogin();
                hideAllNavButtons();
                userInfoLabel.setText("");
                // ===== FIX: MCP connection is independent of user login =====
                // Do NOT change MCP status on logout - it should stay connected
                // updateMCPStatus(false);  // ← REMOVED
            }
        });
    }

    // ================================================================
    // HELPER METHODS
    // ================================================================

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

    public LoginView getLoginView() {
        return loginView;
    }

    public ExecutorService getWorker() {
        return worker;
    }

    // ================================================================
    // MCP BINDING
    // ================================================================

    public void bind(CampusMcpClient mcp, RagService rag) {
        this.mcp = mcp;
        this.rag = rag;
        updateMCPStatus(mcp != null);

        if (mcp != null && bookingView != null && viewBookingView != null) {
            campusService = new CampusService(mcp, dataStorage);
            bookingController = new BookingController(bookingView, campusService);
            viewBookingController = new ViewBookingController(viewBookingView, campusService);
            if (!currentStudentId.isEmpty()) {
                viewBookingController.setStudentId(currentStudentId);
            }
            System.out.println("Booking screens rebuilt with a real connected MCP client");
        }

        if (rag != null && faqView != null) {
            faqController = new FAQController(rag, faqView);
            faqView.setController(faqController);
            System.out.println("FAQController created with RAG in bind()");
        } else if (rag == null) {
            System.out.println("RAG service not available (API key missing?)");
        }
    }

    public CampusMcpClient getMcp() {
        return mcp;
    }

    public RagService getRag() {
        return rag;
    }

    // ================================================================
    // REFRESH DISCOVERY (For debugging)
    // ================================================================

    public void refreshDiscovery() {
        if (mcp == null) {
            return;
        }
        worker.submit(() -> {
            try {
                String tools = mcp.listTools().stream()
                        .map(t -> "  • " + t.name() + " — " + t.description())
                        .collect(Collectors.joining("\n"));
                System.out.println("MCP Tools:\n" + tools);
            } catch (Exception e) {
                System.err.println("Discovery failed: " + e.getMessage());
            }
        });
    }

    // ================================================================
    // getRoot() - For App.java
    // ================================================================

    public BorderPane getRoot() {
        return this;
    }
}