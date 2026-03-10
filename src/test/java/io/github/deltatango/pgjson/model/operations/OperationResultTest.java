package io.github.deltatango.pgjson.model.operations;

import io.github.deltatango.pgjson.exceptions.PostgreJsonException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OperationResultTest {

    // ---- Factory methods ----

    @Test
    void success_createsSuccessVariant() {
        OperationResult<String> result = OperationResult.success("hello");
        assertInstanceOf(OperationResult.Success.class, result);
        assertEquals("hello", ((OperationResult.Success<String>) result).value());
    }

    @Test
    void success_allowsNullValue() {
        OperationResult<String> result = OperationResult.success(null);
        assertInstanceOf(OperationResult.Success.class, result);
        assertNull(((OperationResult.Success<String>) result).value());
    }

    @Test
    void notFound_createsNotFoundVariant() {
        OperationResult<String> result = OperationResult.notFound("entity 123");
        assertInstanceOf(OperationResult.NotFound.class, result);
        assertEquals("entity 123", ((OperationResult.NotFound<String>) result).detail());
    }

    @Test
    void error_createsErrorVariantWithCause() {
        RuntimeException cause = new RuntimeException("boom");
        OperationResult<String> result = OperationResult.error("db failed", cause);
        assertInstanceOf(OperationResult.Error.class, result);
        var error = (OperationResult.Error<String>) result;
        assertEquals("db failed", error.message());
        assertSame(cause, error.cause());
    }

    @Test
    void error_createsErrorVariantWithoutCause() {
        OperationResult<String> result = OperationResult.error("something wrong");
        assertInstanceOf(OperationResult.Error.class, result);
        var error = (OperationResult.Error<String>) result;
        assertEquals("something wrong", error.message());
        assertNull(error.cause());
    }

    // ---- Boolean query methods ----

    @Test
    void isSuccess_trueOnlyForSuccess() {
        assertTrue(OperationResult.success("x").isSuccess());
        assertFalse(OperationResult.notFound("x").isSuccess());
        assertFalse(OperationResult.error("x").isSuccess());
    }

    @Test
    void isNotFound_trueOnlyForNotFound() {
        assertFalse(OperationResult.success("x").isNotFound());
        assertTrue(OperationResult.notFound("x").isNotFound());
        assertFalse(OperationResult.error("x").isNotFound());
    }

    @Test
    void isError_trueOnlyForError() {
        assertFalse(OperationResult.success("x").isError());
        assertFalse(OperationResult.notFound("x").isError());
        assertTrue(OperationResult.error("x").isError());
    }

    // ---- getOrThrow ----

    @Test
    void getOrThrow_returnsValueOnSuccess() throws PostgreJsonException {
        assertEquals("hello", OperationResult.success("hello").getOrThrow());
    }

    @Test
    void getOrThrow_throwsOnNotFound() {
        OperationResult<String> result = OperationResult.notFound("user 42");
        PostgreJsonException ex = assertThrows(PostgreJsonException.class, result::getOrThrow);
        assertTrue(ex.getMessage().contains("user 42"));
    }

    @Test
    void getOrThrow_throwsOnErrorWithCause() {
        RuntimeException cause = new RuntimeException("root");
        OperationResult<String> result = OperationResult.error("db error", cause);
        PostgreJsonException ex = assertThrows(PostgreJsonException.class, result::getOrThrow);
        assertEquals("db error", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void getOrThrow_throwsOnErrorWithoutCause() {
        OperationResult<String> result = OperationResult.error("oops");
        PostgreJsonException ex = assertThrows(PostgreJsonException.class, result::getOrThrow);
        assertEquals("oops", ex.getMessage());
        assertNull(ex.getCause());
    }

    // ---- toOptional ----

    @Test
    void toOptional_presentOnSuccess() {
        assertEquals(Optional.of("val"), OperationResult.success("val").toOptional());
    }

    @Test
    void toOptional_emptyOnSuccessWithNullValue() {
        assertEquals(Optional.empty(), OperationResult.success(null).toOptional());
    }

    @Test
    void toOptional_emptyOnNotFound() {
        assertEquals(Optional.empty(), OperationResult.notFound("x").toOptional());
    }

    @Test
    void toOptional_emptyOnError() {
        assertEquals(Optional.empty(), OperationResult.error("x").toOptional());
    }

    // ---- Pattern matching (exhaustive switch) ----

    @Test
    void patternMatching_coversAllVariants() {
        OperationResult<List<String>> success = OperationResult.success(List.of("a", "b"));
        OperationResult<List<String>> notFound = OperationResult.notFound("no list");
        OperationResult<List<String>> error = OperationResult.error("fail", new RuntimeException());

        assertEquals("success:2", describe(success));
        assertEquals("notFound:no list", describe(notFound));
        assertTrue(describe(error).startsWith("error:fail"));
    }

    private String describe(OperationResult<List<String>> result) {
        return switch (result) {
            case OperationResult.Success<List<String>> s -> "success:" + s.value().size();
            case OperationResult.NotFound<List<String>> n -> "notFound:" + n.detail();
            case OperationResult.Error<List<String>> e -> "error:" + e.message();
        };
    }
}
