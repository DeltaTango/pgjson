/**
 * Domain model classes for the PgJson library.
 *
 * <p>This package contains the core data transfer objects that represent
 * database entities and their metadata:</p>
 *
 * <ul>
 *   <li>{@link io.github.deltatango.pgjson.model.TableDef} -- schema definition stored in the {@code tabledef} table</li>
 *   <li>{@link io.github.deltatango.pgjson.model.DatabaseEntry} -- a JSON document stored in a user-defined table</li>
 *   <li>{@link io.github.deltatango.pgjson.model.IndexInfo} -- metadata about a PostgreSQL GIN index on JSON data</li>
 * </ul>
 *
 * <p>All model classes use Lombok {@code @Data} for boilerplate reduction.</p>
 *
 * @see io.github.deltatango.pgjson.model.enums
 * @see io.github.deltatango.pgjson.model.operations
 * @see io.github.deltatango.pgjson.model.repo
 */
package io.github.deltatango.pgjson.model;
