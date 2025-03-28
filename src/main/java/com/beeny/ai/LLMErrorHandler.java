package com.beeny.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class LLMErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final LLMErrorHandler INSTANCE = new LLMErrorHandler();
    private final Map<ErrorType, Instant> lastReportedErrors = new HashMap<>();
    private static final long ERROR_COOLDOWN_SECONDS = 30;

    public enum ErrorType {
        INVALID_API_KEY("Invalid API Key", "The API key is missing or invalid. Please check your configuration."),
        CONNECTION_ERROR("Connection Error", "Could not connect to the AI provider. Please check your internet connection."),
        API_RATE_LIMIT("Rate Limit Exceeded", "The AI provider's rate limit has been exceeded. Please try again later."),
        PROVIDER_ERROR("AI Provider Error", "The AI provider reported an error. Please try a different provider or check your configuration."),
        GENERIC_ERROR("AI Error", "An unexpected error occurred with the AI service.");

        private final String title;
        private final String defaultMsg;

        ErrorType(String title, String defaultMsg) {
            this.title = title;
            this.defaultMsg = defaultMsg;
        }

        public String getTitle() {
            return title;
        }

        public String getDefaultMsg() {
            return defaultMsg;
        }
    }

    private LLMErrorHandler() {}

    public static LLMErrorHandler getInstance() {
        return INSTANCE;
    }

    public void reportErrorToClient(ErrorType errorType, String details) {
        LOGGER.error("LLM Error: {} - {}", errorType.getTitle(), details);

        if (shouldThrottleError(errorType)) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.execute(() -> {
                String msg = details != null && !details.isEmpty() ? details : errorType.getDefaultMsg();

                SystemToast.show(
                    client.getToastManager(),
                    SystemToast.Type.NARRATOR_TOGGLE,
                    Text.literal(errorType.getTitle()).formatted(Formatting.RED),
                    Text.literal(msg)
                );
            });
        }
    }

    public void reportErrorToPlayer(ServerPlayerEntity player, ErrorType errorType, String details) {
        if (player == null || shouldThrottleError(errorType)) {
            return;
        }

        String msg = details != null && !details.isEmpty() ? details : errorType.getDefaultMsg();

        Text errorMsg = Text.literal("⚠ " + errorType.getTitle() + ": " + msg).formatted(Formatting.RED);

        player.sendMessage(errorMsg, false);
    }

    private boolean shouldThrottleError(ErrorType errorType) {
        Instant now = Instant.now();
        Instant lastReported = lastReportedErrors.get(errorType);

        if (lastReported != null) {
            long secondsSinceLastReport = now.getEpochSecond() - lastReported.getEpochSecond();
            if (secondsSinceLastReport < ERROR_COOLDOWN_SECONDS) {
                return true;
            }
        }

        lastReportedErrors.put(errorType, now);
        return false;
    }

    public ErrorType determineErrorType(Throwable error) {
        String msg = error.getMessage();
        if (msg == null) {
            return ErrorType.GENERIC_ERROR;
        }

        msg = msg.toLowerCase();

        if (msg.contains("api key") || msg.contains("apikey") || msg.contains("authentication") || msg.contains("auth") || msg.contains("401") || msg.contains("unauthorized")) {
            return ErrorType.INVALID_API_KEY;
        }

        if (msg.contains("connect") || msg.contains("timeout") || msg.contains("network") || msg.contains("unreachable") || msg.contains("connection")) {
            return ErrorType.CONNECTION_ERROR;
        }

        if (msg.contains("rate") || msg.contains("limit") || msg.contains("too many requests") || msg.contains("429")) {
            return ErrorType.API_RATE_LIMIT;
        }

        return ErrorType.PROVIDER_ERROR;
    }
}
