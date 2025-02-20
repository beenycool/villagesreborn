package com.beeny.setup;

public class SetupWizard {
    private final LLMConfig llmConfig;

    public SetupWizard() {
        this.llmConfig = new LLMConfig();
    }

    public LLMConfig getLlmConfig() {
        return llmConfig;
    }
}