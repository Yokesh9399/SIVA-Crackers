package com.sivacrackers.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sivacrackers.common.exception.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-25T10:00:00Z"));
    private final LoginAttemptService service = new LoginAttemptService(clock);

    private void fail(String key, int times) {
        for (int i = 0; i < times; i++) {
            service.checkNotLocked(key);
            service.recordFailure(key);
        }
    }

    @Test
    @DisplayName("4 failures: still allowed; the 5th locks the account")
    void locksAfterFive() {
        fail("customer:a@x.test", 4);
        assertThatCode(() -> service.checkNotLocked("customer:a@x.test")).doesNotThrowAnyException();
        service.recordFailure("customer:a@x.test");
        assertThatThrownBy(() -> service.checkNotLocked("customer:a@x.test"))
                .isInstanceOf(TooManyRequestsException.class)
                .satisfies(e -> assertThat(((TooManyRequestsException) e).getRetryAfterSeconds()).isEqualTo(900));
    }

    @Test
    @DisplayName("The lock lasts 15 minutes, then the account can try again")
    void lockExpires() {
        fail("k", 5);
        clock.advance(Duration.ofMinutes(14));
        assertThatThrownBy(() -> service.checkNotLocked("k")).isInstanceOf(TooManyRequestsException.class);
        clock.advance(Duration.ofMinutes(1));
        assertThatCode(() -> service.checkNotLocked("k")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A successful login clears the failure count")
    void successResets() {
        fail("k", 4);
        service.recordSuccess("k");
        fail("k", 4);
        assertThatCode(() -> service.checkNotLocked("k")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Failures older than 15 minutes do not count")
    void oldFailuresForgotten() {
        fail("k", 4);
        clock.advance(Duration.ofMinutes(16));
        fail("k", 4);
        assertThatCode(() -> service.checkNotLocked("k")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Accounts are locked independently")
    void independent() {
        fail("a", 5);
        assertThatCode(() -> service.checkNotLocked("b")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Per-IP limits: 30 logins / 15 min and 10 registrations / hour")
    void ipLimits() {
        for (int i = 0; i < 30; i++) {
            service.acquireLoginSlot("1.2.3.4");
        }
        assertThatThrownBy(() -> service.acquireLoginSlot("1.2.3.4")).isInstanceOf(TooManyRequestsException.class);
        assertThatCode(() -> service.acquireLoginSlot("5.6.7.8")).doesNotThrowAnyException();
        clock.advance(Duration.ofMinutes(15));
        assertThatCode(() -> service.acquireLoginSlot("1.2.3.4")).doesNotThrowAnyException();

        for (int i = 0; i < 10; i++) {
            service.acquireRegistrationSlot("9.9.9.9");
        }
        assertThatThrownBy(() -> service.acquireRegistrationSlot("9.9.9.9")).isInstanceOf(TooManyRequestsException.class);
    }
}
