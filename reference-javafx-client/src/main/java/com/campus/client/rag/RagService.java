package com.campus.client.rag;

import com.campus.client.llm.LlmClient;
import com.campus.client.mcp.CampusMcpClient;

import java.util.Map;


import com.campus.client.llm.LlmClient;
import com.campus.client.mcp.CampusMcpClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Runs the RAG process for the Policy Assistant feature.
 *
 *  <p>It retrieves relevant campus information from the MCP server,
 *  adds the retrieved context into the prompt, then asks the LLM to
 *  generate an answer based on that context.</p>
 */
public final class RagService {

    private final CampusMcpClient mcp;
    private final LlmClient llm;

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
     * Answers a student's policy question using RAG.
     *
     * <p>This method performs network calls, so it should be called from a background thread instead of the JavaFX UI thread.</p>
     * @param question question entered by the student in FAQView
     * @return RAG response containing the answer, context used, and sources
     * @throws Exception if MCP retrieval, prompt loading, or LLM generation fails
     */

    public RagResponse answerQuestion(String question) throws Exception {

        // Step 1: Retrieve relevant context from the campus knowledge base
        // Call search_campus_info to retrieve the top-3 most relevant knowledge base passages.
        String context = mcp.callTool("search_campus_info",
                Map.of("query", question, "topK", 3));

        //  Step 2: Get the campus assistant system prompt from the MCP server.
        String systemPrompt = mcp.getPrompt("campus_assistant",
                Map.of("topic", "campus resource booking policies and facilities"));

        //Step 3: Build the prompt that combines retrieved context and the student's question.

        String userPrompt = """
                Context passages from the campus knowledge base:
                ----------------------------------------------------
                %s
                ----------------------------------------------------
                Using only the context above, answer the student's question.
                If the answer is not in the context, say you are not sure and suggest
                who to contact.

                Question: %s
                """.formatted(context, question);

        // Step 4: Ask the LLM to generate the final answer
        String answer = llm.complete(systemPrompt, userPrompt);

        // Extract source names for display in the FAQ screen
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
