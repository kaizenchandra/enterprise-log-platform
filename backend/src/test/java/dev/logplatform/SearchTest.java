package dev.logplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchTest {
    @Test
    void tenantIsMandatoryAndTextIsNotDsl() {
        var s = new Search(new ObjectMapper(), "http://localhost");
        var q = new Search.Query("2026-11-17T00:00:00Z", "2026-11-17T01:00:00Z", "* OR tenant_id:other", null, null, null, null, 10);
        String b = s.build("mine", q).toString();
        assertThat(b).contains("tenant_id=mine", "match={message=* OR tenant_id:other}");
    }

    @Test
    void boundTimeAndSize() {
        var s = new Search(new ObjectMapper(), "http://localhost");
        assertThatThrownBy(() -> s.build("t", new Search.Query("2026-01-01T00:00:00Z", "2026-11-01T00:00:00Z", null, null, null, null, null, 1000))).isInstanceOf(IllegalArgumentException.class);
    }
}
