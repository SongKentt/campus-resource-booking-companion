package com.campus.client.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sends prompts to the Anthropic API and returns the generated answer
 * This class is used by RagService during the generation step of RAG
 */
public final class AnthropicClient implements LlmClient {

    /** Anthropic Messages API endpoint. */
    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";

    /** Anthropic API version used for the request */
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    private final String apiKey;
    private final String model;
    private final int maxTokens;

    /**
     * Creates an Anthropic client with the required API credentials and model settings
     * @param apiKey    API key for Anthropic
     * @param model     Name of the model to use
     * @param maxTokens maximum number of tokens for the response
     */
    public AnthropicClient(String apiKey, String model, int maxTokens) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Anthropic API key is missing. Please set the ANTHROPIC_API_KEY environment variable.");
        }

        if (model == null || model.isBlank()){
            throw new IllegalArgumentException(
                    "Anthropic model name must not be empty.");
        }

        if (maxTokens <= 0) {
            throw new IllegalArgumentException(
                    "Maximum tokens must be greater than 0.");
        }

        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    /**
     * Sends an augmented prompt to the Anthropic Messages API and returns the generated text.
     *
     * <p>This method waits for the API response, so it should be called from a background thread to avoid freezing the JavaFX UI.</p>
     *
     * @param systemPrompt framing instructions from the campus_assistant MCP prompt
     * @param userPrompt   augmented prompt containing retrieved context and the student's question
     * @return the model's generated text answer
     * @throws IOException          if the HTTP request fails or returns a non-2xx status code
     * @throws InterruptedException if the thread is interrupted while waiting for the response
     */

    @Override
    public String complete(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        // Build the JSON request body for the Anthropic Messages API.
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", maxTokens);

        // Include the system prompt if it is provided
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.put("system", systemPrompt);
        }

        // Add user prompt to the message array
        ArrayNode messages = mapper.createArrayNode();
        ObjectNode userMsg = mapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);
        body.set("messages", messages);

        // Build and send the HTTP POST request to the Anthropic API.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(120))
                .header("content-type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        //Handles unsuccessful API responses
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Anthropic API error " + response.statusCode());
        }

        // Read the content array from the API response.
        JsonNode content = mapper.readTree(response.body()).path("content");

        if (!content.isArray()) {
            throw new IOException(
                    "Invalid Anthropic response: missing content array.");
        }

        // Extract all text blocks from the response.
        StringBuilder out = new StringBuilder();

        for (JsonNode block : content) {
            if ("text".equals(block.path("type").asText())) {
                out.append(block.path("text").asText());
            }
        }

        if (out.isEmpty()) {
            throw new IOException(
                    "Anthropic response did not contain any text.");
        }

        return out.toString();
    }

    @Override
    public String model() {
        return model;
    }
}