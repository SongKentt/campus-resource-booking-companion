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
 * Google Gemini client (Generative Language API). Same shape as {@link AnthropicClient}.
 *
 * <p>Endpoint: {@code POST https://generativelanguage.googleapis.com/v1beta/models/<model>:generateContent}.<br>
 * Auth: {@code x-goog-api-key: <apiKey>} header (the key can also be passed as a query parameter,
 * but the header keeps it out of URLs / logs).<br>
 * Request shape (simplified):</p>
 * <pre>{@code
 * {
 *   "system_instruction": { "parts": [ { "text": "..." } ] },
 *   "contents": [
 *     { "role": "user", "parts": [ { "text": "..." } ] }
 *   ],
 *   "generationConfig": { "maxOutputTokens": 1024 }
 * }
 * }</pre>
 *
 * <p>Set the API key in {@code GEMINI_API_KEY} (or {@code GOOGLE_API_KEY}) in the environment.
 * Model names change over time; confirm the current options in Google's documentation before
 * submitting.</p>
 */
public final class GeminiClient implements LlmClient {

    private static final String ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public GeminiClient(String apiKey, String model, int maxTokens) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Missing Gemini API key (set GEMINI_API_KEY).");
        }
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        ObjectNode body = mapper.createObjectNode();

        // Optional system_instruction (supported on Gemini 1.5+ / 2.x models).
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            ObjectNode sysInstr = mapper.createObjectNode();
            ArrayNode sysParts = mapper.createArrayNode();
            sysParts.add(mapper.createObjectNode().put("text", systemPrompt));
            sysInstr.set("parts", sysParts);
            body.set("system_instruction", sysInstr);
        }

        // contents: [ { role: user, parts: [ { text: ... } ] } ]
        ArrayNode contents = mapper.createArrayNode();
        ObjectNode userContent = mapper.createObjectNode();
        userContent.put("role", "user");
        ArrayNode userParts = mapper.createArrayNode();
        userParts.add(mapper.createObjectNode().put("text", userPrompt));
        userContent.set("parts", userParts);
        contents.add(userContent);
        body.set("contents", contents);

        // generationConfig.maxOutputTokens
        ObjectNode genConfig = mapper.createObjectNode();
        genConfig.put("maxOutputTokens", maxTokens);
        body.set("generationConfig", genConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(ENDPOINT_TEMPLATE, model)))
                .timeout(Duration.ofSeconds(120))
                .header("content-type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Gemini API error " + response.statusCode() + ": " + response.body());
        }

        // Response shape:  { "candidates": [ { "content": { "parts": [ { "text": "..." }, ... ] } } ] }
        JsonNode root = mapper.readTree(response.body());
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            // No candidates can mean a safety block, an empty completion, or a prompt feedback case.
            String feedback = root.path("promptFeedback").toString();
            throw new IOException("Gemini API returned no candidates. " +
                    (feedback.isEmpty() ? response.body() : "promptFeedback=" + feedback));
        }

        // Concatenate all text parts from the first candidate.
        StringBuilder out = new StringBuilder();
        for (JsonNode part : candidates.get(0).path("content").path("parts")) {
            if (part.has("text")) {
                out.append(part.path("text").asText());
            }
        }
        return out.toString();
    }

    @Override
    public String model() {
        return model;
    }
}
