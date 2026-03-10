/**
 * Repository (data access) layer for the PgJson library.
 *
 * <p>This package contains the classes responsible for direct database interaction
 * using JDBC {@link java.sql.PreparedStatement}:</p>
 *
 * <ul>
 *   <li>{@link io.github.deltatango.pgjson.model.repo.TableDefRepo} -- CRUD operations on the
 *       {@code tabledef} metadata table (schema definitions, indexes)</li>
 *   <li>{@link io.github.deltatango.pgjson.model.repo.DatabaseEntryRepo} -- CRUD and search operations
 *       on user-defined JSON data tables</li>
 *   <li>{@link io.github.deltatango.pgjson.model.repo.SearchTerm} -- a parameterized SQL search term
 *       with bound parameter values for safe query construction</li>
 * </ul>
 *
 * <p>All SQL identifiers (table names, column names) are validated against a safe pattern
 * to prevent SQL injection. Query values are always bound via parameterized statements.</p>
 */
package io.github.deltatango.pgjson.model.repo;
