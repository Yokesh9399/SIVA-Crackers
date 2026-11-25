package com.sivacrackers.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds the demo catalogue at startup – ONLY when {@code APP_SEED_DEMO=true}.
 * With the default ({@code false}) this bean does not even exist.
 */
@Component
@ConditionalOnProperty(prefix = "app", name = "seed-demo", havingValue = "true")
public class DemoDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataRunner.class);

    private final DemoDataSeeder seeder;

    public DemoDataRunner(DemoDataSeeder seeder) {
        this.seeder = seeder;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.warn("APP_SEED_DEMO=true: loading DEMONSTRATION catalogue data (marked is_demo). "
                + "Turn this off for the real shop catalogue.");
        seeder.seed();
    }
}
