package com.sivacrackers.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CsvTest {

    @Test
    @DisplayName("CSV cells: quoting, numbers as-is, and spreadsheet formulas neutralised")
    void cells() {
        assertThat(Csv.cell("plain")).isEqualTo("plain");
        assertThat(Csv.cell("a, b")).isEqualTo("\"a, b\"");
        assertThat(Csv.cell("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(Csv.cell("=SUM(A1:A2)")).isEqualTo("'=SUM(A1:A2)");
        assertThat(Csv.cell("+91 98765")).isEqualTo("'+91 98765");
        assertThat(Csv.cell("-cmd")).isEqualTo("'-cmd");
        assertThat(Csv.cell("@x")).isEqualTo("'@x");
        assertThat(Csv.cell(-5)).isEqualTo("-5");
        assertThat(Csv.cell(new BigDecimal("1250.50"))).isEqualTo("1250.50");
        assertThat(Csv.cell(null)).isEmpty();
        assertThat(new Csv().row("a", 1).row("b,c", null).toString()).isEqualTo("﻿a,1\r\n\"b,c\",\r\n");
    }
}
