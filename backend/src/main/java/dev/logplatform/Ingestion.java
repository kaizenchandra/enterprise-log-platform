package dev.logplatform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RestController
public class Ingestion {
    final io.micrometer.core.instrument.MeterRegistry meters;
    final Quota quota;
    final JdbcTemplate db;
    final KafkaTemplate<String, String> kafka;
    final ObjectMapper json;
    final Semaphore permits = new Semaphore(32);

    Ingestion(JdbcTemplate d, KafkaTemplate<String, String> k, ObjectMapper j, Quota q, io.micrometer.core.instrument.MeterRegistry m) {
        meters = m;
        quota = q;
        db = d;
        kafka = k;
        json = j;
    }

    Map<String, Object> source(String key) {
        if (key == null || key.length() > 256)
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var rows = db.queryForList("select * from sources where credential_hash=?", Events.hash(key));
        if (rows.size() != 1) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return rows.getFirst();
    }

    @PostMapping("/api/v1/logs/ingest")
    ResponseEntity<?> ingest(@RequestHeader(value = "X-Ingestion-Key", required = false) String key, @RequestBody Batch b) throws Exception {
        var s = source(key);
        if (!s.get("id").equals(b.sourceId()))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN);
        return accept(s, b.logs());
    }

    ResponseEntity<?> accept(Map<String, Object> s, List<Events.Input> logs) throws Exception {
        if (logs == null || logs.isEmpty() || logs.size() > 500)
            throw new IllegalArgumentException("batch must contain 1..500 events");
        if (!permits.tryAcquire()) return ResponseEntity.status(503).header("Retry-After", "2").build();
        try {
            List<Events.Event> valid = new ArrayList<>();
            List<Map<String, Object>> invalid = new ArrayList<>();
            Instant now = Instant.now();
            for (int n = 0; n < logs.size(); n++) {
                try {
                    valid.add(Events.normalize(logs.get(n), s.get("tenant").toString(), s.get("id").toString(), s.get("service").toString(), s.get("environment").toString(), now));
                } catch (RuntimeException e) {
                    invalid.add(Map.of("position", n, "error", "invalid event"));
                }
            }
            quota.reserve(s.get("tenant").toString(), valid.size(), json.writeValueAsBytes(valid).length);
            List<CompletableFuture<?>> sends = new ArrayList<>();
            for (var e : valid)
                sends.add(kafka.send("logs-v1", e.source_id() + ":" + e.log_id().substring(0, 2), json.writeValueAsString(e)));
            CompletableFuture.allOf(sends.toArray(CompletableFuture[]::new)).get(12, TimeUnit.SECONDS);
            meters.counter("logs.accepted.events").increment(valid.size());
            meters.counter("logs.rejected.events").increment(invalid.size());
            return ResponseEntity.accepted().body(Map.of("milestone", "kafka-acknowledged", "accepted", valid.size(), "rejected", invalid));
        } finally {
            permits.release();
        }
    }

    @PostMapping("/v1/logs")
    ResponseEntity<?> otlp(@RequestHeader(value = "X-Ingestion-Key", required = false) String key, @RequestBody JsonNode root) throws Exception {
        var s = source(key);
        List<Events.Input> logs = new ArrayList<>();
        for (var resource : root.path("resourceLogs"))
            for (var scope : resource.path("scopeLogs"))
                for (var r : scope.path("logRecords")) {
                    Map<String, Object> a = new HashMap<>();
                    for (var attr : r.path("attributes"))
                        a.put(attr.path("key").asText(), attr.path("value").path("stringValue").asText());
                    String body = r.path("body").path("stringValue").asText();
                    String stamp = r.path("timeUnixNano").asText("0");
                    Instant ts = stamp.equals("0") ? Instant.now() : Instant.ofEpochSecond(0, Long.parseLong(stamp));
                    String identity = String.valueOf(a.getOrDefault("event.id", UUID.randomUUID().toString()));
                    logs.add(new Events.Input(identity, ts.toString(), r.path("severityText").asText("INFO"), body, a, r.has("traceId") ? r.get("traceId").asText() : null, r.has("spanId") ? r.get("spanId").asText() : null));
                }
        var response = accept(s, logs);
        if (response.getStatusCode().value() != 202) return response;
        var body = (Map<?, ?>) response.getBody();
        int rejected = ((java.util.List<?>) body.get("rejected")).size();
        return ResponseEntity.ok(rejected == 0 ? Map.of() : Map.of("partialSuccess", Map.of("rejectedLogRecords", String.valueOf(rejected), "errorMessage", "Invalid log records rejected")));
    }

    public record Batch(String sourceId, List<Events.Input> logs) {
    }
}
