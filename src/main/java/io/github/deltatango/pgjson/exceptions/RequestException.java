package io.github.deltatango.pgjson.exceptions;

/**
 * Exception thrown when request validation fails.
 *
 * <p>This exception is thrown for client request validation errors, such as
 * invalid JSON format, missing required fields, or malformed search criteria.
 * It provides detailed error information about what went wrong with the request.</p>
 *
 * <h2>Common Causes:</h2>
 * <ul>
 *   <li>Invalid JSON format in request data</li>
 *   <li>Missing required fields (limit, offset, etc.)</li>
 *   <li>Invalid field values (negative limits, etc.)</li>
 *   <li>Malformed search criteria</li>
 *   <li>Schema validation failures</li>
 * </ul>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * try {
 *     client.selectData("users", searchJson);
 * } catch (RequestException e) {
 *     System.err.println("Request validation failed: " + e.getMessage());
 *     // Handle invalid request
 * }
 * }</pre>
 *
 * @author PostgreSQL JSON Client Team
 * @version 25.10.1
 * @since 1.0.0
 * @see Exception
 * @see PostgreJsonException
 */
public class RequestException extends Exception {

    /**
     * Constructs a new RequestException with the specified detail message.
     *
     * @param exceptionJson the detail message explaining the validation failure
     */
    public RequestException(String exceptionJson) {
        super(exceptionJson);
    }
}
