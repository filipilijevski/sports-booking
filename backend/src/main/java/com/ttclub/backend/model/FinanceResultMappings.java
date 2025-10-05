package com.ttclub.backend.model;

import com.ttclub.backend.dto.FinanceRowDto;
import jakarta.persistence.*;

/**
 * Central place for native‐query result-set mappings used by the finance
 * exports.  The class is *not* an entity - we mark it as {@code @MappedSuperclass}
 * so Hibernate picks it up during bootstrap without generating any DDL.
 */
@MappedSuperclass                 // scanned, but no table generated
@SqlResultSetMapping(
        name = "FinanceRowMapping",
        classes = @ConstructorResult(
                targetClass = FinanceRowDto.class,
                columns = {
                        @ColumnResult(name = "id",       type = Long.class),
                        @ColumnResult(name = "ts",       type = java.sql.Timestamp.class),
                        @ColumnResult(name = "action",   type = String.class),
                        @ColumnResult(name = "subtotal", type = java.math.BigDecimal.class),
                        @ColumnResult(name = "shipping", type = java.math.BigDecimal.class),
                        @ColumnResult(name = "tax",      type = java.math.BigDecimal.class),
                        @ColumnResult(name = "total",    type = java.math.BigDecimal.class),
                        @ColumnResult(name = "status",   type = String.class)
                }
        )
)
public abstract class FinanceResultMappings {
    /* empty - declaration class only */
}
