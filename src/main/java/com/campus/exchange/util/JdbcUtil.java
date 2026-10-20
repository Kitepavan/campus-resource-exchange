package com.campus.exchange.util;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Small JDBC mapping helpers. Keeps nullable timestamp/column handling in one place.
 */
public final class JdbcUtil {

    private JdbcUtil() {
    }

    public static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    public static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    public static Integer toInteger(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).intValue();
    }

    public static BigDecimal toBigDecimal(ResultSet rs, String column) throws SQLException {
        return rs.getBigDecimal(column);
    }
}
