package io.github.deltatango.pgjson.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit breaker implementation for database operations to prevent cascading failures.
 *
 * <p>This circuit breaker monitors database operation failures and opens the circuit
 * when failure rate exceeds the threshold, preventing further calls to the database
 * until the circuit is reset.</p>
 *
 * <h2>Circuit States:</h2>
 * <ul>
 *   <li><strong>CLOSED:</strong> Normal operation, all requests pass through</li>
 *   <li><strong>OPEN:</strong> Circuit is open, requests are rejected immediately</li>
 *   <li><strong>HALF_OPEN:</strong> Testing state, limited requests allowed</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * CircuitBreaker breaker = CircuitBreaker.builder()
 *     .failureThreshold(5)
 *     .timeoutMs(60000)
 *     .build();
 *
 * try {
 *     return breaker.execute(() -> databaseOperation());
 * } catch (CircuitBreakerOpenException e) {
 *     // Handle circuit breaker open
 * }
 * }</pre>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><strong>Failure Threshold:</strong> Opens circuit after N consecutive failures</li>
 *   <li><strong>Timeout:</strong> Automatic reset after timeout period</li>
 *   <li><strong>Half-Open Testing:</strong> Limited requests during recovery</li>
 *   <li><strong>Metrics:</strong> Tracks success/failure rates</li>
 *   <li><strong>Thread Safety:</strong> Safe for concurrent access</li>
 * </ul>
 *
 * @author PostgreSQL JSON Client Team
 * @version 25.10.1
 * @since 1.0.0
 */
@Slf4j
public class CircuitBreaker {

    /**
     * Circuit breaker states.
     */
    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    private final int failureThreshold;
    private final long timeoutMs;
    private final int halfOpenMaxCalls;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger halfOpenCalls = new AtomicInteger(0);
    private final AtomicLong totalCalls = new AtomicLong(0);
    private final AtomicLong totalFailures = new AtomicLong(0);

    /**
     * Constructs a new CircuitBreaker with the specified parameters.
     *
     * @param failureThreshold number of consecutive failures before opening circuit
     * @param timeoutMs timeout in milliseconds before attempting to close circuit
     * @param halfOpenMaxCalls maximum calls allowed in half-open state
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public CircuitBreaker(int failureThreshold, long timeoutMs, int halfOpenMaxCalls) {
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("Failure threshold must be greater than 0");
        }
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("Timeout must be greater than 0");
        }
        if (halfOpenMaxCalls <= 0) {
            throw new IllegalArgumentException("Half-open max calls must be greater than 0");
        }

        this.failureThreshold = failureThreshold;
        this.timeoutMs = timeoutMs;
        this.halfOpenMaxCalls = halfOpenMaxCalls;
    }

    /**
     * Executes the provided operation with circuit breaker protection.
     *
     * <p>This method will either execute the operation or throw a CircuitBreakerOpenException
     * if the circuit is open and not ready for testing.</p>
     *
     * @param operation the operation to execute
     * @param <T> the return type of the operation
     * @return the result of the operation
     * @throws CircuitBreakerOpenException if the circuit is open
     * @throws Exception if the operation fails
     */
    public <T> T execute(Operation<T> operation) throws Exception {
        totalCalls.incrementAndGet();

        if (!allowRequest()) {
            throw new CircuitBreakerOpenException("Circuit breaker is open");
        }

        try {
            T result = operation.execute();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }

    /**
     * Checks if a request should be allowed based on the current circuit state.
     *
     * @return true if the request should be allowed, false otherwise
     */
    private boolean allowRequest() {
        State currentState = state.get();

        switch (currentState) {
            case CLOSED:
                return true;

            case OPEN:
                if (shouldAttemptReset()) {
                    if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                        halfOpenCalls.set(0);
                        log.info("Circuit breaker transitioning to HALF_OPEN state");
                    }
                    return state.get() == State.HALF_OPEN;
                }
                return false;

            case HALF_OPEN:
                int calls = halfOpenCalls.incrementAndGet();
                return calls <= halfOpenMaxCalls;

            default:
                return false;
        }
    }

