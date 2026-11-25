package com.sivacrackers.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** The demo seed is OFF unless APP_SEED_DEMO=true. */
class DemoDataRunnerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(DemoDataSeeder.class, () -> mock(DemoDataSeeder.class))
            .withUserConfiguration(DemoDataRunner.class);

    @Test
    @DisplayName("No setting → demo runner does not exist (default off)")
    void offByDefault() {
        contextRunner.run(ctx -> assertThat(ctx).doesNotHaveBean(DemoDataRunner.class));
    }

    @Test
    @DisplayName("APP_SEED_DEMO=false → demo runner does not exist")
    void offWhenFalse() {
        contextRunner.withPropertyValues("app.seed-demo=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(DemoDataRunner.class));
    }

    @Test
    @DisplayName("APP_SEED_DEMO=true → runner exists and calls the seeder once")
    void onWhenTrue() {
        contextRunner.withPropertyValues("app.seed-demo=true").run(ctx -> {
            assertThat(ctx).hasSingleBean(DemoDataRunner.class);
            ctx.getBean(DemoDataRunner.class).run(new DefaultApplicationArguments());
            verify(ctx.getBean(DemoDataSeeder.class)).seed();
        });
    }
}
