package com.campus.client.ui;

import com.campus.client.controller.FAQController;
import com.campus.client.rag.RagResponse;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * JavaFX view for the RAG Policy Assistant screen.
 * Displays the chat area, retrieved sources, and question input.
 */
public class FAQView extends BaseView {

    private FAQController controller;

    // UI Components

    private final BorderPane root = new BorderPane();

    private final VBox chatBox = new VBox(12);
    private final ScrollPane chatScroll = new ScrollPane(chatBox);

    private final VBox sourcesPanel = new VBox(8);
    private final ScrollPane sourcesScroll = new ScrollPane(sourcesPanel);

    private final TextField questionField = new TextField();

    private final Button sendButton = new Button("Send");

    private final ProgressIndicator loadingIndicator = new ProgressIndicator();

    /**
     * Creates the FAQ screen and sets up its events.
     */
    public FAQView() {
        buildLayout();
        wireEvents();
    }

    /**
     * Sets the controller that handles question submissions.
     *
     * @param controller controller for this view
     */
    public void setController(FAQController controller) {
        this.controller = controller;
    }

    /**
     * Returns the root layout of this screen.
     *
     * @return root BorderPane
     */
    public BorderPane getRoot() {
        return root;
    }

    // Layout Construction

    /**
     * Builds the Policy Assistant screen layout.
     */
    private void buildLayout() {
        root.setPadding(new Insets(0));
        root.setStyle("-fx-background-color: #f5f5f5;");

        // Title bar
        Label titleLabel = new Label("Ask anything about campus booking rules and policies");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setPadding(new Insets(14, 20, 14, 20));

        HBox titleBar = new HBox(titleLabel);
        titleBar.setStyle("-fx-background-color: #1F3864;");
        titleBar.setAlignment(Pos.CENTER_LEFT);
        root.setTop(titleBar);

        // Sources panel
        Label sourcesTitle = new Label("Sources from knowledge base");
        sourcesTitle.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        sourcesTitle.setTextFill(Color.web("#2E75B6"));
        sourcesTitle.setPadding(new Insets(0, 0, 8, 0));

        sourcesPanel.setPadding(new Insets(14));
        sourcesPanel.getChildren().add(sourcesTitle);
        sourcesPanel.setStyle("-fx-background-color: #EBF3FB;");

        sourcesScroll.setFitToWidth(true);
        sourcesScroll.setPrefWidth(280);
        sourcesScroll.setStyle("-fx-background: #EBF3FB; -fx-background-color: #EBF3FB;");

        // Chat panel
        chatBox.setPadding(new Insets(14));
        chatBox.setFillWidth(true);
        chatBox.setStyle("-fx-background-color: #FFFFFF;");

        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background: #FFFFFF; -fx-background-color: #FFFFFF;");
        HBox.setHgrow(chatScroll, Priority.ALWAYS);

        // Loading indicator
        loadingIndicator.setMaxSize(30, 30);
        loadingIndicator.setVisible(false);

        HBox mainArea = new HBox(sourcesScroll, chatScroll);
        HBox.setHgrow(chatScroll, Priority.ALWAYS);
        mainArea.setStyle("-fx-background-color: #f5f5f5;");
        root.setCenter(mainArea);

        // Input bar
        questionField.setPromptText("Type your question about campus booking policies...");
        questionField.setFont(Font.font("Arial", 13));
        questionField.setPrefHeight(38);
        HBox.setHgrow(questionField, Priority.ALWAYS);

        sendButton.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        sendButton.setPrefHeight(38);
        sendButton.setStyle("-fx-background-color: #2E75B6; -fx-text-fill: white; "
                + "-fx-background-radius: 4;");

        HBox inputBar = new HBox(10, questionField, loadingIndicator, sendButton);
        inputBar.setPadding(new Insets(10, 14, 10, 14));
        inputBar.setAlignment(Pos.CENTER);
        inputBar.setStyle("-fx-background-color: #FFFFFF; "
                + "-fx-border-color: #DDDDDD; -fx-border-width: 1 0 0 0;");
        root.setBottom(inputBar);
    }

    /**
     * Connects the Send button and Enter key to question submission
     */
    private void wireEvents() {
        sendButton.setOnAction(e -> submitQuestion());
        questionField.setOnAction(e -> submitQuestion());
    }

    /**
     * Sends the entered question to the controller
     */
    private void submitQuestion() {
        if (controller == null) {
            showError("FAQ controller is not connected.");
            return;
        }

        String question = questionField.getText().trim();
        controller.handleAskQuestion(question);

        if (!question.isBlank()) {
            questionField.clear();
        }
    }

    // UI Update Methods

    /**
     * Shows the submitted question and loading state.
     *
     * @param question submitted question
     */
    public void showLoadingIndicator(String question) {
        // Display student question bubble on the right side of the chat
        Label questionBubble = new Label(question);
        questionBubble.setWrapText(true);
        questionBubble.setMaxWidth(400);
        questionBubble.setFont(Font.font("Arial", 13));
        questionBubble.setStyle("-fx-background-color: #2E75B6; -fx-text-fill: white; "
                + "-fx-padding: 8 12 8 12; -fx-background-radius: 10 10 0 10;");

        HBox questionRow = new HBox(questionBubble);
        questionRow.setAlignment(Pos.CENTER_RIGHT);
        chatBox.getChildren().add(questionRow);

        // Show "Generating answer..." placeholder
        Label generatingLabel = new Label("Generating answer...");
        generatingLabel.setFont(Font.font("Arial", 13));
        generatingLabel.setTextFill(Color.GRAY);
        generatingLabel.setId("loading-label");
        chatBox.getChildren().add(generatingLabel);

        // Show loading indicator in the input bar
        loadingIndicator.setVisible(true);

        // Scroll chat to bottom
        scrollToBottom();
    }


