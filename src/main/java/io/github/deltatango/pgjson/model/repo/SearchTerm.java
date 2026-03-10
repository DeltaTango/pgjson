package io.github.deltatango.pgjson.model.repo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * A parameterized SQL search term with bound parameter values.
 *
 * <p>Instances are built by {@link DatabaseEntryRepo#createTerm} from a parsed search JSON.
 * The {@link #searchTerm} contains the SQL {@code WHERE} clause fragment with {@code ?}
 * placeholders, and {@link #parameters} holds the corresponding values to bind safely
 * via {@link java.sql.PreparedStatement}.</p>
 *
 * @see DatabaseEntryRepo
 */
@Data
@NoArgsConstructor
public class SearchTerm {
    /** SQL {@code WHERE} clause fragment with {@code ?} placeholders (e.g., {@code "json_data->>'email'=?"}). */
    String searchTerm;
    /** Maximum number of rows to return, or {@code null} for unlimited (e.g., array queries). */
    Integer limit;
    /** Ordered list of parameter values to bind to the {@code ?} placeholders in {@link #searchTerm}. */
    List<Object> parameters = new ArrayList<>();

    /**
     * Creates a search term with no bound parameters.
     *
     * @param searchTerm the SQL WHERE clause fragment
     * @param limit      maximum rows to return, or {@code null} for unlimited
     */
    public SearchTerm(String searchTerm, Integer limit) {
        this.searchTerm = searchTerm;
        this.limit = limit;
        this.parameters = new ArrayList<>();
    }

    /**
     * Creates a search term with bound parameters.
     *
     * @param searchTerm the SQL WHERE clause fragment with {@code ?} placeholders
     * @param limit      maximum rows to return, or {@code null} for unlimited
     * @param parameters ordered list of values to bind; {@code null} is treated as empty
     */
    public SearchTerm(String searchTerm, Integer limit, List<Object> parameters) {
        this.searchTerm = searchTerm;
        this.limit = limit;
        this.parameters = parameters != null ? parameters : new ArrayList<>();
    }
}