    /**
     * Checks if enough time has passed to attempt resetting the circuit.
     *
     * @return true if reset should be attempted, false otherwise
     */
    private boolean shouldAttemptReset() {
        long currentTime = Instant.now().toEpochMilli();
        long timeSinceLastFailure = currentTime - lastFailureTime.get();
        return timeSinceLastFailure >= timeoutMs;
    }

    /**
     * Handles successful operation execution.
     */
    private void onSuccess() {
        failureCount.set(0);

        if (state.get() == State.HALF_OPEN) {
            if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                log.info("Circuit breaker transitioning to CLOSED state");
            }
        }
    }

    /**
     * Handles failed operation execution.
     */
    private void onFailure() {
        totalFailures.incrementAndGet();
        lastFailureTime.set(Instant.now().toEpochMilli());

        int failures = failureCount.incrementAndGet();

        if (failures >= failureThreshold) {
            if (state.compareAndSet(State.CLOSED, State.OPEN)
                || state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
                log.warn("Circuit breaker transitioning to OPEN state after {} failures", failures);
            }
        }
    }

    /**
     * Gets the current circuit breaker state.
     *
     * @return current state
     */
    public State getState() {
        return state.get();
    }

    /**
     * Gets the current failure count.
     *
     * @return number of consecutive failures
     */
    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * Gets the total number of calls made.
     *
     * @return total calls
     */
    public long getTotalCalls() {
        return totalCalls.get();
    }

    /**
     * Gets the total number of failures.
     *
     * @return total failures
     */
    public long getTotalFailures() {
        return totalFailures.get();
    }

    /**
     * Gets the failure rate as a percentage.
     *
     * @return failure rate percentage (0.0 to 100.0)
     */
    public double getFailureRate() {
        long calls = totalCalls.get();
        if (calls == 0) {
            return 0.0;
        }
        return (double) totalFailures.get() / calls * 100.0;
    }

    /**
     * Resets the circuit breaker to CLOSED state.
     *
     * <p>This method should be used with caution as it bypasses the normal
     * circuit breaker logic. It's typically used for manual recovery or testing.</p>
     */
    public void reset() {
        state.set(State.CLOSED);
        failureCount.set(0);
        halfOpenCalls.set(0);
        log.info("Circuit breaker manually reset to CLOSED state");
    }

    /**
     * Functional interface for operations that can be executed by the circuit breaker.
     *
     * @param <T> the return type of the operation
     */
    @FunctionalInterface
    public interface Operation<T> {
        /**
         * Executes the operation.
         *
         * @return the result of the operation
         * @throws Exception if the operation fails
         */
        T execute() throws Exception;
    }

    /**
     * Exception thrown when the circuit breaker is open and rejects requests.
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }

    /**
     * Builder class for creating CircuitBreaker instances.
     */
    public static class Builder {
        private int failureThreshold = 5;
        private long timeoutMs = 60000; // 1 minute
        private int halfOpenMaxCalls = 3;

        /**
         * Sets the failure threshold.
         *
         * @param failureThreshold number of consecutive failures before opening circuit
         * @return this builder instance
         */
        public Builder failureThreshold(int failureThreshold) {
            this.failureThreshold = failureThreshold;
            return this;
        }

        /**
         * Sets the timeout in milliseconds.
         *
         * @param timeoutMs timeout in milliseconds
         * @return this builder instance
         */
        public Builder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        /**
         * Sets the maximum calls allowed in half-open state.
         *
         * @param halfOpenMaxCalls maximum calls in half-open state
         * @return this builder instance
         */
        public Builder halfOpenMaxCalls(int halfOpenMaxCalls) {
            this.halfOpenMaxCalls = halfOpenMaxCalls;
            return this;
        }

        /**
         * Builds the CircuitBreaker instance.
         *
         * @return new CircuitBreaker instance
         * @throws IllegalArgumentException if any parameter is invalid
         */
        public CircuitBreaker build() {
            return new CircuitBreaker(failureThreshold, timeoutMs, halfOpenMaxCalls);
        }
    }

    /**
     * Creates a new builder for CircuitBreaker.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a default CircuitBreaker with standard settings.
     *
     * @return default CircuitBreaker instance
     */
    public static CircuitBreaker defaultBreaker() {
        return builder().build();
    }
}
