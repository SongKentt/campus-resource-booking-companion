package com.campus.client.rag;

import com.campus.client.llm.LlmClient;
import com.campus.client.mcp.CampusMcpClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Runs the RAG process for the campus policy assistant feature
 *
 * <p>It retrieves relevant campus information from the MCP server,
 * adds the retrieved context into the prompt, then asks the LLM to
 * generate an answer based on that context.</p>
 */
public final class RagService {

    private final CampusMcpClient mcp;
    private final LlmClient llm;

    public record RagResult(String answer, String retrievedContext) {
    }

    /**
     * Constructs a RagService with the given MCP client and LLM client.
     *
     * @param mcp the Campus MCP client used for retrieval and prompt fetching
     * @param llm the LLM client used to generate the grounded answer
     */

    public RagService(CampusMcpClient mcp, LlmClient llm) {
        this.mcp = mcp;
        this.llm = llm;
    }

    /**
     * Answers a students's policy question using RAG
     *
     * @param question student's question
     * @param topic optional topic from the UI
     * @return answer and retrieved context
     * @throws Exception if retrieval or generation fails
     */
    
    public RagResult ask(String question, String topic) throws Exception {
        RagResponse response = answerQuestion(question, topic);
        return new RagResult(response.getAnswer(), response.getContextUsed());
    }

    /**
     * Main RAG method used by FAQController or other custom classes.
     *
     * @param question student's question
     * @return full RAG response
     * @throws Exception if retrieval or generation fails
     */
    public RagResponse answerQuestion(String question) throws Exception {
        return answerQuestion(question, "");
    }

    /**
     * Runs retrieval, builds the prompt, asks the LLM, and returns the response.
     *
     * @param question student's question
     * @param topic optional topic
     * @return full RAG response
     * @throws Exception if MCP or LLM call fails
     */
    public RagResponse answerQuestion(String question, String topic) throws Exception {
        String safeQuestion = question == null ? "" : question.trim();

        String safeTopic = topic == null || topic.isBlank()
                ? "campus resource booking policies and facilities"
                : topic.trim();

        String context = mcp.callTool(
                "search_campus_info",
                Map.of("query", safeQuestion, "topK", 3)
        );

        String systemPrompt = mcp.getPrompt(
                "campus_assistant",
                Map.of("topic", safeTopic)
        );

        String userPrompt = """
                Context passages from the campus knowledge base:
                ----------------------------------------------------
                %s
                ----------------------------------------------------

                Using only the context above, answer the student's question.
                If the answer is not in the context, say you are not sure and suggest who to contact.

                Question: %s
                """.formatted(context, safeQuestion);

        String answer = llm.complete(systemPrompt, userPrompt);

        List<String> sources = extractSources(context);

        return new RagResponse(answer, context, sources);
    }

    /**
     * Extracts source names such as handbook.txt or faq.txt from the retrieved context.
     *
     * @param context context returned by the search_campus_info tool
     * @return unique source names found in the context
     */
    private List<String> extractSources(String context) {
        if (context == null || context.isBlank()) {
            return List.of();
        }

        return Arrays.stream(context.split("\n"))
                .map(String::trim)
                .filter(line -> line.startsWith("[") && line.contains("]"))
                .map(line -> line.substring(1, line.indexOf("]")).trim())
                .filter(source -> !source.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}