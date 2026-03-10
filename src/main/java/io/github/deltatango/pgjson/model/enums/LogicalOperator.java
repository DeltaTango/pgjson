package io.github.deltatango.pgjson.model.enums;

/**
 * Logical operators for combining multiple search terms in a query.
 *
 * <p>When a search JSON contains multiple fields in the {@code searchTerm} object,
 * the logical operator determines how the resulting SQL {@code WHERE} clauses are joined.</p>
 *
 * <p>The operator is specified in the search JSON via the {@code "logicalOperator"} field.
 * Defaults to {@link #and} when not specified.</p>
 *
 * @see io.github.deltatango.pgjson.model.repo.DatabaseEntryRepo
 */
public enum LogicalOperator {
    /** Matches entries that satisfy <em>any</em> of the search terms ({@code WHERE a OR b}). */
    or("OR"),
    /** Matches entries that satisfy <em>all</em> of the search terms ({@code WHERE a AND b}). */
    and("AND");

    private final String propertyName;

    LogicalOperator(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * Returns the SQL keyword for this operator.
     *
     * @return {@code "AND"} or {@code "OR"}
     */
    public String getPropertyName() {
        return this.propertyName;
    }
}
