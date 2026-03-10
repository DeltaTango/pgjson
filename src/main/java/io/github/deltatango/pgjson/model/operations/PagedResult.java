package io.github.deltatango.pgjson.model.operations;

import java.util.List;

/**
 * A paginated result set containing items and total count metadata.
 *
 * <p>Returned by {@code selectDataWithCount} methods to provide consumers with
 * the information needed to build pagination UI: the current page of items,
 * the total number of matching rows, and the limit/offset used for the query.</p>
 *
 * @param items      the items in the current page
 * @param totalCount the total number of rows matching the query (before limit/offset)
 * @param limit      the maximum number of items per page
 * @param offset     the number of items skipped
 * @param <T>        the type of items in the page
 */
public record PagedResult<T>(List<T> items, long totalCount, int limit, int offset) {
}
