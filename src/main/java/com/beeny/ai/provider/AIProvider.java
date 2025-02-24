package com.beeny.ai.provider;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface AIProvider {
    CompletableFuture<String> generateResponse(String prompt, Map<String, String> context);
    void initialize(Map<String, String> config);
    String getName();
    boolean isAvailable();
    void shutdown();
}