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

/**
 * JavaFX host for the Campus Resource Booking Companion.
 * Resolves configuration, connects to the Campus MCP server
 * over HTTP/SSE on a background thread, builds the RAG stack, and hands everything to {@link MainView}.
 *
 * <p>Configuration (all overridable):
 * <ul>
 *   <li>{@code -Dmcp.server.url} or env {@code MCP_SERVER_URL} (default http://localhost:8080)</li>
 *   <li>env {@code ANTHROPIC_API_KEY} (required for the RAG tab)</li>
 *   <li>{@code -Danthropic.model} (default claude-sonnet-4-6)</li>
 * </ul>
 */
public final class App extends Application {

    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static final String DEFAULT_URL = "http://localhost:8080";

    private static final String DEFAULT_ANTHROPIC_MODEL = "claude-sonnet-4-6";

    /** Default is anthropic. Change this to suit your need - anthropic, gemini, openai, google
     Remember to set the <PROVIDER>_API_KEY value in your environment variable.
     Warning: DO NOT store API_Keys in your source code!
     **/
    private static final String DEFAULT_PROVIDER = "anthropic";

    // ===== ADD THIS: Static instance for getMainView() =====
    private static App instance;

    private CampusMcpClient mcp;
    private MainView view;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // ===== ADD THIS: Store instance reference =====
        instance = this;

        view = new MainView();
        stage.setTitle("Campus Resource Booking Companion");
        // ===== FIX: MainView extends BorderPane, use it directly =====
        stage.setScene(new Scene(view, 1100, 750));
        stage.show();

        Thread t = new Thread(this::bootstrap, "mcp-bootstrap");
        t.setDaemon(true);
        t.start();
    }

    private void bootstrap() {
        try {
            String url = firstNonBlank(System.getProperty("mcp.server.url"),
                    System.getenv("MCP_SERVER_URL"), DEFAULT_URL);
            Platform.runLater(() -> {
                // ===== FIX: Use setStatusMessage() instead of setStatus() =====
                view.setStatusMessage("Connecting to MCP server at " + url + " …");
                System.out.println("Connecting to MCP server at " + url);
            });

            mcp = new CampusMcpClient(url);
            var init = mcp.connect();

            // Added multiple LLM models
            LlmClient llm = buildLlmClient();
            RagService rag = (llm == null) ? null : new RagService(mcp, llm);
            String llmNote = (llm == null)
                    ? "LLM: disabled (set <PROVIDER>_API_KEY to enable the RAG tab)"
                    : "LLM: " + llm.model();

            final RagService ragFinal = rag;

            Platform.runLater(() -> {
                view.bind(mcp, ragFinal);
                // ===== FIX: Use setStatusMessage() instead of setStatus() =====
                view.setStatusMessage("Connected to '" + init.serverInfo().name() + "'.  " + llmNote);
                view.refreshDiscovery();
            });
        } catch (Exception e) {
            log.error("Bootstrap failed", e);
            Platform.runLater(() -> {
                // ===== FIX: Use setStatusMessage() instead of setStatus() =====
                view.setStatusMessage("Connection failed: " + e.getMessage()
                        + "  (Is the server running?)");
            });
        }
    }

    /**
     * Builds an LLM client based on the {@code LLM_PROVIDER} setting and the appropriate API-key
     * environment variable. Returns {@code null} (RAG tab disabled) if no key is configured.
     */
    private LlmClient buildLlmClient() {
        String provider = firstNonBlank(System.getProperty("llm.provider"),
                System.getenv("LLM_PROVIDER"), DEFAULT_PROVIDER);

        // ===== FIX: Handle null provider safely =====
        if (provider == null) {
            log.warn("No LLM provider specified; RAG tab disabled.");
            return null;
        }

        provider = provider.toLowerCase();
        int maxTokens = 1024;

        switch (provider) {
            case "anthropic" -> {
                String key = firstNonBlank(System.getProperty("anthropic.apiKey"),
                        System.getenv("ANTHROPIC_API_KEY"), null);
                if (key == null) return null;
                String model = firstNonBlank(System.getProperty("anthropic.model"), DEFAULT_ANTHROPIC_MODEL);
                return new AnthropicClient(key, model, maxTokens);
            }

            default -> {
                log.warn("Unknown LLM_PROVIDER '{}'; RAG tab disabled.", provider);
                return null;
            }
        }
    }

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
        if (mcp != null) {
            mcp.close();
        }
        Platform.exit();
    }
}