    /**
     *  Displays the generated answer and retrieved sources.
     *
     * @param response completed RAG response
     */
    public void displayResponse(RagResponse response) {
        chatBox.getChildren().removeIf(node ->
                node instanceof Label lbl && "loading-label".equals(lbl.getId()));

        // ── Display LLM answer in chat
        Label assistantLabel = new Label("Assistant");
        assistantLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        assistantLabel.setTextFill(Color.web("#1F3864"));

        Label answerBubble = new Label(response.getAnswer());
        answerBubble.setWrapText(true);
        answerBubble.setMaxWidth(450);
        answerBubble.setFont(Font.font("Arial", 13));
        answerBubble.setStyle("-fx-background-color: #F0F0F0; -fx-text-fill: #1A1A1A; "
                + "-fx-padding: 8 12 8 12; -fx-background-radius: 10 10 10 0;");

        VBox assistantBlock = new VBox(4, assistantLabel, answerBubble);
        assistantBlock.setAlignment(Pos.CENTER_LEFT);

        HBox answerRow = new HBox(assistantBlock);
        answerRow.setAlignment(Pos.CENTER_LEFT);
        chatBox.getChildren().add(answerRow);

        // Updates the sources panel using the retrieved RAG context.
        if (sourcesPanel.getChildren().size() > 1) {
            sourcesPanel.getChildren().remove(1, sourcesPanel.getChildren().size());
        }

        if (response.getSources() != null && !response.getSources().isEmpty()) {
            for (String source : response.getSources()) {
                Label sourceLabel = new Label("📄 " + source);
                sourceLabel.setWrapText(true);
                sourceLabel.setFont(Font.font("Arial", 12));
                sourceLabel.setTextFill(Color.web("#444444"));
                sourcesPanel.getChildren().add(sourceLabel);
            }
        }

        // Display full retrieved context text in sources panel
        if (response.getContextUsed() != null && !response.getContextUsed().isBlank()) {
            TextArea contextArea = new TextArea(response.getContextUsed());
            contextArea.setEditable(false);
            contextArea.setWrapText(true);
            contextArea.setPrefRowCount(10);
            contextArea.setFont(Font.font("Arial", 11));
            contextArea.setStyle("-fx-background-color: transparent; -fx-border-color: #CCCCCC; "
                    + "-fx-background-radius: 4; -fx-border-radius: 4;");
            sourcesPanel.getChildren().add(contextArea);
        }

        loadingIndicator.setVisible(false);
        scrollToBottom();
    }

    /**
     * Shows an error message in the chat area.
     *
     * @param message the error message to display
     */
    public void showChatError(String message) {
        // Remove the loading placeholder if present
        chatBox.getChildren().removeIf(node ->
                node instanceof Label lbl && "loading-label".equals(lbl.getId()));

        Label errorLabel = new Label(message);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(400);
        errorLabel.setFont(Font.font("Arial", 13));
        errorLabel.setStyle("-fx-background-color: #FDECEA; -fx-text-fill: #C0392B; "
                + "-fx-padding: 8 12 8 12; -fx-background-radius: 6; "
                + "-fx-border-color: #E74C3C; -fx-border-width: 1; -fx-border-radius: 6;");

        HBox errorRow = new HBox(errorLabel);
        errorRow.setAlignment(Pos.CENTER_LEFT);
        chatBox.getChildren().add(errorRow);

        loadingIndicator.setVisible(false);
        scrollToBottom();
    }

    /**
     * Enables or disables question input while processing.
     *
     * @param enabled true to enable input, false to disable it
     */
    public void setInputEnabled(boolean enabled) {
        sendButton.setDisable(!enabled);
        questionField.setDisable(!enabled);
        loadingIndicator.setVisible(!enabled);
    }

    /**
     * Scrolls the chat display to the bottom so the latest message is always visible.
     */
    private void scrollToBottom() {
        chatScroll.layout();
        chatScroll.setVvalue(1.0);
    }

    // ── BaseView abstract method implementations ───────────────────────────────────────

    /**
     * Displays a validation or system error message as an inline alert above the input bar.
     * Inherited from {@link BaseView} and used for empty query validation (FR7).
     *
     * @param message the error message to display
     */
    @Override
    public void showError(String message) {
        showChatError(message);
    }

    /**
     * Displays a success notification in the chat area.
     * Inherited from {@link BaseView}.
     *
     * @param message the success message to display
     */
    @Override
    public void showSuccess(String message) {
        Label successLabel = new Label(message);
        successLabel.setFont(Font.font("Arial", 13));
        successLabel.setStyle("-fx-background-color: #E8F8E8; -fx-text-fill: #27AE60; "
                + "-fx-padding: 8 12 8 12; -fx-background-radius: 6; "
                + "-fx-border-color: #2ECC71; -fx-border-width: 1; -fx-border-radius: 6;");

        HBox successRow = new HBox(successLabel);
        successRow.setAlignment(Pos.CENTER_LEFT);
        chatBox.getChildren().add(successRow);
        scrollToBottom();
    }

    @Override
    public void onShow() {
        // Nothing needed when this view is shown
    }

    @Override
    public void onHide() {
        // Nothing needed when this view is hidden
    }
}
