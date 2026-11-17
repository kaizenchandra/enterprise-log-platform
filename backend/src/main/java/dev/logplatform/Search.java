package dev.logplatform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Search {
    final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    final ObjectMapper json;
    final String url;

    Search(ObjectMapper j, @Value("${platform.search-url}") String u) {
        json = j;
        url = u;
    }

    JsonNode request(String method, String path, Object body) throws Exception {
        String value = body instanceof String ? (String) body : json.writeValueAsString(body);
        var req = HttpRequest.newBuilder(URI.create(url + path)).timeout(Duration.ofSeconds(5)).header("Content-Type", path.equals("/_bulk") ? "application/x-ndjson" : "application/json").method(method, HttpRequest.BodyPublishers.ofString(value)).build();
        var r = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() >= 300) throw new IllegalStateException("Search storage unavailable: " + r.statusCode());
        return json.readTree(r.body());
    }

    Map<String, Object> build(String tenant, Query q) {
        Instant from = Instant.parse(q.from()), to = Instant.parse(q.to());
        if (!from.isBefore(to) || Duration.between(from, to).compareTo(Duration.ofDays(7)) > 0 || q.text() != null && q.text().length() > 256)
            throw new IllegalArgumentException();
        int limit = q.limit() == null ? 100 : q.limit();
        if (limit < 1 || limit > 500) throw new IllegalArgumentException();
        List<Object> filters = new ArrayList<>();
        filters.add(Map.of("term", Map.of("tenant_id", tenant)));
        filters.add(Map.of("range", Map.of("event_timestamp", Map.of("gte", q.from(), "lt", q.to()))));
        if (q.service() != null && !q.service().isBlank())
            filters.add(Map.of("term", Map.of("service_name", q.service())));
        if (q.severity() != null && !q.severity().isBlank())
            filters.add(Map.of("term", Map.of("severity", q.severity())));
        if (q.traceId() != null && !q.traceId().isBlank()) filters.add(Map.of("term", Map.of("trace_id", q.traceId())));
        Map<String, Object> bool = new HashMap<>();
        bool.put("filter", filters);
        if (q.text() != null && !q.text().isBlank())
            bool.put("must", List.of(Map.of("match", Map.of("message", q.text()))));
        Map<String, Object> b = new HashMap<>(Map.of("size", limit, "timeout", "2s", "track_total_hits", true, "query", Map.of("bool", bool), "sort", List.of(Map.of("event_timestamp", "desc"), Map.of("log_id", "asc"))));
        if (q.cursor() != null) {
            if (q.cursor().size() != 2) throw new IllegalArgumentException();
            b.put("search_after", q.cursor());
        }
        return b;
    }

    JsonNode search(String tenant, Query q) throws Exception {
        return request("POST", "/logs-*/_search", build(tenant, q));
    }

    public record Query(String from, String to, String text, String service, String severity, String traceId,
                        List<Object> cursor, Integer limit) {
    }
}
