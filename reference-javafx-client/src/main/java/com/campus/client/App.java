package com.campus.client;

import com.campus.client.llm.AnthropicClient;
import com.campus.client.llm.LlmClient;
import com.campus.client.mcp.CampusMcpClient;
import com.campus.client.rag.RagService;
import com.campus.client.ui.MainView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class App extends Application {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    // default server url
    private static final String DEFAULT_URL = "http://localhost:8080";

    // using claude sonnet for ai assist
    private static final String DEFAULT_ANTHROPIC_MODEL = "claude-sonnet-4-6";

    // we're using anthropic as the default llm provider
    private static final String DEFAULT_PROVIDER = "anthropic";

    private CampusMcpClient mcp;  // talks to the mcp server
    private MainView view;        // main ui

    // standard javafx entry point
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // create the ui first
        view = new MainView();

        // set up the window
        stage.setTitle("Campus Resource Booking Companion");
        stage.setScene(new Scene(view, 1100, 750));
        stage.show();

        // start connecting in background so ui stays responsive
        Thread t = new Thread(this::bootstrap, "mcp-bootstrap");
        t.setDaemon(true);
        t.start();
    }

    // does all the setup work , connects to server, sets up llm, binds everything
    private void bootstrap() {
        try {
            // get server url from system property or env var or use default
            String url = firstNonBlank(System.getProperty("mcp.server.url"),
                    System.getenv("MCP_SERVER_URL"), DEFAULT_URL);

            // tell the user we connecting
            Platform.runLater(() -> {
                view.setStatusMessage("Connecting to MCP server at " + url + " …");
                System.out.println("Connecting to MCP server at " + url);
            });

            // connect to the mcp server
            mcp = new CampusMcpClient(url);
            var init = mcp.connect();

            // try to setup the llm for the ai assistant
            LlmClient llm = buildLlmClient();

            // if llm exists, create rag service, otherwise leave it null
            RagService rag = (llm == null) ? null : new RagService(mcp, llm);

            // status message about llm
            String llmNote = (llm == null)
                    ? "LLM: disabled (set <PROVIDER>_API_KEY to enable the RAG tab)"
                    : "LLM: " + llm.model();

            final RagService ragFinal = rag;

            // update the ui with everything we set up
            Platform.runLater(() -> {
                view.bind(mcp, ragFinal);
                view.setStatusMessage("Connected to '" + init.serverInfo().name() + "'.  " + llmNote);
                view.refreshDiscovery();
            });

        } catch (Exception e) {
            // something went wrong, show error in ui
            log.error("Bootstrap failed", e);
            Platform.runLater(() -> {
                view.setStatusMessage("Connection failed: " + e.getMessage()
                        + "  (Is the server running?)");
            });
        }
    }

    // sets up the llm client based on which provider we're using
    private LlmClient buildLlmClient() {
        // check which provider to use - from system property or env var
        String provider = firstNonBlank(System.getProperty("llm.provider"),
                System.getenv("LLM_PROVIDER"), DEFAULT_PROVIDER);

        if (provider == null) {
            log.warn("No LLM provider specified; RAG tab disabled.");
            return null;
        }

        provider = provider.toLowerCase();
        int maxTokens = 1024;

        switch (provider) {
            case "anthropic" -> {
                // try to get the api key
                String key = firstNonBlank(System.getProperty("anthropic.apiKey"),
                        System.getenv("ANTHROPIC_API_KEY"), null);

                // no key means no llm
                if (key == null) return null;

                // use default model or what the user specified
                String model = firstNonBlank(System.getProperty("anthropic.model"), DEFAULT_ANTHROPIC_MODEL);
                return new AnthropicClient(key, model, maxTokens);
            }

            default -> {
                log.warn("Unknown LLM_PROVIDER '{}'; RAG tab disabled.", provider);
                return null;
            }
        }
    }

    // helper that returns the first non-empty string from a list of values
    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    @Override
    public void stop() {
        // cleanup when app closes
        if (mcp != null) {
            mcp.close();
        }
        Platform.exit();
    }
}