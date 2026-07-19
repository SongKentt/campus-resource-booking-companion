package com.campus.client.ui;

import com.campus.client.controller.FAQController;
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

import java.util.List;


public class FAQView extends BaseView {

    private FAQController controller;

    // all the ui bits and pieces for the chat screen
    private final BorderPane root = new BorderPane();

    private final VBox chatBox = new VBox(12);           // holds all chat messages
    private final ScrollPane chatScroll = new ScrollPane(chatBox);

    private final VBox sourcesPanel = new VBox(8);       // shows where the answer came from
    private final ScrollPane sourcesScroll = new ScrollPane(sourcesPanel);

    private final TextField questionField = new TextField();  // where user types their question

    private final Button sendButton = new Button("Send");

    private final ProgressIndicator loadingIndicator = new ProgressIndicator(); // spinning wheel while waiting for answer


    public FAQView() {
        buildLayout();
        wireEvents();
    }

    public void setController(FAQController controller) {
        this.controller = controller;
    }


    public BorderPane getRoot() {
        return root;
    }

    // this whole method just adds stuff to the screen  its long but its just UI setup
    private void buildLayout() {
        root.setPadding(new Insets(0));
        root.setStyle("-fx-background-color: #f5f5f5;");

        // the blue bar at the top with the title
        Label titleLabel = new Label("Ask anything about campus booking rules and policies");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setPadding(new Insets(14, 20, 14, 20));

        HBox titleBar = new HBox(titleLabel);
        titleBar.setStyle("-fx-background-color: #1F3864;");
        titleBar.setAlignment(Pos.CENTER_LEFT);
        root.setTop(titleBar);

        // the sources panel to shows what documents/knowledge the AI used to answer
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

        // the chat area where messages appear
        chatBox.setPadding(new Insets(14));
        chatBox.setFillWidth(true);
        chatBox.setStyle("-fx-background-color: #FFFFFF;");

        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background: #FFFFFF; -fx-background-color: #FFFFFF;");
        HBox.setHgrow(chatScroll, Priority.ALWAYS);

        loadingIndicator.setMaxSize(30, 30);
        loadingIndicator.setVisible(false);

        // main area split into sources which is panel left and chat right
        HBox mainArea = new HBox(sourcesScroll, chatScroll);
        HBox.setHgrow(chatScroll, Priority.ALWAYS);
        mainArea.setStyle("-fx-background-color: #f5f5f5;");
        root.setCenter(mainArea);

        // the bottom bar where user types and clicks send
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

    // connects the send button and enter key so they both submit the question
    private void wireEvents() {
        sendButton.setOnAction(e -> submitQuestion());
        questionField.setOnAction(e -> submitQuestion());
    }

    // takes whatever is in the text field and sends it to the controller
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

    // shows the user's question in the chat and the loading spinner basically tells the user to wait for the AI to respond

    public void showLoadingIndicator(String question) {
        // blue bubble on the right side - this is the user's question
        Label questionBubble = new Label(question);
        questionBubble.setWrapText(true);
        questionBubble.setMaxWidth(400);
        questionBubble.setFont(Font.font("Arial", 13));
        questionBubble.setStyle("-fx-background-color: #2E75B6; -fx-text-fill: white; "
                + "-fx-padding: 8 12 8 12; -fx-background-radius: 10 10 0 10;");

        HBox questionRow = new HBox(questionBubble);
        questionRow.setAlignment(Pos.CENTER_RIGHT);
        chatBox.getChildren().add(questionRow);

        // placeholder text while waiting for the actual answer
        Label generatingLabel = new Label("Generating answer...");
        generatingLabel.setFont(Font.font("Arial", 13));
        generatingLabel.setTextFill(Color.GRAY);
        generatingLabel.setId("loading-label");
        chatBox.getChildren().add(generatingLabel);

        // show the spinning wheel in the input bar
        loadingIndicator.setVisible(true);

        // scroll down so user can see their question
        scrollToBottom();
    }

    // this is where the answer actually shows up
    public void displayResponse(String answer, String context, List<String> sources) {

        chatBox.getChildren().removeIf(node ->
                node instanceof Label lbl && "loading-label".equals(lbl.getId()));

        // show "Assistant" label
        Label assistantLabel = new Label("Assistant");
        assistantLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        assistantLabel.setTextFill(Color.web("#1F3864"));

        // the actual answer on the left side
        Label answerBubble = new Label(answer);
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

        // update the sources panel
        if (sourcesPanel.getChildren().size() > 1) {
            sourcesPanel.getChildren().remove(1, sourcesPanel.getChildren().size());
        }

        if (sources != null && !sources.isEmpty()) {
            for (String source : sources) {
                Label sourceLabel = new Label("📄 " + source);
                sourceLabel.setWrapText(true);
                sourceLabel.setFont(Font.font("Arial", 12));
                sourceLabel.setTextFill(Color.web("#444444"));
                sourcesPanel.getChildren().add(sourceLabel);
            }
        }

        // show the full context text too
        if (context != null && !context.isBlank()) {
            TextArea contextArea = new TextArea(context);
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

    // shows an error message in the chat when something goes wrong
    public void showChatError(String message) {
        // remove the loading placeholder if its still there
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

    // disables the input fields while waiting for an answer so the user cant spam
    public void setInputEnabled(boolean enabled) {
        sendButton.setDisable(!enabled);
        questionField.setDisable(!enabled);
        loadingIndicator.setVisible(!enabled);
    }

    // scrolls the chat to the bottom so the latest message is always visible
    private void scrollToBottom() {
        chatScroll.layout();
        chatScroll.setVvalue(1.0);
    }

    // BaseView methods
    @Override
    public void showError(String message) {
        showChatError(message);
    }

    // shows success messages
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
    }

    @Override
    public void onHide() {
    }
}