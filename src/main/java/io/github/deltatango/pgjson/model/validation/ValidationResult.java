package io.github.deltatango.pgjson.model.validation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of validating a JSON document against a JSON Schema 2020-12 schema.
 *
 * <p>A {@code true} {@link #validationStatus} indicates the document is valid.
 * When validation fails, {@link #validationMessage} contains a human-readable
 * description of the validation errors.</p>
 *
 * @see io.github.deltatango.pgjson.util.ValidationUtil
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationResult {
    /** {@code true} if the document passed validation, {@code false} otherwise. */
    private Boolean validationStatus;
    /** Human-readable validation message; contains error details when validation fails. */
    private String validationMessage;
}
