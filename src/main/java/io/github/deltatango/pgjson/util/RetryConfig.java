package io.github.deltatango.pgjson.util;

/**
 * Configuration class for retry logic with exponential backoff.
 *
 * <p>This class provides configurable retry parameters for database operations,
 * including maximum retry attempts, base delay, and exponential backoff multiplier.</p>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * RetryConfig config = RetryConfig.builder()
 *     .maxAttempts(3)
 *     .baseDelayMs(1000)
 *     .maxDelayMs(10000)
 *     .multiplier(2.0)
 *     .build();
 * }</pre>
 *
 * <h2>Retry Strategy:</h2>
 * <ul>
 *   <li><strong>Exponential Backoff:</strong> Delay increases exponentially with each retry</li>
 *   <li><strong>Jitter:</strong> Random variation to prevent thundering herd</li>
 *   <li><strong>Max Delay Cap:</strong> Prevents excessive delays</li>
 *   <li><strong>Retryable Exceptions:</strong> Only retries on specific database exceptions</li>
 * </ul>
 *
 * @author PostgreSQL JSON Client Team
 * @version 25.10.1
 * @since 1.0.0
 */
public class RetryConfig {

    private final int maxAttempts;
    private final long baseDelayMs;
    private final long maxDelayMs;
    private final double multiplier;
    private final boolean enableJitter;

    /**
     * Constructs a new RetryConfig with the specified parameters.
     *
     * @param maxAttempts maximum number of retry attempts (must be > 0)
     * @param baseDelayMs base delay in milliseconds (must be > 0)
     * @param maxDelayMs maximum delay in milliseconds (must be >= baseDelayMs)
     * @param multiplier exponential backoff multiplier (must be > 1.0)
     * @param enableJitter whether to add random jitter to delays
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public RetryConfig(int maxAttempts, long baseDelayMs, long maxDelayMs, double multiplier, boolean enableJitter) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("Max attempts must be greater than 0");
        }
        if (baseDelayMs <= 0) {
            throw new IllegalArgumentException("Base delay must be greater than 0");
        }
        if (maxDelayMs < baseDelayMs) {
            throw new IllegalArgumentException("Max delay must be greater than or equal to base delay");
        }
        if (multiplier <= 1.0) {
            throw new IllegalArgumentException("Multiplier must be greater than 1.0");
        }

        this.maxAttempts = maxAttempts;
        this.baseDelayMs = baseDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.multiplier = multiplier;
        this.enableJitter = enableJitter;
    }

    /**
     * Gets the maximum number of retry attempts.
     *
     * @return maximum retry attempts
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Gets the base delay in milliseconds.
     *
     * @return base delay in milliseconds
     */
    public long getBaseDelayMs() {
        return baseDelayMs;
    }

    /**
     * Gets the maximum delay in milliseconds.
     *
     * @return maximum delay in milliseconds
     */
    public long getMaxDelayMs() {
        return maxDelayMs;
    }

    /**
     * Gets the exponential backoff multiplier.
     *
     * @return multiplier value
     */
    public double getMultiplier() {
        return multiplier;
    }

    /**
     * Checks if jitter is enabled.
     *
     * @return true if jitter is enabled, false otherwise
     */
    public boolean isJitterEnabled() {
        return enableJitter;
    }

    /**
     * Calculates the delay for the specified attempt number.
     *
     * <p>Uses exponential backoff with optional jitter to prevent thundering herd problems.</p>
     *
     * @param attemptNumber the attempt number (0-based)
     * @return calculated delay in milliseconds
     */
    public long calculateDelay(int attemptNumber) {
        if (attemptNumber <= 0) {
            return baseDelayMs;
        }

        // Calculate exponential backoff: baseDelay * (multiplier ^ attemptNumber)
        double exponentialDelay = baseDelayMs * Math.pow(multiplier, attemptNumber);

        // Cap at maximum delay
        long delay = Math.min((long) exponentialDelay, maxDelayMs);

        // Add jitter if enabled (±25% random variation)
        if (enableJitter) {
            double jitterFactor = 0.75 + (Math.random() * 0.5); // 0.75 to 1.25
            delay = (long) (delay * jitterFactor);
        }

        return Math.max(delay, baseDelayMs);
    }

    /**
     * Builder class for creating RetryConfig instances.
     */
    public static class Builder {
        private int maxAttempts = 3;
        private long baseDelayMs = 1000;
        private long maxDelayMs = 10000;
        private double multiplier = 2.0;
        private boolean enableJitter = true;

        /**
         * Sets the maximum number of retry attempts.
         *
         * @param maxAttempts maximum retry attempts
         * @return this builder instance
         */
        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        /**
         * Sets the base delay in milliseconds.
         *
         * @param baseDelayMs base delay in milliseconds
         * @return this builder instance
         */
        public Builder baseDelayMs(long baseDelayMs) {
            this.baseDelayMs = baseDelayMs;
            return this;
        }

        /**
         * Sets the maximum delay in milliseconds.
         *
         * @param maxDelayMs maximum delay in milliseconds
         * @return this builder instance
         */
        public Builder maxDelayMs(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
            return this;
        }

        /**
         * Sets the exponential backoff multiplier.
         *
         * @param multiplier multiplier value
         * @return this builder instance
         */
        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        /**
         * Sets whether to enable jitter.
         *
         * @param enableJitter true to enable jitter, false otherwise
         * @return this builder instance
         */
        public Builder enableJitter(boolean enableJitter) {
            this.enableJitter = enableJitter;
            return this;
        }

        /**
         * Builds the RetryConfig instance.
         *
         * @return new RetryConfig instance
         * @throws IllegalArgumentException if any parameter is invalid
         */
        public RetryConfig build() {
            return new RetryConfig(maxAttempts, baseDelayMs, maxDelayMs, multiplier, enableJitter);
        }
    }

    /**
     * Creates a new builder for RetryConfig.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a default RetryConfig with standard settings.
     *
     * @return default RetryConfig instance
     */
    public static RetryConfig defaultConfig() {
        return builder().build();
    }
}
