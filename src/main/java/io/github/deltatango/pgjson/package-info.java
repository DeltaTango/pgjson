/**
 * PgJson -- a document-oriented JSON storage library on top of PostgreSQL.
 *
 * <p>This is the root package of the PgJson library. The main entry point is
 * {@link io.github.deltatango.pgjson.PostgreSqlJsonClient}, a facade that delegates
 * to focused internal services:</p>
 *
 * <ul>
 *   <li>{@code SchemaService} -- JSON Schema management and validation</li>
 *   <li>{@code DataService} -- CRUD operations on JSON documents</li>
 *   <li>{@code SearchService} -- search and select with parameterized queries</li>
 *   <li>{@code UiLabelService} -- UI label extraction from schema vocabulary extensions</li>
 * </ul>
 *
 * <p>Internal services are package-private and not part of the public API.
 * All public access goes through {@code PostgreSqlJsonClient}.</p>
 *
 * @see io.github.deltatango.pgjson.PostgreSqlJsonClient
 */
package io.github.deltatango.pgjson;
