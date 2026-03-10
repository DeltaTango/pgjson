package io.github.deltatango.pgjson.exceptions;

/**
 * Exception thrown when PostgreSQL JSON operations fail.
 *
 * <p>This exception is thrown for database-related errors, connection issues,
 * and other PostgreSQL JSON Client operation failures. It provides detailed
 * error information and supports exception chaining for debugging.</p>
 *
 * <h2>Common Causes:</h2>
 * <ul>
 *   <li>Database connection failures</li>
 *   <li>SQL execution errors</li>
 *   <li>Schema validation failures</li>
 *   <li>Resource management issues</li>
 *   <li>Configuration problems</li>
 * </ul>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * try {
 *     client.insertData("users", jsonData);
 * } catch (PostgreJsonException e) {
 *     System.err.println("Database error: " + e.getMessage());
 *     if (e.getCause() != null) {
 *         System.err.println("Root cause: " + e.getCause().getMessage());
 *     }
 * }
 * }</pre>
 *
 * @author PostgreSQL JSON Client Team
 * @version 25.10.1
 * @since 1.0.0
 * @see Exception
 */
public class PostgreJsonException extends Exception {

    /**
     * Constructs a new PostgreJsonException with the specified detail message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public PostgreJsonException(String message) {
        super(message);
    }

    /**
     * Constructs a new PostgreJsonException with the specified detail message and cause.
     *
     * <p>This constructor is useful for exception chaining, allowing the original
     * exception to be preserved for debugging purposes.</p>
     *
     * @param message the detail message explaining the cause of the exception
     * @param cause the cause of this exception (which is saved for later retrieval by the getCause() method)
     */
    public PostgreJsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
