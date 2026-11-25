package com.sivacrackers.security;

import com.sivacrackers.common.exception.TooManyRequestsException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Brute-force protection (in memory, per server instance).
 *
 * <ul>
 *   <li><b>Per account</b>: 5 failed attempts within 15 minutes lock that login identifier for
 *       15 minutes. During the lock even the correct password is refused (429 + Retry-After).</li>
 *   <li><b>Per IP address</b>: at most 30 login attempts per 15 minutes and 10 registrations per
 *       hour, whatever accounts are tried.</li>
 * </ul>
 * Keys are login identifiers / IP addresses only – passwords are never stored or logged.
 */
@Service
public class LoginAttemptService {

    public static final int MAX_FAILURES = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    static final int LOGINS_PER_IP = 30;
    static final Duration LOGIN_IP_WINDOW = Duration.ofMinutes(15);
    static final int REGISTRATIONS_PER_IP = 10;
    static final Duration REGISTRATION_IP_WINDOW = Duration.ofHours(1);
    private static final int MAX_TRACKED = 10_000;

    private final Clock clock;
    private final Map<String, Failures> failures = new ConcurrentHashMap<>();
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    private record Failures(int count, Instant firstFailure, Instant lockedUntil) {
    }

    private record Window(int count, Instant start) {
    }

    /** Throws 429 if this account is currently locked. */
    public void checkNotLocked(String accountKey) {
        Failures f = failures.get(accountKey);
        Instant now = clock.instant();
        if (f != null && f.lockedUntil() != null && now.isBefore(f.lockedUntil())) {
            throw new TooManyRequestsException(
                    "Too many failed attempts. Please wait 15 minutes and try again.",
                    secondsUntil(now, f.lockedUntil()));
        }
    }

    public void recordFailure(String accountKey) {
        Instant now = clock.instant();
        prune(failures);
        failures.compute(accountKey, (k, f) -> {
            if (f == null || f.lockedUntil() != null && !now.isBefore(f.lockedUntil())
                    || Duration.between(f.firstFailure(), now).compareTo(LOCK_DURATION) > 0) {
                f = new Failures(0, now, null);
            }
            int count = f.count() + 1;
            return new Failures(count, f.firstFailure(), count >= MAX_FAILURES ? now.plus(LOCK_DURATION) : null);
        });
    }

    public void recordSuccess(String accountKey) {
        failures.remove(accountKey);
    }

    public void acquireLoginSlot(String ip) {
        acquire("login-ip:" + ip, LOGINS_PER_IP, LOGIN_IP_WINDOW,
                "Too many login attempts from your network. Please wait a few minutes and try again.");
    }

    public void acquireRegistrationSlot(String ip) {
        acquire("register-ip:" + ip, REGISTRATIONS_PER_IP, REGISTRATION_IP_WINDOW,
                "Too many accounts created from your network. Please try again later.");
    }

    private void acquire(String key, int limit, Duration window, String message) {
        Instant now = clock.instant();
        prune(windows);
        Window w = windows.compute(key, (k, old) -> old == null || !now.isBefore(old.start().plus(window))
                ? new Window(1, now)
                : new Window(old.count() + 1, old.start()));
        if (w.count() > limit) {
            throw new TooManyRequestsException(message, secondsUntil(now, w.start().plus(window)));
        }
    }

    /** Keeps memory bounded if an attacker sprays many different keys. */
    private static void prune(Map<String, ?> map) {
        if (map.size() > MAX_TRACKED) {
            map.clear();
        }
    }

    /** Whole seconds until {@code until}, rounded up, so Retry-After never says "try now" too early. */
    private static long secondsUntil(Instant now, Instant until) {
        long millis = Duration.between(now, until).toMillis();
        return Math.max(1, (millis + 999) / 1000);
    }
}
