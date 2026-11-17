package dev.logplatform;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class Retention {
    final JdbcTemplate db;
    final Search store;

    Retention(JdbcTemplate db, Search store) {
        this.db = db;
        this.store = store;
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
    @Transactional
    public void expire() throws Exception {
        for (var policy : db.queryForList("select * from retention_policies for update skip locked")) apply(policy);
    }

    @Transactional
    public long runTenant(String tenant) throws Exception {
        var rows = db.queryForList("select * from retention_policies where tenant=? for update", tenant);
        if (rows.isEmpty()) throw new IllegalArgumentException("Configure a retention policy first");
        return apply(rows.getFirst());
    }

    long apply(Map<String, Object> policy) throws Exception {
        if ((boolean) policy.get("legal_hold")) return 0;
        String tenant = policy.get("tenant").toString();
        String cutoff = Instant.now().minus(Duration.ofDays(((Number) policy.get("days")).longValue())).toString();
        var filters = List.of(Map.of("term", Map.of("tenant_id", tenant)), Map.of("range", Map.of("event_timestamp", Map.of("lt", cutoff))));
        var result = store.request("POST", "/logs-*/_delete_by_query?conflicts=proceed&refresh=true", Map.of("query", Map.of("bool", Map.of("filter", filters))));
        if (result.path("version_conflicts").asLong() > 0 || result.path("failures").size() > 0 || result.path("timed_out").asBoolean())
            throw new IllegalStateException("Retention incomplete");
        db.update("insert into audit_events(tenant,action) values(?,?)", tenant, "hot-retention-pass");
        return result.path("deleted").asLong();
    }
}
