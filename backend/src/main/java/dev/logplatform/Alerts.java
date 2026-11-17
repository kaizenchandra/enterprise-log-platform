package dev.logplatform;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class Alerts {
    final JdbcTemplate db;
    final Search store;

    Alerts(JdbcTemplate d, Search s) {
        db = d;
        store = s;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void evaluate() throws Exception {
        for (var r : db.queryForList("select * from alert_rules where enabled=true for update skip locked")) {
            Instant now = Instant.now();
            var q = new Search.Query(now.minusSeconds(300).toString(), now.toString(), null, r.get("service").toString(), "ERROR", null, null, 1);
            var result = store.search(r.get("tenant").toString(), q);
            if (result.path("timed_out").asBoolean() || result.path("_shards").path("failed").asInt() > 0) continue;
            long count = result.path("hits").path("total").path("value").asLong();
            boolean firing = count >= ((Number) r.get("threshold")).longValue();
            if (firing != (boolean) r.get("firing")) {
                db.update("insert into alert_history(rule_id,tenant,state,event_count) values(?,?,?,?)", r.get("id"), r.get("tenant"), firing ? "FIRING" : "RESOLVED", count);
                db.update("insert into alert_notifications(id,tenant,rule_id,payload) values(?,?,?,?::jsonb)", java.util.UUID.randomUUID(), r.get("tenant"), r.get("id"), store.json.writeValueAsString(java.util.Map.of("ruleId", r.get("id"), "state", firing ? "FIRING" : "RESOLVED", "count", count)));
            }
            db.update("update alert_rules set firing=?,last_evaluated=? where id=?", firing, java.sql.Timestamp.from(now), r.get("id"));
        }
    }
}
