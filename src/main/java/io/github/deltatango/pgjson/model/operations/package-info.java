/**
 * Operation result types for the PgJson library.
 *
 * <p>This package provides the result abstractions returned by all public API methods:</p>
 *
 * <ul>
 *   <li>{@link io.github.deltatango.pgjson.model.operations.OperationResult} -- a sealed type with three variants:
 *       {@code Success}, {@code NotFound}, and {@code Error}</li>
 *   <li>{@link io.github.deltatango.pgjson.model.operations.Result} -- detailed result payload for data
 *       operations (insert, update, delete) containing the affected entry's UUID, final JSON data,
 *       table name, validation result, and status</li>
 * </ul>
 */
package io.github.deltatango.pgjson.model.operations;
