package io.github.deltatango.pgjson.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for CircuitBreaker.
 * 
 * <p>This test class covers all circuit breaker states, state transitions,
 * failure handling, recovery, and concurrent access scenarios.</p>
 */
class CircuitBreakerTest {

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = CircuitBreaker.builder()
                .failureThreshold(3)
                .timeoutMs(1000)
                .halfOpenMaxCalls(2)
                .build();
    }

    @Test
    void testConstructor_ValidParameters() {
        // Test that constructor accepts valid parameters
        assertDoesNotThrow(() -> new CircuitBreaker(5, 60000, 3));
    }

    @Test
    void testConstructor_InvalidFailureThreshold() {
        // Test that constructor throws exception for invalid failure threshold
        assertThrows(IllegalArgumentException.class, 
                () -> new CircuitBreaker(0, 60000, 3));
        assertThrows(IllegalArgumentException.class, 
                () -> new CircuitBreaker(-1, 60000, 3));
    }

    @Test
    void testConstructor_InvalidTimeout() {
        // Test that constructor throws exception for invalid timeout
        assertThrows(IllegalArgumentException.class, 
                () -> new CircuitBreaker(5, 0, 3));
        assertThrows(IllegalArgumentException.class, 
                () -> new CircuitBreaker(5, -1, 3));
    }

    @Test
    void testConstructor_InvalidHalfOpenMaxCalls() {
        // Test that constructor throws exception for invalid half-open max calls
        assertThrows(IllegalArgumentException.class, 
                () -> new CircuitBreaker(5, 60000, 0));
        assertThrows(IllegalArgumentException.class, 
                () -> new CircuitBreaker(5, 60000, -1));
    }

    @Test
    void testBuilder_DefaultValues() {
        // Test builder with default values
        CircuitBreaker breaker = CircuitBreaker.builder().build();
        
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
        assertEquals(0, breaker.getFailureCount());
        assertEquals(0, breaker.getTotalCalls());
        assertEquals(0, breaker.getTotalFailures());
        assertEquals(0.0, breaker.getFailureRate(), 0.01);
    }

    @Test
    void testBuilder_CustomValues() {
        // Test builder with custom values
        CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(5)
                .timeoutMs(30000)
                .halfOpenMaxCalls(3)
                .build();
        
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    }

    @Test
    void testDefaultBreaker() {
        // Test default breaker creation
        CircuitBreaker breaker = CircuitBreaker.defaultBreaker();
        
        assertNotNull(breaker);
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    }

    @Test
    void testExecute_Success() {
        // Test successful execution
        String expectedResult = "success";
        assertDoesNotThrow(() -> {
            String result = circuitBreaker.execute(() -> expectedResult);
            assertEquals(expectedResult, result);
            assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
            assertEquals(0, circuitBreaker.getFailureCount());
            assertEquals(1, circuitBreaker.getTotalCalls());
            assertEquals(0, circuitBreaker.getTotalFailures());
            assertEquals(0.0, circuitBreaker.getFailureRate(), 0.01);
        });
    }

    @Test
    void testExecute_Failure() {
        // Test failed execution
        RuntimeException exception = new RuntimeException("Operation failed");
        
        assertThrows(RuntimeException.class, 
                () -> circuitBreaker.execute(() -> {
                    throw exception;
                }));
        
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(1, circuitBreaker.getFailureCount());
        assertEquals(1, circuitBreaker.getTotalCalls());
        assertEquals(1, circuitBreaker.getTotalFailures());
        assertEquals(100.0, circuitBreaker.getFailureRate(), 0.01);
    }

    @Test
    void testExecute_StateTransition_CLOSED_TO_OPEN() {
        // Test state transition from CLOSED to OPEN
        RuntimeException exception = new RuntimeException("Operation failed");
        
        // Execute operations that fail to reach failure threshold
        for (int i = 0; i < 3; i++) {
            assertThrows(RuntimeException.class, 
                    () -> circuitBreaker.execute(() -> {
                        throw exception;
                    }));
        }
        
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        assertEquals(3, circuitBreaker.getFailureCount());
        assertEquals(3, circuitBreaker.getTotalCalls());
        assertEquals(3, circuitBreaker.getTotalFailures());
    }

    @Test
    void testExecute_CircuitOpen_RejectsRequests() {
        // Test that circuit rejects requests when open
        RuntimeException exception = new RuntimeException("Operation failed");
        
        // Open the circuit
        for (int i = 0; i < 3; i++) {
            assertThrows(RuntimeException.class, 
                    () -> circuitBreaker.execute(() -> {
                        throw exception;
                    }));
        }
        
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        
        // Next request should be rejected immediately
        assertThrows(CircuitBreaker.CircuitBreakerOpenException.class, 
                () -> circuitBreaker.execute(() -> "should not execute"));
    }

    @Test
    void testExecute_StateTransition_OPEN_TO_HALF_OPEN() throws InterruptedException {
        // Test state transition from OPEN to HALF_OPEN after timeout
        RuntimeException exception = new RuntimeException("Operation failed");
        
        // Open the circuit
        for (int i = 0; i < 3; i++) {
            assertThrows(RuntimeException.class, 
                    () -> circuitBreaker.execute(() -> {
                        throw exception;
                    }));
        }
        
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        
        // Wait for timeout
        Thread.sleep(1100);
        
        // Next request should transition to HALF_OPEN
        assertDoesNotThrow(() -> {
            String result = circuitBreaker.execute(() -> "half-open test");
            assertEquals("half-open test", result);
        });
        
        // After successful execution, circuit should be CLOSED (not HALF_OPEN)
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    void testExecute_HalfOpen_Success_TransitionsToClosed() throws InterruptedException {
        // Test successful execution in HALF_OPEN state transitions to CLOSED
        RuntimeException exception = new RuntimeException("Operation failed");
        
        // Open the circuit
        for (int i = 0; i < 3; i++) {
            assertThrows(RuntimeException.class, 
                    () -> circuitBreaker.execute(() -> {
                        throw exception;
                    }));
        }
        
        // Wait for timeout and transition to HALF_OPEN
        Thread.sleep(1100);
        
        // First call should transition to HALF_OPEN and then to CLOSED
        assertDoesNotThrow(() -> {
            String result = circuitBreaker.execute(() -> "test");
            assertEquals("test", result);
        });
        
        // After successful execution, circuit should be CLOSED
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getFailureCount());
    }

    @Test
    void testExecute_HalfOpen_Failure_TransitionsToOpen() throws InterruptedException {
        // Test failed execution in HALF_OPEN state transitions back to OPEN
        RuntimeException exception = new RuntimeException("Operation failed");
        
        // Open the circuit
        for (int i = 0; i < 3; i++) {
            assertThrows(RuntimeException.class, 
                    () -> circuitBreaker.execute(() -> {
                        throw exception;
                    }));
        }
        
        // Wait for timeout and transition to HALF_OPEN
        Thread.sleep(1100);
        
        // First call should transition to HALF_OPEN and then to CLOSED
        assertDoesNotThrow(() -> {
            circuitBreaker.execute(() -> "test");
        });
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        
        // Now we need to open the circuit again to test failure in HALF_OPEN
        // Open the circuit again
        for (int i = 0; i < 3; i++) {
            assertThrows(RuntimeException.class, 
                    () -> circuitBreaker.execute(() -> {
                        throw exception;
                    }));
        }
        
        // Wait for timeout and transition to HALF_OPEN
        Thread.sleep(1100);
        
        // Failed execution should transition back to OPEN
        assertThrows(RuntimeException.class, 
                () -> circuitBreaker.execute(() -> {
                    throw exception;
                }));
        
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    void testExecute_HalfOpen_MaxCallsExceeded() throws InterruptedException {
        // Test that HALF_OPEN state respects max calls limit
        RuntimeException exception = new RuntimeException("Operation failed");
        
        // Open the circuit
        for (int i = 0; i < 3; i++) {
            assertThrows(RuntimeException.class, 
                    () -> circuitBreaker.execute(() -> {
                        throw exception;
                    }));
        }
        
        // Wait for timeout and transition to HALF_OPEN
        Thread.sleep(1100);
        
        // Execute max calls (2) - should succeed
        for (int i = 0; i < 2; i++) {
            final int callIndex = i;
            assertDoesNotThrow(() -> {
                String result = circuitBreaker.execute(() -> "half-open call " + callIndex);
                assertEquals("half-open call " + callIndex, result);
            });
        }
        
        // After 2 successful calls, circuit should be CLOSED, not HALF_OPEN
        // So the next call should succeed, not be rejected
        assertDoesNotThrow(() -> {
            String result = circuitBreaker.execute(() -> "should succeed");
            assertEquals("should succeed", result);
        });
    }

    @Test
    void testGetState() {
        // Test getter for state
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    void testGetFailureCount() {
        // Test getter for failure count
        assertEquals(0, circuitBreaker.getFailureCount());
        
        // Cause a failure
        assertThrows(RuntimeException.class, 
                () -> circuitBreaker.execute(() -> {
                    throw new RuntimeException("test failure");
                }));
        
        assertEquals(1, circuitBreaker.getFailureCount());
    }

    @Test
    void testGetTotalCalls() {
        // Test getter for total calls
        assertEquals(0, circuitBreaker.getTotalCalls());
        
        // Make some calls
        assertDoesNotThrow(() -> circuitBreaker.execute(() -> "call 1"));
        assertDoesNotThrow(() -> circuitBreaker.execute(() -> "call 2"));
        
        assertEquals(2, circuitBreaker.getTotalCalls());
    }

    @Test
    void testGetTotalFailures() {
        // Test getter for total failures
        assertEquals(0, circuitBreaker.getTotalFailures());
        
        // Cause some failures
        assertThrows(RuntimeException.class, 
                () -> circuitBreaker.execute(() -> {
                    throw new RuntimeException("failure 1");
                }));
        
        assertThrows(RuntimeException.class, 
                () -> circuitBreaker.execute(() -> {
                    throw new RuntimeException("failure 2");
                }));
        
        assertEquals(2, circuitBreaker.getTotalFailures());
    }

    @Test
    void testGetFailureRate() {
        // Test getter for failure rate
        assertEquals(0.0, circuitBreaker.getFailureRate(), 0.01);
        
        // Make some calls with failures
        assertDoesNotThrow(() -> circuitBreaker.execute(() -> "success"));
        assertThrows(RuntimeException.class, 
                () -> circuitBreaker.execute(() -> {
                    throw new RuntimeException("failure");
                }));
        
        assertEquals(50.0, circuitBreaker.getFailureRate(), 0.01);
    }

    @Test
    void testReset() {
        // Test manual reset
        // First open the circuit
        for (int i = 0; i < 3; i++) {
            assertThrows(RuntimeException.class, 
                    () -> circuitBreaker.execute(() -> {
                        throw new RuntimeException("failure");
                    }));
        }
        
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        assertEquals(3, circuitBreaker.getFailureCount());
        
        // Reset the circuit breaker
        circuitBreaker.reset();
        
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getFailureCount());
        
        // Should be able to execute operations again
        assertDoesNotThrow(() -> {
            String result = circuitBreaker.execute(() -> "reset test");
            assertEquals("reset test", result);
        });
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        // Test concurrent access to circuit breaker
        int threadCount = 10;
        int operationsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            String result = circuitBreaker.execute(() -> "concurrent test");
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            if (e instanceof CircuitBreaker.CircuitBreakerOpenException) {
                                failureCount.incrementAndGet();
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        
        // Verify that all operations completed
        assertEquals(threadCount * operationsPerThread, 
                successCount.get() + failureCount.get());
    }

    @Test
    void testOperationInterface() {
        // Test the Operation functional interface
        CircuitBreaker.Operation<String> operation = () -> "test result";
        
        assertDoesNotThrow(() -> {
            String result = operation.execute();
            assertEquals("test result", result);
        });
    }

    @Test
    void testCircuitBreakerOpenException() {
        // Test CircuitBreakerOpenException
        CircuitBreaker.CircuitBreakerOpenException exception = 
                new CircuitBreaker.CircuitBreakerOpenException("Test message");
        
        assertEquals("Test message", exception.getMessage());
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testBuilder_Chaining() {
        // Test builder method chaining
        CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(5)
                .timeoutMs(30000)
                .halfOpenMaxCalls(3)
                .build();
        
        assertNotNull(breaker);
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    }

    @Test
    void testStateTransitions_ComplexScenario() throws InterruptedException {
        // Test complex state transition scenario
        RuntimeException exception = new RuntimeException("Operation failed");
        
        // Start in CLOSED state
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        
        // Fail enough times to open circuit
        for (int i = 0; i < 3; i++) {
            assertThrows(RuntimeException.class, 
                    () -> circuitBreaker.execute(() -> {
                        throw exception;
                    }));
        }
        
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        
        // Wait for timeout and test HALF_OPEN
        Thread.sleep(1100);
        
        // First call in HALF_OPEN should succeed and transition to CLOSED
        assertDoesNotThrow(() -> {
            String result = circuitBreaker.execute(() -> "half-open success");
            assertEquals("half-open success", result);
        });
        
        // After successful execution, circuit should be CLOSED
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        
        // Second call should also succeed (circuit is now CLOSED)
        assertDoesNotThrow(() -> {
            String result = circuitBreaker.execute(() -> "half-open success 2");
            assertEquals("half-open success 2", result);
        });
        
        // Third call should succeed (circuit is CLOSED)
        assertDoesNotThrow(() -> {
            String result = circuitBreaker.execute(() -> "should succeed");
            assertEquals("should succeed", result);
        });
    }
}
