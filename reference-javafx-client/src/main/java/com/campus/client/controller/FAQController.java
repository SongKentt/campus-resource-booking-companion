package com.campus.client.controller;

import com.campus.client.rag.RagService;
import com.campus.client.ui.FAQView;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class FAQController {

    private final RagService ragService;
    private final FAQView view;


    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "rag-worker");
        t.setDaemon(true);
        return t;
    });


    public FAQController(RagService ragService, FAQView view) {
        this.ragService = ragService;
        this.view = view;
    }


    public void handleAskQuestion(String question) {

        // basic validation - empty question? just tell the user and stop
        if (question == null || question.isBlank()) {
            view.showError("Please enter a question before submitting.");
            return;
        }

        // show the loading spinner and disable the input while we process
        Platform.runLater(() -> {
            view.setInputEnabled(false);
            view.showLoadingIndicator(question);
        });

        // run the actual RAG stuff on a background thread so the UI doesnt freeze
        worker.submit(() -> {
            try {
                RagService.RagResult result = ragService.ask(question,"general");

                String answer = result.answer();
                String context = result.retrievedContext();
                List<String> sources = extractSources(context);

                // back on the UI thread to show the answer
                Platform.runLater(() -> {
                    view.displayResponse(answer, context, sources);
                    view.setInputEnabled(true);
                });

            } catch (Exception e) {
                // if something goes wrong and show an error message in the chat
                Platform.runLater(() -> {
                    view.showChatError("Unable to generate an answer. Please check the MCP server, API key, and network connection, then try again.");
                    view.setInputEnabled(true);
                });
            }
        });
    }

    // this extracts the source references from the context text
    private List<String> extractSources(String context){
        List<String> sources = new ArrayList<>();
        if(context == null || context.isEmpty()){
            return sources;
        }

        String[] lines = context.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("[") && line.contains("]")) {
                String source = line.substring(1, line.indexOf("]")).trim();
                if (!source.isEmpty() && !sources.contains(source)) {
                    sources.add(source);
                }
            }
        }
        return sources;
    }


}