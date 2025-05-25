package com.beeny.villagesreborn.core.llm;

/**
 * Response object for LLM conversation generation
 */
public class ConversationResponse {
    private final String response;
    private final boolean success;
    private final String errorMessage;
    private final int tokensUsed;
    private final long responseTime;

    private ConversationResponse(String response, boolean success, String errorMessage, int tokensUsed, long responseTime) {
        this.response = response;
        this.success = success;
        this.errorMessage = errorMessage;
        this.tokensUsed = tokensUsed;
        this.responseTime = responseTime;
    }

    public static ConversationResponse success(String response, int tokensUsed, long responseTime) {
        return new ConversationResponse(response, true, null, tokensUsed, responseTime);
    }

    public static ConversationResponse failure(String errorMessage) {
        return new ConversationResponse(null, false, errorMessage, 0, 0);
    }

    public String getResponse() { return response; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public int getTokensUsed() { return tokensUsed; }
    public long getResponseTime() { return responseTime; }
}