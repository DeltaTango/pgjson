package io.github.deltatango.pgjson.util;

import io.github.deltatango.pgjson.exceptions.PostgreJsonException;
import io.github.deltatango.pgjson.model.operations.OperationResult;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * Executor for database operations with retry logic and circuit breaker protection.
 * 
 * <p>This class provides a robust wrapper around database operations, implementing
 * retry logic with exponential backoff and circuit breaker pattern to handle
 * transient failures and prevent cascading failures.</p>
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li><strong>Retry Logic:</strong> Exponential backoff with configurable parameters</li>
 *   <li><strong>Circuit Breaker:</strong> Prevents cascading failures during outages</li>
 *   <li><strong>Exception Handling:</strong> Distinguishes between retryable and non-retryable exceptions</li>
 *   <li><strong>Metrics:</strong> Tracks operation success/failure rates</li>
 *   <li><strong>Thread Safety:</strong> Safe for concurrent access</li>
 * </ul>
 * 
 * <h2>Usage:</h2>
 * <pre>{@code
 * DatabaseOperationExecutor executor = new DatabaseOperationExecutor(
 *     RetryConfig.defaultConfig(),
 *     CircuitBreaker.defaultBreaker()
 * );
 * 
 * List<DatabaseEntry> result = executor.executeWithRetry(connection, 
 *     conn -> databaseEntryRepo.searchData(conn, tableName, order, limit, offset, searchObject, indexes, logicalOperator)
 * );
 * }</pre>
 * 
 * <h2>Retryable Exceptions:</h2>
 * <ul>
 *   <li><strong>SQLException:</strong> Connection timeouts, deadlocks, temporary failures</li>
 *   <li><strong>PostgreJsonException:</strong> Database-related errors that might be transient</li>
 * </ul>
 * 
 * <h2>Non-Retryable Exceptions:</h2>
 * <ul>
 *   <li><strong>IllegalArgumentException:</strong> Invalid parameters</li>
 *   <li><strong>RequestException:</strong> Validation errors</li>
 *   <li><strong>SecurityException:</strong> Authentication/authorization failures</li>
 * </ul>
 * 
 * @author PostgreSQL JSON Client Team
 * @version 25.10.1
 * @since 1.0.0
 * @see RetryConfig
 * @see CircuitBreaker
 */
@Slf4j
public class DatabaseOperationExecutor {
    
    private final RetryConfig retryConfig;
    private final CircuitBreaker circuitBreaker;
    
    /**
     * Constructs a new DatabaseOperationExecutor with the specified retry and circuit breaker configuration.
     * 
     * @param retryConfig retry configuration
     * @param circuitBreaker circuit breaker instance
     * @throws IllegalArgumentException if either parameter is null
     */
    public DatabaseOperationExecutor(RetryConfig retryConfig, CircuitBreaker circuitBreaker) {
        if (retryConfig == null) {
            throw new IllegalArgumentException("Retry config cannot be null");
        }
        if (circuitBreaker == null) {
            throw new IllegalArgumentException("Circuit breaker cannot be null");
        }
        
        this.retryConfig = retryConfig;
        this.circuitBreaker = circuitBreaker;
    }
    
    /**
     * Executes a database operation with retry logic and circuit breaker protection.
     * 
     * <p>This method will attempt to execute the operation with the configured retry logic.
     * If the circuit breaker is open, it will throw a CircuitBreakerOpenException immediately.
     * If the operation fails with a retryable exception, it will retry according to the retry configuration.</p>
     * 
     * @param connection the database connection
     * @param operation the database operation to execute
     * @param <T> the return type of the operation
     * @return the result of the operation
     * @throws CircuitBreaker.CircuitBreakerOpenException if the circuit breaker is open
     * @throws PostgreJsonException if the operation fails after all retries
     * @throws IllegalArgumentException if connection or operation is null
     */
    public <T> T executeWithRetry(Connection connection, Function<Connection, T> operation) throws PostgreJsonException {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }
        
        Exception lastException = null;
        
