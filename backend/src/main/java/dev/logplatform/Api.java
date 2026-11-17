package dev.logplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class Api {
    final Retention retention;
    final Search store;
    final JdbcTemplate db;
    final ObjectMapper json;

    Api(Search s, JdbcTemplate d, ObjectMapper j, Retention r) {
        retention = r;
        store = s;
        db = d;
        json = j;
    }

    static String tenant(Jwt j) {
        String t = j.getClaimAsString("tenant_id");
        if (t == null || !t.matches("[a-z0-9-]{1,64}"))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN);
        return t;
    }

    static void admin(Jwt j) {
        if (j.getClaimAsStringList("roles") == null || !j.getClaimAsStringList("roles").contains("admin"))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @PostMapping("/logs/search")
    Object search(@AuthenticationPrincipal Jwt j, @RequestBody Search.Query q) throws Exception {
        return store.search(tenant(j), q);
    }

    @PostMapping("/logs/aggregate")
    Object aggregate(@AuthenticationPrincipal Jwt j, @RequestBody Search.Query q) throws Exception {
        var b = store.build(tenant(j), q);
        b.put("size", 0);
        b.put("aggs", Map.of("severity", Map.of("terms", Map.of("field", "severity", "size", 6)), "services", Map.of("terms", Map.of("field", "service_name", "size", 20)), "timeline", Map.of("date_histogram", Map.of("field", "event_timestamp", "fixed_interval", "1m"))));
        return store.request("POST", "/logs-*/_search", b);
    }

    @GetMapping("/logs/fields")
    Object fields(@AuthenticationPrincipal Jwt j) {
        tenant(j);
        return List.of("message", "service_name", "environment", "severity", "trace_id", "event_timestamp");
    }

    @GetMapping("/sources")
    Object sources(@AuthenticationPrincipal Jwt j) {
        return db.queryForList("select id,service,environment from sources where tenant=?", tenant(j));
    }

    @PostMapping("/sources")
    Object source(@AuthenticationPrincipal Jwt j, @RequestBody Map<String, String> b) {
        admin(j);
        String key = UUID.randomUUID() + "-" + UUID.randomUUID();
        db.update("insert into sources values(?,?,?,?,?)", b.get("id"), tenant(j), b.get("service"), b.get("environment"), Events.hash(key));
        return Map.of("key", key);
    }

    @GetMapping("/saved-queries")
    Object saved(@AuthenticationPrincipal Jwt j) throws Exception {
        var rows = db.queryForList("select * from saved_queries where tenant=?", tenant(j));
        for (var row : rows) row.put("definition", json.readTree(row.get("definition").toString()));
        return rows;
    }

    @PostMapping("/saved-queries")
    Object save(@AuthenticationPrincipal Jwt j, @RequestBody Map<String, Object> b) throws Exception {
        String t = tenant(j);
        String definition = json.writeValueAsString(b.get("query"));
        store.build(t, json.readValue(definition, Search.Query.class));
        UUID id = UUID.randomUUID();
        db.update("insert into saved_queries values(?,?,?,?::jsonb)", id, t, b.get("name"), definition);
        return Map.of("id", id);
    }

    @GetMapping("/alerts")
    Object alerts(@AuthenticationPrincipal Jwt j) {
        return db.queryForList("select * from alert_history where tenant=? order by occurred_at desc limit 100", tenant(j));
    }

    @PostMapping("/alert-rules")
    Object rule(@AuthenticationPrincipal Jwt j, @RequestBody Map<String, String> b) {
        admin(j);
        long threshold = Long.parseLong(b.get("threshold"));
        if (threshold < 1) throw new IllegalArgumentException();
        UUID id = UUID.randomUUID();
        db.update("insert into alert_rules(id,tenant,service,threshold) values(?,?,?,?)", id, tenant(j), b.get("service"), threshold);
        return Map.of("id", id);
    }

    @PostMapping("/retention")
    Object retention(@AuthenticationPrincipal Jwt j, @RequestBody Map<String, String> b) {
        admin(j);
        int days = Integer.parseInt(b.get("days"));
        if (days < 1 || days > 30) throw new IllegalArgumentException();
        db.update("insert into retention_policies(tenant,days,legal_hold) values(?,?,?) on conflict(tenant) do update set days=excluded.days,legal_hold=excluded.legal_hold", tenant(j), days, Boolean.parseBoolean(b.getOrDefault("legalHold", "false")));
        return Map.of("days", days);
    }

    @GetMapping("/audit")
    Object audit(@AuthenticationPrincipal Jwt j) {
        admin(j);
        return db.queryForList("select * from audit_events where tenant=? order by occurred_at desc limit 100", tenant(j));
    }

    @GetMapping("/logs/{id}")
    Object get(@AuthenticationPrincipal Jwt j, @PathVariable String id) throws Exception {
        if (!id.matches("[a-f0-9]{64}")) throw new IllegalArgumentException();
        var filters = List.of(Map.of("term", Map.of("tenant_id", tenant(j))), Map.of("term", Map.of("log_id", id)));
        return store.request("POST", "/logs-*/_search", Map.of("size", 1, "timeout", "2s", "query", Map.of("bool", Map.of("filter", filters))));
    }

    @GetMapping("/logs/facets")
    Object facets(@AuthenticationPrincipal Jwt j, @RequestParam String from, @RequestParam String to) throws Exception {
        var body = store.build(tenant(j), new Search.Query(from, to, null, null, null, null, null, 1));
        body.put("size", 0);
        body.put("aggs", Map.of("services", Map.of("terms", Map.of("field", "service_name", "size", 50)), "severity", Map.of("terms", Map.of("field", "severity", "size", 6)), "environments", Map.of("terms", Map.of("field", "environment", "size", 20))));
        return store.request("POST", "/logs-*/_search", body);
    }

    @PostMapping("/retention/run")
    Object runRetention(@AuthenticationPrincipal Jwt j) throws Exception {
        admin(j);
        return Map.of("deleted", retention.runTenant(tenant(j)));
    }

    @GetMapping("/alert-notifications")
    Object notifications(@AuthenticationPrincipal Jwt j) {
        admin(j);
        return db.queryForList("select id,rule_id,attempts,delivered,dead,next_attempt from alert_notifications where tenant=? order by next_attempt desc limit 100", tenant(j));
    }
}
