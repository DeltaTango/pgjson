/**
 * Utility classes for the PgJson library.
 *
 * <p>This package provides cross-cutting infrastructure used by the service and repository layers:</p>
 *
 * <ul>
 *   <li>{@link io.github.deltatango.pgjson.util.DbUtil} -- HikariCP connection pool management</li>
 *   <li>{@link io.github.deltatango.pgjson.util.ManagedConnection} -- {@link AutoCloseable} wrapper
 *       for safe connection lifecycle management</li>
 *   <li>{@link io.github.deltatango.pgjson.util.JsonUtil} -- JSON object and array merge utilities (Gson-based)</li>
 *   <li>{@link io.github.deltatango.pgjson.util.ValidationUtil} -- JSON Schema 2020-12 validation with
 *       custom PgJson vocabulary support</li>
 *   <li>{@link io.github.deltatango.pgjson.util.SchemaCache} -- thread-safe compiled schema cache with
 *       hash-based invalidation</li>
 *   <li>{@link io.github.deltatango.pgjson.util.IndexUtil} -- extraction of index hints from JSON Schema
 *       {@code x-pgjson-index} vocabulary extensions</li>
 *   <li>{@link io.github.deltatango.pgjson.util.DateUtil} -- conversion between {@link java.time.LocalDateTime}
 *       and {@link java.sql.Timestamp}</li>
 *   <li>{@link io.github.deltatango.pgjson.util.FileUtil} -- safe file reading with path traversal protection</li>
 *   <li>{@link io.github.deltatango.pgjson.util.CircuitBreaker} -- circuit breaker state machine
 *       (CLOSED / OPEN / HALF_OPEN) for resilient database operations</li>
 *   <li>{@link io.github.deltatango.pgjson.util.RetryConfig} -- exponential backoff retry configuration
 *       with jitter support</li>
 *   <li>{@link io.github.deltatango.pgjson.util.DatabaseOperationExecutor} -- retry + circuit breaker
 *       orchestrator for database operations</li>
 * </ul>
 */
package io.github.deltatango.pgjson.util;