        for (int attempt = 0; attempt < retryConfig.getMaxAttempts(); attempt++) {
            try {
                return circuitBreaker.execute(() -> operation.apply(connection));
            } catch (CircuitBreaker.CircuitBreakerOpenException e) {
                log.warn("Circuit breaker is open, rejecting request");
                throw e;
            } catch (Exception e) {
                lastException = e;
                
                if (!isRetryableException(e)) {
                    log.debug("Non-retryable exception encountered: {}", e.getClass().getSimpleName());
                    throw new PostgreJsonException("Database operation failed with non-retryable exception", e);
                }
                
                if (attempt < retryConfig.getMaxAttempts() - 1) {
                    long delay = retryConfig.calculateDelay(attempt);
                    log.warn("Database operation failed (attempt {}/{}), retrying in {}ms: {}", 
                            attempt + 1, retryConfig.getMaxAttempts(), delay, e.getMessage());
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new PostgreJsonException("Retry interrupted", ie);
                    }
                } else {
                    log.error("Database operation failed after {} attempts", retryConfig.getMaxAttempts());
                }
            }
        }
        
        throw new PostgreJsonException("Database operation failed after " + retryConfig.getMaxAttempts() + " attempts", lastException);
    }
    
    /**
     * Executes a database operation with retry logic, using a managed connection.
     * 
     * <p>This method is a convenience wrapper that handles managed connections automatically.
     * It's the preferred method for most use cases as it ensures proper connection cleanup.</p>
     * 
     * @param dbUtil the database utility for connection management
     * @param operation the database operation to execute
     * @param <T> the return type of the operation
     * @return the result of the operation
     * @throws CircuitBreaker.CircuitBreakerOpenException if the circuit breaker is open
     * @throws PostgreJsonException if the operation fails after all retries
     * @throws IllegalArgumentException if dbUtil or operation is null
     */
    public <T> T executeWithRetry(DbUtil dbUtil, Function<Connection, T> operation) throws PostgreJsonException {
        if (dbUtil == null) {
            throw new IllegalArgumentException("DbUtil cannot be null");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }
        
        try (ManagedConnection managedConnection = dbUtil.getManagedConnection()) {
            return executeWithRetry(managedConnection.getConnection(), operation);
        } catch (SQLException e) {
            throw new PostgreJsonException("Failed to get managed connection", e);
        }
    }
    
    /**
     * Executes a database operation that returns {@link OperationResult} with retry logic and circuit breaker protection.
     *
     * <p>If the operation returns {@link OperationResult.Error} with a retryable cause, the operation
     * is retried according to the retry configuration. {@link OperationResult.Success} and
     * {@link OperationResult.NotFound} results are returned immediately without retrying.</p>
     *
     * @param connection the database connection
     * @param operation the database operation to execute
     * @param <T> the value type inside the OperationResult
     * @return the OperationResult from the operation
     * @throws CircuitBreaker.CircuitBreakerOpenException if the circuit breaker is open
     * @throws IllegalArgumentException if connection or operation is null
     */
    public <T> OperationResult<T> executeOperationWithRetry(Connection connection, Function<Connection, OperationResult<T>> operation) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }

        OperationResult<T> lastResult = null;

        for (int attempt = 0; attempt < retryConfig.getMaxAttempts(); attempt++) {
            try {
                OperationResult<T> result = circuitBreaker.execute(() -> operation.apply(connection));

                // Success and NotFound are final -- return immediately
                if (result instanceof OperationResult.Success || result instanceof OperationResult.NotFound) {
                    return result;
                }

                // Error -- check if retryable
                if (result instanceof OperationResult.Error<T> error) {
                    lastResult = result;
                    Throwable cause = error.cause();

                    if (cause instanceof Exception exc && isRetryableException(exc)) {
                        if (attempt < retryConfig.getMaxAttempts() - 1) {
                            long delay = retryConfig.calculateDelay(attempt);
                            log.warn("Database operation returned retryable error (attempt {}/{}), retrying in {}ms: {}",
                                    attempt + 1, retryConfig.getMaxAttempts(), delay, error.message());
                            try {
                                Thread.sleep(delay);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                return OperationResult.error("Retry interrupted", ie);
                            }
                        } else {
                            log.error("Database operation failed after {} attempts: {}", retryConfig.getMaxAttempts(), error.message());
                        }
                    } else {
                        // Non-retryable error -- return immediately
                        return result;
                    }
                }
            } catch (CircuitBreaker.CircuitBreakerOpenException e) {
                log.warn("Circuit breaker is open, rejecting request");
                throw e;
            } catch (Exception e) {
                // Unexpected exception from circuit breaker execution
                lastResult = OperationResult.error("Unexpected error during operation execution", e);

                if (!isRetryableException(e)) {
                    return lastResult;
                }

                if (attempt < retryConfig.getMaxAttempts() - 1) {
                    long delay = retryConfig.calculateDelay(attempt);
                    log.warn("Database operation threw exception (attempt {}/{}), retrying in {}ms: {}",
                            attempt + 1, retryConfig.getMaxAttempts(), delay, e.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return OperationResult.error("Retry interrupted", ie);
                    }
                } else {
                    log.error("Database operation failed after {} attempts", retryConfig.getMaxAttempts());
                }
            }
        }

        return lastResult != null ? lastResult
                : OperationResult.error("Database operation failed after " + retryConfig.getMaxAttempts() + " attempts");
    }

    /**
     * Executes a database operation that returns {@link OperationResult} with retry logic, using a managed connection.
     *
     * @param dbUtil the database utility for connection management
     * @param operation the database operation to execute
     * @param <T> the value type inside the OperationResult
     * @return the OperationResult from the operation
     * @throws CircuitBreaker.CircuitBreakerOpenException if the circuit breaker is open
     * @throws IllegalArgumentException if dbUtil or operation is null
     */
    public <T> OperationResult<T> executeOperationWithRetry(DbUtil dbUtil, Function<Connection, OperationResult<T>> operation) {
        if (dbUtil == null) {
            throw new IllegalArgumentException("DbUtil cannot be null");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }

        try (ManagedConnection managedConnection = dbUtil.getManagedConnection()) {
            return executeOperationWithRetry(managedConnection.getConnection(), operation);
        } catch (SQLException e) {
            return OperationResult.error("Failed to get managed connection", e);
        }
    }

    /**
     * Checks if an exception is retryable.
     * 
     * <p>This method determines whether an exception should trigger a retry attempt.
     * Only certain types of exceptions are considered retryable, typically those
     * indicating transient failures that might succeed on retry.</p>
     * 
     * @param exception the exception to check
     * @return true if the exception is retryable, false otherwise
     */
    private boolean isRetryableException(Exception exception) {
        if (exception instanceof SQLException) {
            SQLException sqlException = (SQLException) exception;
            String sqlState = sqlException.getSQLState();
            
            // Retry on connection-related errors
            if ("08000".equals(sqlState) || // Connection exception
                "08003".equals(sqlState) || // Connection does not exist
                "08006".equals(sqlState) || // Connection failure
                "08001".equals(sqlState) || // SQL client unable to establish connection
                "08004".equals(sqlState)) { // SQL server rejected connection
                return true;
            }
            
            // Retry on deadlock and lock timeout
            if ("40P01".equals(sqlState) || // Deadlock detected
                "55P03".equals(sqlState)) { // Lock not available
                return true;
            }
            
            // Retry on temporary resource issues
            if (sqlException.getMessage() != null) {
                String message = sqlException.getMessage().toLowerCase();
                if (message.contains("timeout") ||
                    message.contains("connection") ||
                    message.contains("temporary") ||
                    message.contains("busy") ||
                    message.contains("resource")) {
                    return true;
                }
            }
        }
        
        if (exception instanceof PostgreJsonException) {
            String message = exception.getMessage();
            if (message != null) {
                String lowerMessage = message.toLowerCase();
                if (lowerMessage.contains("connection") ||
                    lowerMessage.contains("timeout") ||
                    lowerMessage.contains("temporary")) {
                    return true;
                }
            }
        }
        
        // Check for RuntimeException with PostgreJsonException as cause
        if (exception instanceof RuntimeException) {
            Throwable cause = exception.getCause();
            if (cause instanceof PostgreJsonException) {
                String message = cause.getMessage();
                if (message != null) {
                    String lowerMessage = message.toLowerCase();
                    if (lowerMessage.contains("connection") ||
                        lowerMessage.contains("timeout") ||
                        lowerMessage.contains("temporary")) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Gets the current retry configuration.
     * 
     * @return retry configuration
     */
    public RetryConfig getRetryConfig() {
        return retryConfig;
    }
    
    /**
     * Gets the current circuit breaker.
     * 
     * @return circuit breaker instance
     */
    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }
    
    /**
     * Gets the current circuit breaker state.
     * 
     * @return circuit breaker state
     */
    public CircuitBreaker.State getCircuitBreakerState() {
        return circuitBreaker.getState();
    }
    
    /**
     * Gets the circuit breaker failure rate.
     * 
     * @return failure rate percentage
     */
    public double getCircuitBreakerFailureRate() {
        return circuitBreaker.getFailureRate();
    }
    
    /**
     * Resets the circuit breaker.
     * 
     * <p>This method should be used with caution as it bypasses the normal
     * circuit breaker logic. It's typically used for manual recovery or testing.</p>
     */
    public void resetCircuitBreaker() {
        circuitBreaker.reset();
    }
}
