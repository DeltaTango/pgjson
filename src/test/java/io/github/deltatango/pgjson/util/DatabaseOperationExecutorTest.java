package io.github.deltatango.pgjson.util;

import io.github.deltatango.pgjson.exceptions.PostgreJsonException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for DatabaseOperationExecutor.
 * 
 * <p>This test class covers all scenarios including successful execution,
 * retry logic, circuit breaker behavior, and error handling.</p>
 */
class DatabaseOperationExecutorTest {
    
    private DatabaseOperationExecutor executor;
    private RetryConfig retryConfig;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        retryConfig = RetryConfig.builder()
                .maxAttempts(3)
                .baseDelayMs(100)
                .maxDelayMs(1000)
                .multiplier(2.0)
                .build();
        
        circuitBreaker = CircuitBreaker.builder()
                .failureThreshold(3)
                .timeoutMs(1000)
                .halfOpenMaxCalls(2)
                .build();
        
        executor = new DatabaseOperationExecutor(retryConfig, circuitBreaker);
    }

    @Test
    void testConstructor_ValidParameters() {
        // Test that constructor accepts valid parameters
        assertDoesNotThrow(() -> new DatabaseOperationExecutor(retryConfig, circuitBreaker));
    }

    @Test
    void testConstructor_NullRetryConfig() {
        // Test that constructor throws exception for null retry config
        assertThrows(IllegalArgumentException.class, 
                () -> new DatabaseOperationExecutor(null, circuitBreaker));
    }

    @Test
    void testConstructor_NullCircuitBreaker() {
        // Test that constructor throws exception for null circuit breaker
        assertThrows(IllegalArgumentException.class, 
                () -> new DatabaseOperationExecutor(retryConfig, null));
    }

    @Test
    void testExecuteWithRetry_Success() throws Exception {
        // Test successful execution without retries
        String expectedResult = "test result";
        Function<Connection, String> operation = conn -> expectedResult;
        
        // Create a simple test connection
        Connection testConnection = new TestConnection();
        String result = executor.executeWithRetry(testConnection, operation);
        
        assertEquals(expectedResult, result);
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getFailureCount());
    }

    @Test
    void testExecuteWithRetry_NullConnection() {
        // Test that null connection throws exception
        Function<Connection, String> operation = conn -> "result";
        
        assertThrows(IllegalArgumentException.class, 
                () -> executor.executeWithRetry((Connection) null, operation));
    }

    @Test
    void testExecuteWithRetry_NullOperation() {
        // Test that null operation throws exception
        Connection testConnection = new TestConnection();
        assertThrows(IllegalArgumentException.class, 
                () -> executor.executeWithRetry(testConnection, (Function<Connection, String>) null));
    }

    @Test
    void testExecuteWithRetry_RetryableException_SuccessAfterRetry() throws Exception {
        // Test retry logic with retryable exception
        String expectedResult = "success after retry";
        Function<Connection, String> operation = conn -> {
            if (circuitBreaker.getTotalCalls() < 2) {
                // Use RuntimeException with PostgreJsonException cause
                throw new RuntimeException(new PostgreJsonException("Connection timeout"));
            }
            return expectedResult;
        };
        
        Connection testConnection = new TestConnection();
        String result = executor.executeWithRetry(testConnection, operation);
        
        assertEquals(expectedResult, result);
        assertEquals(2, circuitBreaker.getTotalCalls());
        assertEquals(1, circuitBreaker.getTotalFailures());
    }

    @Test
    void testExecuteWithRetry_RetryableException_MaxRetriesExceeded() {
        // Test that max retries exceeded throws exception
        Function<Connection, String> operation = conn -> {
            throw new RuntimeException(new PostgreJsonException("Connection timeout"));
        };
        
        Connection testConnection = new TestConnection();
        PostgreJsonException exception = assertThrows(PostgreJsonException.class, 
                () -> executor.executeWithRetry(testConnection, operation));
        
        assertTrue(exception.getMessage().contains("failed after 3 attempts"));
        assertEquals(3, circuitBreaker.getTotalCalls());
        assertEquals(3, circuitBreaker.getTotalFailures());
    }

    @Test
    void testExecuteWithRetry_NonRetryableException() {
        // Test that non-retryable exception is not retried
        Function<Connection, String> operation = conn -> {
            throw new IllegalArgumentException("Invalid parameter");
        };
        
        Connection testConnection = new TestConnection();
        PostgreJsonException exception = assertThrows(PostgreJsonException.class, 
                () -> executor.executeWithRetry(testConnection, operation));
        
        assertTrue(exception.getMessage().contains("non-retryable exception"));
        assertEquals(1, circuitBreaker.getTotalCalls());
        assertEquals(1, circuitBreaker.getTotalFailures());
    }

    @Test
    void testExecuteWithRetry_CircuitBreakerOpen() {
        // Test circuit breaker open scenario
        // Create a fresh executor for this test
        CircuitBreaker freshCircuitBreaker = CircuitBreaker.builder()
                .failureThreshold(3)
                .timeoutMs(1000)
                .halfOpenMaxCalls(2)
                .build();
        DatabaseOperationExecutor freshExecutor = new DatabaseOperationExecutor(retryConfig, freshCircuitBreaker);
        
        // First, open the circuit breaker by making it fail
        try {
            Connection testConnection = new TestConnection();
            freshExecutor.executeWithRetry(testConnection, conn -> {
                throw new RuntimeException(new PostgreJsonException("Connection failed"));
            });
        } catch (PostgreJsonException e) {
            // Expected after max retries
        }
        
        // Now circuit should be open
        assertEquals(CircuitBreaker.State.OPEN, freshCircuitBreaker.getState());
        
        // Next call should throw CircuitBreakerOpenException immediately
        Connection testConnection = new TestConnection();
        assertThrows(CircuitBreaker.CircuitBreakerOpenException.class, 
                () -> freshExecutor.executeWithRetry(testConnection, conn -> "result"));
    }

    @Test
    void testExecuteWithRetry_InterruptedException() {
        // Test interruption during retry delay
        Function<Connection, String> operation = conn -> {
            throw new RuntimeException(new PostgreJsonException("Connection timeout"));
        };
        
        Thread.currentThread().interrupt();
        
        Connection testConnection = new TestConnection();
        PostgreJsonException exception = assertThrows(PostgreJsonException.class, 
                () -> executor.executeWithRetry(testConnection, operation));
        
        assertTrue(exception.getMessage().contains("Retry interrupted"));
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    void testIsRetryableException_SQLException_ConnectionErrors() {
        // Test various SQL connection error states
        String[] connectionStates = {"08000", "08003", "08006", "08001", "08004"};
        
        for (String state : connectionStates) {
            SQLException sqlException = new SQLException("Connection error", state);
            assertTrue(isRetryableException(sqlException), 
                    "SQL state " + state + " should be retryable");
        }
    }

    @Test
    void testIsRetryableException_SQLException_DeadlockErrors() {
        // Test deadlock and lock timeout states
        String[] lockStates = {"40P01", "55P03"};
        
        for (String state : lockStates) {
            SQLException sqlException = new SQLException("Lock error", state);
            assertTrue(isRetryableException(sqlException), 
                    "SQL state " + state + " should be retryable");
        }
    }

    @Test
    void testIsRetryableException_SQLException_MessageBased() {
        // Test message-based retryable detection
        String[] retryableMessages = {
            "Connection timeout",
            "Connection failed",
            "Temporary failure",
            "Resource busy",
            "Database connection lost"
        };
        
        for (String message : retryableMessages) {
            SQLException sqlException = new SQLException(message);
            assertTrue(isRetryableException(sqlException), 
                    "Message '" + message + "' should be retryable");
        }
    }

    @Test
    void testIsRetryableException_PostgreJsonException_Retryable() {
        // Test PostgreJsonException with retryable messages
        String[] retryableMessages = {
            "Connection failed",
            "Timeout occurred",
            "Temporary database error"
        };
        
        for (String message : retryableMessages) {
            PostgreJsonException exception = new PostgreJsonException(message);
            assertTrue(isRetryableException(exception), 
                    "Message '" + message + "' should be retryable");
        }
    }

    @Test
    void testIsRetryableException_NonRetryableExceptions() {
        // Test non-retryable exceptions
        Exception[] nonRetryableExceptions = {
            new IllegalArgumentException("Invalid parameter"),
            new SecurityException("Access denied"),
            new RuntimeException("General error")
        };
        
        for (Exception exception : nonRetryableExceptions) {
            assertFalse(isRetryableException(exception), 
                    exception.getClass().getSimpleName() + " should not be retryable");
        }
    }

    @Test
    void testGetRetryConfig() {
        // Test getter for retry config
        assertEquals(retryConfig, executor.getRetryConfig());
    }

    @Test
    void testGetCircuitBreaker() {
        // Test getter for circuit breaker
        assertEquals(circuitBreaker, executor.getCircuitBreaker());
    }

    @Test
    void testGetCircuitBreakerState() {
        // Test getter for circuit breaker state
        assertEquals(CircuitBreaker.State.CLOSED, executor.getCircuitBreakerState());
    }

    @Test
    void testGetCircuitBreakerFailureRate() {
        // Test getter for circuit breaker failure rate
        assertEquals(0.0, executor.getCircuitBreakerFailureRate(), 0.01);
    }

    @Test
    void testResetCircuitBreaker() {
        // Test circuit breaker reset
        // Create a fresh executor for this test
        CircuitBreaker freshCircuitBreaker = CircuitBreaker.builder()
                .failureThreshold(3)
                .timeoutMs(1000)
                .halfOpenMaxCalls(2)
                .build();
        DatabaseOperationExecutor freshExecutor = new DatabaseOperationExecutor(retryConfig, freshCircuitBreaker);
        
        // First open the circuit breaker by making it fail
        try {
            Connection testConnection = new TestConnection();
            freshExecutor.executeWithRetry(testConnection, conn -> {
                throw new RuntimeException(new PostgreJsonException("Connection failed"));
            });
        } catch (PostgreJsonException e) {
            // Expected after max retries
        }
        
        assertEquals(CircuitBreaker.State.OPEN, freshCircuitBreaker.getState());
        
        // Reset the circuit breaker
        freshExecutor.resetCircuitBreaker();
        
        assertEquals(CircuitBreaker.State.CLOSED, freshCircuitBreaker.getState());
        assertEquals(0, freshCircuitBreaker.getFailureCount());
    }

    // Helper method to access private isRetryableException method
    private boolean isRetryableException(Exception exception) {
        try {
            java.lang.reflect.Method method = DatabaseOperationExecutor.class
                    .getDeclaredMethod("isRetryableException", Exception.class);
            method.setAccessible(true);
            return (Boolean) method.invoke(executor, exception);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access private method", e);
        }
    }

    // Simple test connection implementation
    private static class TestConnection implements Connection {
        @Override
        public java.sql.Statement createStatement() throws SQLException {
            return null;
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql) throws SQLException {
            return null;
        }

        @Override
        public java.sql.CallableStatement prepareCall(String sql) throws SQLException {
            return null;
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            return null;
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return false;
        }

        @Override
        public void commit() throws SQLException {
        }

        @Override
        public void rollback() throws SQLException {
        }

        @Override
        public void close() throws SQLException {
        }

        @Override
        public boolean isClosed() throws SQLException {
            return false;
        }

        @Override
        public java.sql.DatabaseMetaData getMetaData() throws SQLException {
            return null;
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return false;
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {
        }

        @Override
        public String getCatalog() throws SQLException {
            return null;
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return 0;
        }

        @Override
        public java.sql.SQLWarning getWarnings() throws SQLException {
            return null;
        }

        @Override
        public void clearWarnings() throws SQLException {
        }

        @Override
        public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return null;
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return null;
        }

        @Override
        public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return null;
        }

        @Override
        public java.util.Map<String, Class<?>> getTypeMap() throws SQLException {
            return null;
        }

        @Override
        public void setTypeMap(java.util.Map<String, Class<?>> map) throws SQLException {
        }

        @Override
        public void setHoldability(int holdability) throws SQLException {
        }

        @Override
        public int getHoldability() throws SQLException {
            return 0;
        }

        @Override
        public java.sql.Savepoint setSavepoint() throws SQLException {
            return null;
        }

        @Override
        public java.sql.Savepoint setSavepoint(String name) throws SQLException {
            return null;
        }

        @Override
        public void rollback(java.sql.Savepoint savepoint) throws SQLException {
        }

        @Override
        public void releaseSavepoint(java.sql.Savepoint savepoint) throws SQLException {
        }

        @Override
        public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return null;
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return null;
        }

        @Override
        public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return null;
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return null;
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return null;
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return null;
        }

        @Override
        public java.sql.Clob createClob() throws SQLException {
            return null;
        }

        @Override
        public java.sql.Blob createBlob() throws SQLException {
            return null;
        }

        @Override
        public java.sql.NClob createNClob() throws SQLException {
            return null;
        }

        @Override
        public java.sql.SQLXML createSQLXML() throws SQLException {
            return null;
        }

        @Override
        public boolean isValid(int timeout) throws SQLException {
            return true;
        }

        @Override
        public void setClientInfo(String name, String value) throws java.sql.SQLClientInfoException {
        }

        @Override
        public void setClientInfo(java.util.Properties properties) throws java.sql.SQLClientInfoException {
        }

        @Override
        public String getClientInfo(String name) throws SQLException {
            return null;
        }

        @Override
        public java.util.Properties getClientInfo() throws SQLException {
            return null;
        }

        @Override
        public java.sql.Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return null;
        }

        @Override
        public java.sql.Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return null;
        }

        @Override
        public void setSchema(String schema) throws SQLException {
        }

        @Override
        public String getSchema() throws SQLException {
            return null;
        }

        @Override
        public void abort(java.util.concurrent.Executor executor) throws SQLException {
        }

        @Override
        public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) throws SQLException {
        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            return 0;
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }
    }
}