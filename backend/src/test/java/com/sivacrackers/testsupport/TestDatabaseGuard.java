package com.sivacrackers.testsupport;

import com.zaxxer.hikari.HikariDataSource;
import java.util.regex.Pattern;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Last line of defence for every Spring context started by a test (registered in
 * {@code src/test/resources/META-INF/spring.factories}, so no test can forget it):
 * a database connection pool may only point at a <b>localhost</b> MySQL database whose name ends in
 * {@code _test}. Anything else – the development database {@code siva_crackers}, a remote or production
 * server – stops the test before a single connection is opened.
 */
public class TestDatabaseGuard implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final Pattern SAFE_URL =
            Pattern.compile("^jdbc:mysql://(localhost|127\\.0\\.0\\.1)(:\\d+)?/[A-Za-z0-9_]+_test(\\?.*)?$");

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        context.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof HikariDataSource ds) {
                    check(ds.getJdbcUrl());
                }
                return bean;
            }
        });
    }

    /** Throws unless {@code url} is a local *_test MySQL database. The URL itself is never printed. */
    public static void check(String url) {
        if (url == null || !SAFE_URL.matcher(url).matches()) {
            throw new IllegalStateException("Refusing to start: tests may only use a localhost MySQL database whose name "
                    + "ends in _test (configure TEST_DB_URL; run them with backend/scripts/run-integration-tests.ps1).");
        }
    }
}
