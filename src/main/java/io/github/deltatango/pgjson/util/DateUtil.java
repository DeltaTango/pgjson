package io.github.deltatango.pgjson.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Utility for converting between {@link LocalDateTime} and {@link Timestamp}.
 *
 * <p>Used by the repository layer to map Java date/time values to and from
 * PostgreSQL {@code TIMESTAMP} columns.</p>
 */
public class DateUtil {

    /**
     * Converts a {@link LocalDateTime} to a JDBC {@link Timestamp}.
     *
     * @param localDateTime the local date-time to convert
     * @return the equivalent SQL timestamp
     */
    public Timestamp localDateTimeToSqlTimestamp(LocalDateTime localDateTime) {
        return Timestamp.valueOf(localDateTime);
    }

    /**
     * Converts a JDBC {@link Timestamp} to a {@link LocalDateTime}.
     *
     * @param timestamp the SQL timestamp to convert, or {@code null}
     * @return the equivalent local date-time, or {@code null} if the input is {@code null}
     */
    public LocalDateTime sqlTimestampToLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime();
    }
}
