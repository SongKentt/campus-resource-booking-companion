package com.campus.client.controller;

import com.campus.client.rag.RagResponse;
import com.campus.client.rag.RagService;
import com.campus.client.ui.FAQView;
import javafx.application.Platform;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller for the Policy Assistant (FAQ) screen.
 *
 * <p>This class receives the student's question from FAQView, calls RagService
 *  * in a background thread, and sends the result back to the view for display.</p>
 */
public class FAQController {

    private final RagService ragService;
    private final FAQView view;

    /**
     * Background worker used for MCP and LLM calls.
     * These calls should not run on the JavaFX Application Thread.
     */
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "rag-worker");
        t.setDaemon(true);
        return t;
    });

    /**
     * Creates a controller for the FAQ / Policy Assistant screen
     *
     * @param ragService service that runs the RAG pipeline
     * @param view       FAQ view controlled by this controller
     */
    public FAQController(RagService ragService, FAQView view) {
        this.ragService = ragService;
        this.view = view;
    }

    /**
     * Handles the send button action from FAQView
     *
     * <p>The method validates the question, disables the input while processing,
     * runs the RAG process in the background, and updates the UI after the result
     * is ready.</p>
     *
     * @param question question entered by the student in FAQView
     */
    public void handleAskQuestion(String question) {

        // Validate input before running the RAG process
        if (question == null || question.isBlank()) {
            view.showError("Please enter a question before submitting.");
            return;
        }

        // Show loading state before starting the background task.
        Platform.runLater(() -> {
            view.setInputEnabled(false);
            view.showLoadingIndicator(question);
        });

        // Run MCP retrieval and LLM generation on a background thread.
        worker.submit(() -> {
            try {
                RagResponse response = ragService.answerQuestion(question);

                // Success: update UI with context and answer
                Platform.runLater(() -> {
                    view.displayResponse(response);
                    view.setInputEnabled(true);
                });

            } catch (Exception e) {
                // Failure: show error message in chat area
                Platform.runLater(() -> {
                    view.showChatError("Unable to generate an answer. Please check the MCP server, API key, and network connection, then try again.");
                    view.setInputEnabled(true);
                });
            }
        });
    }

    /**
     *Stops the background worker when the application is closing
     */
    public void shutdown() {
        worker.shutdownNow();
    }
}
