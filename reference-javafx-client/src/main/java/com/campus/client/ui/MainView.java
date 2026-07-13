package com.campus.client.ui;

import com.campus.client.controller.FAQController;
import com.campus.client.mcp.CampusMcpClient;
import com.campus.client.model.DataStorage;
import com.campus.client.model.Student;
import com.campus.client.rag.RagService;
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
 * This class owns the one shared navbar across all screens.
 */
public class MainView extends BorderPane {

    // NavBar Components
    private HBox navbar;
    private Circle mcpStatusIndicator;
    private Label mcpStatusLabel;
    private Label userInfoLabel;

    private Button homeBtn;
    private Button bookingBtn;
    private Button historyBtn;
    private Button policyBtn;
    private Button logoutBtn;

    // Views (Content areas)
    private LoginView loginView;
    private VBox homeContent;
    private String currentStudentId = "";

    // ResourceBooking View
    private BookingView bookingView;
    // BookingHistory View
    private ViewBookingView viewBookingView;

    // PolicyAssistant View
    private FAQView faqView;

    // Placeholders (keep for Availability)
    private VBox availabilityPlaceholder;


    // MCP/RAG References
    private CampusMcpClient mcp;
    private RagService rag;
    private FAQController faqController;


    // BackgroundThread
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ui-worker");
        t.setDaemon(true);
        return t;
    });

    //Status Label  (For App.java's setStatus() calls)
    private Label statusLabel;

    // DataStorage instance for reading user data from file
    private DataStorage dataStorage;

    // User data loaded from file (studentId -> password)
    private final Map<String, String> userCredentials = new HashMap<>();


    // Constructor
    public MainView() {
        // Initialize DataStorage and load users from file
        initDataStorage();

        buildNavbar();          // Build the shared navbar
        buildLoginView();       // Create login screen
        buildHomeContent();     // Create home screen with cards
        buildBookingViews();    // Create booking views
        buildFAQView();         // Create FAQ view
        buildPlaceholders();    // Create placeholder for Availability only
        buildStatusLabel();     // Create hidden status label

        // Set up login action
        setupLoginAction();

        setTop(navbar);
        showLogin();

        // Debug: Check if FAQView was created
        System.out.println("DEBUG: FAQView is " + (faqView == null ? "NULL" : "CREATED"));
        System.out.println("DEBUG: RAG is " + (rag == null ? "NULL" : "NOT NULL"));
    }


    // Loads users from userData.txt
    private void initDataStorage() {
        try {
            // Create DataStorage with file paths
            // DataStorage(String userDataFilePath, String bookingHistoryFilePath)
            dataStorage = new DataStorage(
                    "data/userData.txt",      // User data file
                    "data/bookingHistory.txt" // Booking history file
            );

            // Load students from userData.txt
            List<Student> students = dataStorage.loadStudents();

            // Populate the userCredentials map
            for (Student student : students) {
                userCredentials.put(student.getStudentId(), student.getStudentPassword());
                System.out.println("Loaded user: " + student.getStudentId() + " -> " + student.getStudentName());
            }

            System.out.println("Total users loaded from file: " + userCredentials.size());

            // If no users loaded, fall back to hardcoded
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

     //Fallback method for hardcoded test users if file loading fails.
    private void initHardcodedTestUsers() {
        userCredentials.put("0375421", "password123");
        userCredentials.put("0377465", "password123");
        userCredentials.put("0387332", "password123");
        userCredentials.put("0376612", "password123");
        userCredentials.put("0387409", "password123");
        userCredentials.put("1234567", "test123");
        System.out.println("Loaded " + userCredentials.size() + " hardcoded test users");
    }

    // Setup login action
    private void setupLoginAction() {
        loginView.setLoginAction(() -> {
            loginView.clearFieldErrors();

            String id = loginView.getStudentId();
            String password = loginView.getPassword();

            // Validate empty fields
            if (id.isEmpty()) {
                loginView.showStudentIdError("Please enter a Student ID");
                return;
            }
            if (password.isEmpty()) {
                loginView.showPasswordError("Please enter a password");
                return;
            }

            // Validate against user credentials from file
            if (isValidUser(id, password)) {
                // Success!
                currentStudentId = id;
                System.out.println("Login successful for: " + id);
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

    //Validates a user against credentials loaded from userData.txt.
    private boolean isValidUser(String id, String password) {
        String expectedPassword = userCredentials.get(id);
        return expectedPassword != null && expectedPassword.equals(password);
    }

    // Shared NavBar Builder
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

        // User Info (shows logged-in student ID)
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

        // Spacer pushes user info to the right
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

    // Creates 4 compact navigation button for main resource
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

    // Creates a button for Home and Logout.
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

    // Status Label (For App.java's setStatus() calls)
    private void buildStatusLabel() {
        statusLabel = new Label();
        statusLabel.setVisible(false);
        this.getChildren().add(statusLabel);
    }

    // Sets the status message in the navbar.Called by App.java via view.setStatus()
    public void setStatus(String message) {
        Platform.runLater(() -> {
            userInfoLabel.setText(message);
        });
    }

    // View Builders
    private void buildLoginView() {
        loginView = new LoginView();
    }

    private void buildHomeContent() {
        homeContent = new VBox(35);
        homeContent.setAlignment(Pos.TOP_LEFT);
        homeContent.setPadding(new Insets(40, 50, 40, 50));
        homeContent.setId("homeContent");
        homeContent.setStyle("-fx-background-color: #F8F9FA;");

        // logo circle BESIDE the heading/subtitle
        HBox headerRow = new HBox(30);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setId("headerRow");

        StackPane logoCircle = new StackPane();
        logoCircle.setPrefSize(90, 90);
        logoCircle.setMaxSize(90, 90);
        logoCircle.setStyle("-fx-background-color: #DCDCDC; -fx-background-radius: 100;");
        Label logoLabel = new Label("CR");
        logoLabel.setFont(Font.font("System", 35));
        logoLabel.setId("logo");
        logoCircle.getChildren().add(logoLabel);

        VBox textBlock = new VBox(8);
        textBlock.setAlignment(Pos.CENTER_LEFT);

        Label welcomeLabel = new Label("Welcome to Campus Resource Booking Companion");
        welcomeLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        welcomeLabel.setStyle("-fx-text-fill: #2C3E50;");
        welcomeLabel.setId("welcomeLabel");
        welcomeLabel.setWrapText(true);

        Label subtitleLabel = new Label("Select a resource type to begin booking");
        subtitleLabel.setFont(Font.font("System", 14));
        subtitleLabel.setStyle("-fx-text-fill: #7F8C8D;");
        subtitleLabel.setId("subtitleLabel");

        textBlock.getChildren().addAll(welcomeLabel, subtitleLabel);
        headerRow.getChildren().addAll(logoCircle, textBlock);

        // 4 Resource Cards in one row
        HBox cardsRow = new HBox(20);
        cardsRow.setAlignment(Pos.CENTER);
        cardsRow.setId("cardsRow");
        cardsRow.setPadding(new Insets(10, 0, 0, 0));

        cardsRow.getChildren().addAll(
                createResourceCard("Discussion Room"),
                createResourceCard("Study Pod"),
                createResourceCard("Lab Workstation"),
                createResourceCard("Sports Facility")
        );

        VBox cardsWrapper = new VBox(cardsRow);
        cardsWrapper.setAlignment(Pos.CENTER);
        cardsWrapper.setPrefWidth(Double.MAX_VALUE);

        homeContent.getChildren().addAll(headerRow, cardsWrapper);
    }

    private VBox createResourceCard(String name) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #E8ECF0; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 12; " +
                        "-fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 4); " +
                        "-fx-cursor: hand;"
        );
        card.setPadding(new Insets(25, 35, 25, 35));
        card.setPrefWidth(150);
        card.setPrefHeight(120);

        card.setOnMouseEntered(e -> {
            card.setStyle(
                    "-fx-background-color: #F0F0F0; " +
                            "-fx-border-color: #CCCCCC; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 12; " +
                            "-fx-background-radius: 12; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 6); " +
                            "-fx-cursor: hand;"
            );
            card.setScaleX(1.02);
            card.setScaleY(1.02);
        });

        card.setOnMouseExited(e -> {
            card.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-border-color: #E8ECF0; " +
                            "-fx-border-width: 1; " +
                            "-fx-border-radius: 12; " +
                            "-fx-background-radius: 12; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 4); " +
                            "-fx-cursor: hand;"
            );
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        Label iconLabel = new Label(name.substring(0, 1));
        iconLabel.setFont(Font.font("System", FontWeight.BOLD, 32));
        iconLabel.setStyle("-fx-text-fill: #2C3E50;");
        iconLabel.setId("cardIcon");

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        nameLabel.setStyle("-fx-text-fill: #2C3E50;");
        nameLabel.setId("cardName");

        card.getChildren().addAll(iconLabel, nameLabel);

        // Click event - navigates to booking
        card.setOnMouseClicked(e -> showBooking());

        return card;
    }

    // Creates the booking views
    private void buildBookingViews() {
        bookingView = new BookingView();
        viewBookingView = new ViewBookingView();
    }

    // Creates the Policy Assistant view from Member 5.
    private void buildFAQView() {
        // Create the FAQ view
        faqView = new FAQView();

        // If RAG is available, create controller and connect it
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

    // Navigation methods
    public void showLogin() {
        setCenter(loginView);
        loginView.onShow();
        hideAllNavButtons();
        userInfoLabel.setText("");
    }

    // Shows the home screen with resource cards.
    public void showHome() {
        setCenter(homeContent);
        showAllNavButtons();
        updateMCPStatus(mcp != null);

        if (bookingView != null) {
            bookingView.setStudentId(currentStudentId);
        }
    }


    // Shows the Resource Booking screen
    public void showBooking() {
        if (bookingView != null) {
            setCenter(bookingView);
            bookingView.onShow();
            bookingView.setStudentId(currentStudentId);
        } else {
            setCenter(createPlaceholder("Resource Booking"));
        }
    }

    // Shows the Booking History screen
    public void showHistory() {
        if (viewBookingView != null) {
            setCenter(viewBookingView);
            viewBookingView.onShow();
        } else {
            setCenter(createPlaceholder("Booking History"));
        }
    }

    // Shows the Policy Assistant screen
    public void showPolicy() {
        System.out.println("showPolicy() called");
        System.out.println("faqView is: " + (faqView == null ? "NULL" : "NOT NULL"));

        if (faqView != null) {
            // FAQView uses a BorderPane root, so we need getRoot()
            setCenter(faqView.getRoot());
            faqView.onShow();
            System.out.println("FAQView displayed");
        } else {
            System.out.println("faqView is NULL - showing placeholder");
            setCenter(createPlaceholder("Policy Assistant"));
        }
    }

    // Logout
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
                updateMCPStatus(false);
            }
        });
    }

    // Helper methods
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

    // MCP Binding
    public void bind(CampusMcpClient mcp, RagService rag) {
        this.mcp = mcp;
        this.rag = rag;
        updateMCPStatus(mcp != null);

        // Update FAQ with real RagService
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

    // Refresh Discovery (For debugging)
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

    // getRoot() - For App.java
    public BorderPane getRoot() {
        return this;
    }
}