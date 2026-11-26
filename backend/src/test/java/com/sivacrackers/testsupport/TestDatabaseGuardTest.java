package com.sivacrackers.testsupport;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

class TestDatabaseGuardTest {

    @Test
    @DisplayName("Only local *_test databases are allowed; dev, production and remote URLs are refused")
    void urls() {
        assertThatCode(() -> TestDatabaseGuard.check("jdbc:mysql://localhost:3306/siva_crackers_test")).doesNotThrowAnyException();
        assertThatCode(() -> TestDatabaseGuard.check("jdbc:mysql://127.0.0.1/siva_crackers_test?useSSL=false")).doesNotThrowAnyException();
        for (String bad : new String[] {
                "jdbc:mysql://localhost:3306/siva_crackers",            // development database
                "jdbc:mysql://db.example.com:3306/siva_crackers_test",  // remote server
                "jdbc:mysql://localhost:3306/siva_crackers_test_backup", // name must END in _test
                "jdbc:mysql://localhost:3306/prod?x=_test",
                "jdbc:postgresql://localhost/siva_crackers_test",
                "", null}) {
            assertThatThrownBy(() -> TestDatabaseGuard.check(bad))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageNotContaining("siva_crackers\"");
        }
    }

    @Test
    @DisplayName("A Spring context with a connection pool for the development database fails to start")
    void blocksContext() {
        try (GenericApplicationContext ctx = new GenericApplicationContext()) {
            new TestDatabaseGuard().initialize(ctx);
            ctx.registerBean(HikariDataSource.class, () -> {
                HikariDataSource ds = new HikariDataSource();
                ds.setJdbcUrl("jdbc:mysql://localhost:3306/siva_crackers");
                return ds;
            });
            assertThatThrownBy(ctx::refresh).hasRootCauseInstanceOf(IllegalStateException.class);
        }
    }
}
