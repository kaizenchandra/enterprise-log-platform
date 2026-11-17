package dev.logplatform;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventsTest {
    final Instant now = Instant.parse("2026-11-17T10:00:00Z");

    Events.Input input(String id, String message) {
        return new Events.Input(id, now.toString(), "warn", message, Map.of("password", "fake-secret"), null, null);
    }

    @Test
    void stableIdentityAndTenantIsolation() {
        var a = Events.normalize(input("1", "same"), "a", "s", "service", "prod", now);
        var retry = Events.normalize(input("1", "changed"), "a", "s", "service", "prod", now.plusSeconds(1));
        var b = Events.normalize(input("1", "same"), "b", "s", "service", "prod", now);
        assertThat(a.log_id()).isEqualTo(retry.log_id()).isNotEqualTo(b.log_id());
        assertThat(a.attributes().get("password")).isEqualTo("[REDACTED]");
        assertThat(a.severity()).isEqualTo("WARNING");
    }

    @Test
    void repeatedMessagesAreDistinct() {
        assertThat(Events.normalize(input("1", "same"), "a", "s", "v", "p", now).log_id()).isNotEqualTo(Events.normalize(input("2", "same"), "a", "s", "v", "p", now).log_id());
    }

    @Test
    void redactSecrets() {
        assertThat(Events.redact("Bearer abc.def password=hello bob@example.com")).doesNotContain("abc.def", "hello", "bob@example.com");
    }

    @Test
    void rejectFutureAndOversize() {
        assertThatThrownBy(() -> Events.normalize(new Events.Input("1", now.plusSeconds(600).toString(), "INFO", "x", null, null, null), "t", "s", "v", "p", now)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Events.normalize(input("1", "x".repeat(16385)), "t", "s", "v", "p", now)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void boundedRedactionForLargeUntrustedMessages() {
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            String large = "x".repeat(16000);
            for (int i = 0; i < 100; i++) {
                assertThat(Events.redact(large)).isEqualTo(large);
                assertThat(Events.redact(large + "@invalid")).isNotNull();
            }
        });
    }
}
