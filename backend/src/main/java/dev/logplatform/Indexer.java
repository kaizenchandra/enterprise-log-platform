package dev.logplatform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "platform.worker", havingValue = "true")
public class Indexer {
    final Search store;
    final ObjectMapper json;
    final KafkaTemplate<String, String> kafka;

    Indexer(Search store, ObjectMapper json, KafkaTemplate<String, String> kafka) {
        this.store = store;
        this.json = json;
        this.kafka = kafka;
    }

    @KafkaListener(topics = "logs-v1", batch = "true")
    public void index(List<String> records) throws Exception {
        List<String> chunk = new ArrayList<>();
        int bytes = 0;
        for (String record : records) {
            int size = record.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + 256;
            if (!chunk.isEmpty() && bytes + size > 1048576) {
                indexChunk(chunk);
                chunk = new ArrayList<>();
                bytes = 0;
            }
            chunk.add(record);
            bytes += size;
        }
        if (!chunk.isEmpty()) indexChunk(chunk);
    }

    void indexChunk(List<String> records) throws Exception {
        List<String> pending = new ArrayList<>(records);
        // At most 100 records per poll; bulk chunks are bounded to approximately 1 MiB.
        for (int attempt = 0; !pending.isEmpty() && attempt < 3; attempt++) {
            List<String> valid = new ArrayList<>();
            StringBuilder bulk = new StringBuilder();
            for (String value : pending) {
                Events.Event event;
                try {
                    event = json.readValue(value, Events.Event.class);
                } catch (Exception e) {
                    quarantine(value, "malformed-record");
                    continue;
                }
                String index = "logs-" + event.event_timestamp().atZone(ZoneOffset.UTC).toLocalDate();
                bulk.append(json.writeValueAsString(Map.of("create", Map.of("_index", index, "_id", event.log_id())))).append('\n').append(value).append('\n');
                valid.add(value);
            }
            if (valid.isEmpty()) return;
            JsonNode items = store.request("POST", "/_bulk", bulk.toString()).path("items");
            if (items.size() != valid.size()) throw new IllegalStateException("Incomplete bulk response");
            List<String> retry = new ArrayList<>();
            for (int n = 0; n < valid.size(); n++) {
                int status = items.get(n).path("create").path("status").asInt();
                if (status == 201 || status == 409) continue;
                if (status == 429 || status == 403 || status == 404 || status >= 500 || status == 0)
                    retry.add(valid.get(n));
                else quarantine(valid.get(n), "bulk-status-" + status);
            }
            pending = retry;
            if (!pending.isEmpty()) Thread.sleep(100L * (attempt + 1));
        }
        // Offset commit is withheld. Replayed successes conflict harmlessly.
        if (!pending.isEmpty()) throw new IllegalStateException("Retryable bulk items remain");
    }

    void quarantine(String value, String reason) throws Exception {
        String id = Events.hash(value);
        kafka.send("logs-quarantine-v1", id, json.writeValueAsString(Map.of("id", id, "reason", reason, "record", value))).get(12, TimeUnit.SECONDS);
    }

    @org.springframework.context.annotation.Bean
    org.springframework.kafka.listener.CommonErrorHandler errors() {
        return new org.springframework.kafka.listener.DefaultErrorHandler(new org.springframework.util.backoff.FixedBackOff(2000, org.springframework.util.backoff.FixedBackOff.UNLIMITED_ATTEMPTS));
    }
}
