package com.campus.client.model;

public class RagResponse {

    private final String answer;
    private final String retrievedContext;

    public RagResponse(String answer, String retrievedContext) {
        this.answer = answer;
        this.retrievedContext = retrievedContext;
    }

    public String getAnswer() {
        return answer;
    }

    public String getRetrievedContext() {
        return retrievedContext;
    }
}
