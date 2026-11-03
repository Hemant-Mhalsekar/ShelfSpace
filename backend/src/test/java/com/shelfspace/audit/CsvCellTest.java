package com.shelfspace.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvCellTest {

    @Test
    void quotesAndEscapes() {
        assertThat(AuditController.cell("a, \"b\"")).isEqualTo("\"a, \"\"b\"\"\"");
        assertThat(AuditController.cell(null)).isEmpty();
    }

    @Test
    void neutralisesFormulaStarters() {
        assertThat(AuditController.cell("=1+1")).isEqualTo("\"'=1+1\"");
        assertThat(AuditController.cell("+1")).isEqualTo("\"'+1\"");
        assertThat(AuditController.cell("-1")).isEqualTo("\"'-1\"");
        assertThat(AuditController.cell("@SUM(A1)")).isEqualTo("\"'@SUM(A1)\"");
        assertThat(AuditController.cell("daysLate=2")).isEqualTo("\"daysLate=2\"");
    }
}
