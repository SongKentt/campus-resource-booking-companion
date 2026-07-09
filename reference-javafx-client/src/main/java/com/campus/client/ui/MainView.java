package com.campus.client.ui;

import com.campus.client.mcp.CampusMcpClient;
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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

// ================================================================
// CLASS DECLARATION
// ================================================================

/**
 * Main navigation container for the Campus Resource Booking Companion.
 *
 * This class owns the SINGLE SHARED NAVBAR used across all screens.
 * All screens (LoginView, BookingView, ViewBookingView, etc.) are displayed
 * in the center content area below this navbar.
 *
 * Layout:
 * - TOP: Navigation bar (shared across all screens)
 * - CENTER: Content area that switches between views
 *
 * Design matches the Figma wireframes from Part A report.
 */
public class MainView extends BorderPane {

    // ================================================================
    // SECTION 1: NAVBAR COMPONENTS (SHARED ACROSS ALL SCREENS)
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
    // SECTION 2: VIEWS (Content areas)
    // ================================================================

    private LoginView loginView;
    private VBox homeContent;
    private String currentStudentId = "";

    // ---- REAL VIEWS (from Member 4) ----
    private BookingView bookingView;
    private ViewBookingView viewBookingView;

    // ---- Placeholders (keep for Availability and Policy) ----
    private VBox availabilityPlaceholder;
    private VBox policyPlaceholder;

    // ================================================================
    // SECTION 3: MCP/RAG REFERENCES (From Reference)
    // ================================================================

    private CampusMcpClient mcp;
    private RagService rag;

