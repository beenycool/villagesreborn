package com.beeny.villagesreborn.platform.fabric.error;

/**
 * Represents a permanent error that cannot be resolved with retry.
 * Examples: configuration errors, missing dependencies, API key issues.
 */
public class PermanentError extends Exception {
    private final ErrorSeverity severity;
    private final boolean allowDegradedMode;

    public PermanentError(String message, Throwable cause, ErrorSeverity severity, boolean allowDegradedMode) {
        super(message, cause);
        this.severity = severity;
        this.allowDegradedMode = allowDegradedMode;
    }

    public PermanentError(String message, ErrorSeverity severity, boolean allowDegradedMode) {
        super(message);
        this.severity = severity;
        this.allowDegradedMode = allowDegradedMode;
    }

    public ErrorSeverity getSeverity() {
        return severity;
    }

    public boolean isAllowDegradedMode() {
        return allowDegradedMode;
    }

    public enum ErrorSeverity {
        LOW,     // Non-critical features affected
        MEDIUM,  // Important features affected
        HIGH     // Core functionality affected
    }
}