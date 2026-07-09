package com.campus.client.rag;

import java.util.List;

/**
 * Stores the result returned from the RAG process.
 * It contains the generated answer, retrieved context, and source names.
 */
public final class RagResponse {

    private final String answer;
    private final String contextUsed;
    private final List<String> sources;

    public RagResponse(String answer, String contextUsed, List<String> sources) {
        this.answer = answer;
        this.contextUsed = contextUsed;
        this.sources = sources;
    }

    public String getAnswer() {
        return answer;
    }

    public String getContextUsed() {
        return contextUsed;
    }

    public List<String> getSources() {
        return sources;
    }

    @Override
    public String toString() {
        return "RagResponse{answer='" + answer + "', contextLength="
                + (contextUsed == null ? 0 : contextUsed.length())
                + ", sources=" + sources + "}";
    }
}