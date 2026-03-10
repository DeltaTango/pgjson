package io.github.deltatango.pgjson.model.operations;

import io.github.deltatango.pgjson.model.validation.ValidationResult;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Detailed result payload for a data operation (insert, update, merge, or delete).
 *
 * <p>Returned inside an {@link OperationResult.Success} when a data operation completes.
 * Contains the affected entry's UUID, the final JSON data after the operation, and
 * the outcome of schema validation.</p>
 */
@Data
@AllArgsConstructor
public class Result {
    /** UUID of the affected database entry. */
    private String iduuid;
    /** The final JSON data after the operation (post-merge, post-validation). */
    private String finalJsonData;
    /** Name of the data table the operation was performed on. */
    private String tableName;
    /** Schema validation outcome; {@code null} if validation was not applicable. */
    private ValidationResult validationResult;
    /** {@code true} if the operation succeeded, {@code false} otherwise. */
    private Boolean resultStatus;
    /** Human-readable message describing the operation outcome. */
    private String resultMessage;
}
