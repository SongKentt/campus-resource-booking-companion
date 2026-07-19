package com.campus.client.rag;

import com.campus.client.llm.LlmClient;
import com.campus.client.mcp.CampusMcpClient;

import java.util.Map;

public final class RagService {

    public record RagResult(String retrievedContext, String systemPrompt, String answer) {
    }

    private final CampusMcpClient mcp;
    private final LlmClient llm;

    public RagService(CampusMcpClient mcp, LlmClient llm) {
        this.mcp = mcp;
        this.llm = llm;
    }

    public RagResult ask(String question, String topic) throws Exception {
        // get relevant passages from the knowledge base
        String context = mcp.callTool("search_campus_info",
                Map.of("query", question, "topK", 3));

        // get the prompt template from the server and build the user prompt
        // this keeps the system prompt on the server so we can update it without changing code
        String systemPrompt = mcp.getPrompt("campus_assistant",
                Map.of("topic", topic == null || topic.isBlank() ? "general campus services" : topic));

        String userPrompt = """
            Context passages from the campus knowledge base:
            ----------------------------------------------------
            %s
            ----------------------------------------------------
            Using only the context above, answer the student's question. If the answer is not in the
            context, say you are not sure and suggest who to contact.

            Question: %s
            """.formatted(context, question);

        // send to the llm and get the answer
        String answer = llm.complete(systemPrompt, userPrompt);

        return new RagResult(context, systemPrompt, answer);
    }
}