    // ================================================================
    // SECTION 4: BACKGROUND THREAD (Prevents UI freezing - NFR2)
    // ================================================================

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ui-worker");
        t.setDaemon(true);
        return t;
    });

    // ================================================================
    // SECTION 5: STATUS LABEL (For App.java's setStatus() calls)
    // ================================================================

    private Label statusLabel;

    // ================================================================
    // SECTION 6: TEST USERS (Hardcoded for testing)
    // ================================================================

    private final Map<String, String> testUsers = new HashMap<>();

    // ================================================================
    // SECTION 7: CONSTRUCTOR
    // ================================================================

    public MainView() {
        // Initialize test users
        initTestUsers();

        buildNavbar();          // Build the shared navbar
        buildLoginView();       // Create login screen
        buildHomeContent();     // Create home screen with cards
        buildBookingViews();    // Create real booking views (Member 4)
        buildPlaceholders();    // Create placeholders for Availability and Policy
        buildStatusLabel();     // Create hidden status label

        // Set up login action
        setupLoginAction();

        setTop(navbar);         // Put navbar at top
        showLogin();            // Start with login screen (FR1)
    }

    // ================================================================
    // SECTION 8: INITIALIZE TEST USERS
    // ================================================================

    private void initTestUsers() {
        testUsers.put("0375421", "password123");
        testUsers.put("0377465", "password123");
        testUsers.put("0387332", "password123");
        testUsers.put("0376612", "password123");
        testUsers.put("0387409", "password123");
        testUsers.put("1234567", "test123");
    }

    // ================================================================
    // SECTION 9: SETUP LOGIN ACTION
    // ================================================================

    /**
     * Sets up the login action for the login view.
     * This handles the login button click event.
     */
    private void setupLoginAction() {
        loginView.setLoginAction(() -> {
            loginView.clearFieldErrors();

            String id = loginView.getStudentId();
            String password = loginView.getPassword();

            // Validate empty fields (FR7)
            if (id.isEmpty()) {
                loginView.showStudentIdError("Please enter a Student ID");
                return;
            }
            if (password.isEmpty()) {
                loginView.showPasswordError("Please enter a password");
                return;
            }

            // Validate against test users
            if (isValidTestUser(id, password)) {
                // Success!
                currentStudentId = id;
                System.out.println("✅ Login successful for: " + id);
                showHome();
                showAllNavButtons();
                setStatusMessage("Logged in as: " + id);
                loginView.clearFields();
            } else {
                loginView.showPasswordError("Invalid Student ID or password.");
                System.out.println("❌ Login failed for: " + id);
            }
        });
    }

    /**
     * Validates a user against the hardcoded test users.
     * Later this will be replaced with real CampusService validation.
     */
    private boolean isValidTestUser(String id, String password) {
        String expectedPassword = testUsers.get(id);
        return expectedPassword != null && expectedPassword.equals(password);
    }

    // ================================================================
    // SECTION 10: SHARED NAVBAR BUILDER (ONE NAVBAR TO RULE THEM ALL)
    // ================================================================

    private void buildNavbar() {
        navbar = new HBox(10);
        navbar.setPadding(new Insets(8, 15, 8, 15));
        navbar.setStyle("-fx-background-color: #2C3E50;");
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.setId("navbar");

        // ---- Navigation Buttons ----
        homeBtn = createHomeLogoutButton("Home");
        bookingBtn = createNavButton("Resource\nBooking");
        historyBtn = createNavButton("Booking\nHistory");
        policyBtn = createNavButton("Policy\nAssistant");
        logoutBtn = createHomeLogoutButton("Logout");

        // ---- MCP Status Indicator (Green/Red circle) ----
        mcpStatusIndicator = new Circle(8);
        mcpStatusIndicator.setFill(Color.RED);
        mcpStatusIndicator.setId("mcpStatus");

        mcpStatusLabel = new Label("MCP Disconnected");
        mcpStatusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11px;");
        mcpStatusLabel.setId("mcpStatusLabel");
        mcpStatusLabel.setPadding(new Insets(0, 10, 0, 5));

        // ---- User Info (shows logged-in student ID) ----
        userInfoLabel = new Label();
        userInfoLabel.setStyle("-fx-text-fill: #95A5A6; -fx-font-size: 11px;");
        userInfoLabel.setId("userInfo");

        // ---- Hide buttons until login ----
        hideAllNavButtons();

        // ---- Event Handlers ----
        homeBtn.setOnAction(e -> showHome());
        bookingBtn.setOnAction(e -> showBooking());
        historyBtn.setOnAction(e -> showHistory());
        policyBtn.setOnAction(e -> showPolicy());
        logoutBtn.setOnAction(e -> handleLogout());

        // ---- Spacer pushes user info to the right ----
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // ---- Add all to navbar ----
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

    /**
     * Creates a compact navigation button - shorter and wider.
     * Used for the 4 main resource buttons.
     */
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

    /**
     * Creates a button with original size for Home and Logout.
     */
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
    // SECTION 11: STATUS LABEL (For App.java's setStatus() calls)
    // ================================================================

    private void buildStatusLabel() {
        // This is a hidden label that App.java uses to set status messages
        // The status messages are shown in userInfoLabel
        statusLabel = new Label();
        statusLabel.setVisible(false);
        // Add it to the root so it exists for App.java
        this.getChildren().add(statusLabel);
    }

    /**
     * Sets the status message in the navbar.
     * Called by App.java via view.setStatus().
     */
    public void setStatus(String message) {
        Platform.runLater(() -> {
            userInfoLabel.setText(message);
        });
    }

    // ================================================================
    // SECTION 12: VIEW BUILDERS
    // ================================================================

    private void buildLoginView() {
        loginView = new LoginView();
    }

    private void buildHomeContent() {
        homeContent = new VBox(35);
        homeContent.setAlignment(Pos.TOP_LEFT);
        homeContent.setPadding(new Insets(40, 50, 40, 50));
        homeContent.setId("homeContent");
        homeContent.setStyle("-fx-background-color: #F8F9FA;");

        // ---- Top row: logo circle BESIDE the heading/subtitle ----
        HBox headerRow = new HBox(30);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setId("headerRow");

        // Circular logo container
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

        // ---- 4 Resource Cards in ONE ROW ----
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

        // ---- Hover effect - SAME for all cards ----
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

        // ---- Card content - First letter as icon ----
        Label iconLabel = new Label(name.substring(0, 1));
        iconLabel.setFont(Font.font("System", FontWeight.BOLD, 32));
        iconLabel.setStyle("-fx-text-fill: #2C3E50;");
        iconLabel.setId("cardIcon");

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        nameLabel.setStyle("-fx-text-fill: #2C3E50;");
        nameLabel.setId("cardName");

        card.getChildren().addAll(iconLabel, nameLabel);

        // ---- Click event - navigates to booking ----
        card.setOnMouseClicked(e -> showBooking());

        return card;
    }

    /**
     * Creates the real booking views from Member 4.
     * These screens do NOT have their own navbars.
     * They rely on MainView's shared navbar.
     */
    private void buildBookingViews() {
        // ---- Create BookingView ----
        bookingView = new BookingView();
        // No navigation callbacks needed - uses MainView's shared navbar

        // ---- Create ViewBookingView ----
        viewBookingView = new ViewBookingView();
        // No navigation callbacks needed - uses MainView's shared navbar
    }

    private void buildPlaceholders() {
        availabilityPlaceholder = createPlaceholder("Resource Availability");
        policyPlaceholder = createPlaceholder("Policy Assistant");
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
    // SECTION 13: NAVIGATION METHODS
    // ================================================================

    /**
     * Shows the login screen (FR1).
     * Navbar buttons are hidden.
     */
    public void showLogin() {
        setCenter(loginView);
        loginView.onShow();
        hideAllNavButtons();
        userInfoLabel.setText("");
    }

    /**
     * Shows the home screen with resource cards.
     * Navbar buttons are shown after successful login.
     */
    public void showHome() {
        setCenter(homeContent);
        showAllNavButtons();
        updateMCPStatus(mcp != null);

        // Pass student ID to booking view if it exists
        if (bookingView != null) {
            bookingView.setStudentId(currentStudentId);
        }
    }

    /**
     * Shows the Resource Availability screen (FR2).
     * Currently a placeholder - to be replaced by Member 4.
     */
    public void showAvailability() {
        setCenter(availabilityPlaceholder);
    }

    /**
     * Shows the Resource Booking screen (FR3).
     * Uses the real BookingView from Member 4.
     */
    public void showBooking() {
        if (bookingView != null) {
            setCenter(bookingView);
            bookingView.onShow();
            bookingView.setStudentId(currentStudentId);
        } else {
            setCenter(createPlaceholder("Resource Booking"));
        }
    }

    /**
     * Shows the Booking History screen (FR4, FR5).
     * Uses the real ViewBookingView from Member 4.
     */
    public void showHistory() {
        if (viewBookingView != null) {
            setCenter(viewBookingView);
            viewBookingView.onShow();
        } else {
            setCenter(createPlaceholder("Booking History"));
        }
    }

    /**
     * Shows the Policy Assistant screen (FR6).
     * Currently a placeholder - to be replaced by Member 5.
     */
    public void showPolicy() {
        setCenter(policyPlaceholder);
    }

    // ================================================================
    // SECTION 14: LOGOUT (FR10)
    // ================================================================

    /**
     * Handles logout with confirmation dialog.
     * Clears session and returns to login screen.
     */
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

    // ================================================================
    // SECTION 15: HELPER METHODS
    // ================================================================

    /**
     * Shows all navigation buttons (called after successful login).
     */
    public void showAllNavButtons() {
        homeBtn.setVisible(true);
        bookingBtn.setVisible(true);
        historyBtn.setVisible(true);
        policyBtn.setVisible(true);
        logoutBtn.setVisible(true);
    }

    /**
     * Hides all navigation buttons (called before login).
     */
    public void hideAllNavButtons() {
        homeBtn.setVisible(false);
        bookingBtn.setVisible(false);
        historyBtn.setVisible(false);
        policyBtn.setVisible(false);
        logoutBtn.setVisible(false);
    }

    /**
     * Sets the user info label (shows logged-in student ID).
     */
    public void setStatusMessage(String message) {
        userInfoLabel.setText(message);
    }

    /**
     * Updates the MCP connection status indicator (NFR4).
     */
    public void updateMCPStatus(boolean connected) {
        Platform.runLater(() -> {
            mcpStatusIndicator.setFill(connected ? Color.GREEN : Color.RED);
            mcpStatusLabel.setText(connected ? "MCP Connected" : "MCP Disconnected");
        });
    }

    /**
     * Gets the LoginView (for testing).
     */
    public LoginView getLoginView() {
        return loginView;
    }

    /**
     * Gets the background worker thread (for other controllers).
     */
    public ExecutorService getWorker() {
        return worker;
    }

    // ================================================================
    // SECTION 16: MCP BINDING (FROM REFERENCE)
    // ================================================================

    /**
     * Binds the MCP client and RAG service to this view.
     * Called by App.java after successful connection.
     */
    public void bind(CampusMcpClient mcp, RagService rag) {
        this.mcp = mcp;
        this.rag = rag;
        updateMCPStatus(mcp != null);
        if (rag == null) {
            System.out.println("RAG service not available (API key missing?)");
        }
    }

    /**
     * Gets the MCP client (for other controllers).
     */
    public CampusMcpClient getMcp() {
        return mcp;
    }

    /**
     * Gets the RAG service (for other controllers).
     */
    public RagService getRag() {
        return rag;
    }

    // ================================================================
    // SECTION 17: REFRESH DISCOVERY (FROM REFERENCE - For debugging)
    // ================================================================

    /**
     * Populates the discovery information.
     * Useful for debugging MCP connection.
     */
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
    // SECTION 18: getRoot() - FOR App.java
    // ================================================================

    /**
     * Returns the root node for this view.
     * Since MainView extends BorderPane, we return 'this'.
     * Called by App.java via view.getRoot().
     */
    public BorderPane getRoot() {
        return this;
    }
}