package com.beeny.villagesreborn.platform.fabric.error;

/**
 * Represents a transient error that may be resolved with retry.
 * Examples: network timeouts, temporary resource unavailability.
 */
public class TransientError extends Exception {
    private final int retryCount;
    private final long nextRetryDelay;

    public TransientError(String message, Throwable cause, int retryCount, long nextRetryDelay) {
        super(message, cause);
        this.retryCount = retryCount;
        this.nextRetryDelay = nextRetryDelay;
    }

    public TransientError(String message, int retryCount, long nextRetryDelay) {
        super(message);
        this.retryCount = retryCount;
        this.nextRetryDelay = nextRetryDelay;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public long getNextRetryDelay() {
        return nextRetryDelay;
    }

    public boolean shouldRetry() {
        return retryCount < 3; // Max 3 retries
    }
}