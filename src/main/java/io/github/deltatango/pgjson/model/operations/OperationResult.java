package io.github.deltatango.pgjson.model.operations;

import io.github.deltatango.pgjson.exceptions.PostgreJsonException;

import java.util.Optional;

/**
 * A sealed result type that represents the outcome of a database operation.
 *
 * <p>Every operation returns one of three variants:</p>
 * <ul>
 *   <li>{@link Success} -- the operation completed and produced a value.</li>
 *   <li>{@link NotFound} -- the requested entity or resource does not exist.</li>
 *   <li>{@link Error} -- an infrastructure or unexpected error occurred.</li>
 * </ul>
 *
 * <h2>Usage with pattern matching (Java 21+):</h2>
 * <pre>{@code
 * OperationResult<TableDef> result = repo.selectTableDefByUuid(conn, uuid);
 * switch (result) {
 *     case OperationResult.Success<TableDef> s -> process(s.value());
 *     case OperationResult.NotFound<TableDef> n -> log.info("Not found: {}", n.detail());
 *     case OperationResult.Error<TableDef> e   -> handleError(e.message(), e.cause());
 * }
 * }</pre>
 *
 * <h2>Usage with convenience methods:</h2>
 * <pre>{@code
 * TableDef td = result.getOrThrow();          // unwrap or throw PostgreJsonException
 * Optional<TableDef> opt = result.toOptional(); // bridge to Optional
 * }</pre>
 *
 * @param <T> the type of the successful result value
 * @author PostgreSQL JSON Client Team
 * @version 26.2.1
 * @since 26.2.1
 */
public sealed interface OperationResult<T> permits
        OperationResult.Success,
        OperationResult.NotFound,
        OperationResult.Error {

    // ---- Variants ----

    /**
     * The operation completed successfully and produced a value.
     *
     * @param value the result value (may be an empty collection for list queries with no matches)
     * @param <T>   the type of the value
     */
    record Success<T>(T value) implements OperationResult<T> {}

    /**
     * The requested entity or resource was not found.
     *
     * @param detail a human-readable description of what was not found
     * @param <T>    the expected result type (phantom)
     */
    record NotFound<T>(String detail) implements OperationResult<T> {}

    /**
     * An infrastructure or unexpected error occurred.
     *
     * @param message a human-readable error description
     * @param cause   the underlying throwable (may be {@code null})
     * @param <T>     the expected result type (phantom)
     */
    record Error<T>(String message, Throwable cause) implements OperationResult<T> {}

    // ---- Static factories ----

    /**
     * Creates a successful result.
     *
     * @param value the result value
     * @param <T>   the type of the value
     * @return a {@link Success} containing the value
     */
    static <T> OperationResult<T> success(T value) {
        return new Success<>(value);
    }

    /**
     * Creates a not-found result.
     *
     * @param detail description of what was not found
     * @param <T>    the expected result type
     * @return a {@link NotFound} with the given detail
     */
    static <T> OperationResult<T> notFound(String detail) {
        return new NotFound<>(detail);
    }

    /**
     * Creates an error result.
     *
     * @param message error description
     * @param cause   the underlying throwable (may be {@code null})
     * @param <T>     the expected result type
     * @return an {@link Error} with the given message and cause
     */
    static <T> OperationResult<T> error(String message, Throwable cause) {
        return new Error<>(message, cause);
    }

    /**
     * Creates an error result without a cause.
     *
     * @param message error description
     * @param <T>     the expected result type
     * @return an {@link Error} with the given message and no cause
     */
    static <T> OperationResult<T> error(String message) {
        return new Error<>(message, null);
    }

    // ---- Convenience query methods ----

    /**
     * Returns {@code true} if this result is a {@link Success}.
     *
     * @return {@code true} for success, {@code false} otherwise
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * Returns {@code true} if this result is a {@link NotFound}.
     *
     * @return {@code true} for not-found, {@code false} otherwise
     */
    default boolean isNotFound() {
        return this instanceof NotFound;
    }

    /**
     * Returns {@code true} if this result is an {@link Error}.
     *
     * @return {@code true} for error, {@code false} otherwise
     */
    default boolean isError() {
        return this instanceof Error;
    }

    // ---- Unwrap methods ----

    /**
     * Returns the value if this is a {@link Success}, otherwise throws {@link PostgreJsonException}.
     *
     * @return the success value
     * @throws PostgreJsonException if this result is {@link NotFound} or {@link Error}
     */
    default T getOrThrow() throws PostgreJsonException {
        return switch (this) {
            case Success<T> s -> s.value();
            case NotFound<T> n -> throw new PostgreJsonException("Not found: " + n.detail());
            case Error<T> e -> throw new PostgreJsonException(e.message(), e.cause());
        };
    }

    /**
     * Converts this result to an {@link Optional}.
     *
     * <p>Maps {@link Success} to {@link Optional#of(Object)} (or {@link Optional#empty()} if
     * the value is {@code null}), and both {@link NotFound} and {@link Error} to
     * {@link Optional#empty()}.</p>
     *
     * @return an Optional containing the value on success, empty otherwise
     */
    default Optional<T> toOptional() {
        return switch (this) {
            case Success<T> s -> Optional.ofNullable(s.value());
            case NotFound<T> n -> Optional.empty();
            case Error<T> e -> Optional.empty();
        };
    }
}
