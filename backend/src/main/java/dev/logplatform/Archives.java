package dev.logplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@RestController
@RequestMapping("/api/v1/archives")
public class Archives {
    final Search store;
    final JdbcTemplate db;
    final ObjectMapper json;
    final KafkaTemplate<String, String> kafka;
    final S3Client s3;
    final String bucket = "log-archives";
    final Semaphore slots = new Semaphore(2);

    Archives(Search store, JdbcTemplate db, ObjectMapper json, KafkaTemplate<String, String> kafka, @Value("${S3_ENDPOINT:http://localhost:9000}") String endpoint, @Value("${S3_ACCESS_KEY:local-archive}") String key, @Value("${S3_SECRET_KEY:local-archive-password}") String secret) {
        this.store = store;
        this.db = db;
        this.json = json;
        this.kafka = kafka;
        s3 = S3Client.builder().endpointOverride(URI.create(endpoint)).region(Region.US_EAST_1).forcePathStyle(true).credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(key, secret))).httpClientBuilder(UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofSeconds(2)).socketTimeout(Duration.ofSeconds(5))).overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(15))).build();
    }

    @jakarta.annotation.PreDestroy
    void close() {
        s3.close();
    }

    void acquire() {
        if (!slots.tryAcquire())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
    }

    @GetMapping
    Object list(@AuthenticationPrincipal Jwt jwt) {
        Api.admin(jwt);
        return db.queryForList("select * from archive_manifests where tenant=? order by created_at desc limit 100", Api.tenant(jwt));
    }

    @PostMapping
    Object archive(@AuthenticationPrincipal Jwt jwt, @org.springframework.web.bind.annotation.RequestBody Search.Query query) throws Exception {
        Api.admin(jwt);
        String tenant = Api.tenant(jwt);
        acquire();
        try {
            var result = store.search(tenant, query);
            if (result.path("timed_out").asBoolean() || result.path("_shards").path("failed").asInt() > 0)
                throw new IllegalStateException("Partial archive query");
            var hits = result.path("hits").path("hits");
            if (hits.isEmpty()) throw new IllegalArgumentException("No events");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (var gzip = new GZIPOutputStream(bytes)) {
                for (var hit : hits) {
                    gzip.write(json.writeValueAsBytes(hit.path("_source")));
                    gzip.write('\n');
                }
            }
            byte[] compressed = bytes.toByteArray();
            if (compressed.length > 8388608) throw new IllegalArgumentException("Archive page too large");
            try {
                s3.headBucket(b -> b.bucket(bucket));
            } catch (NoSuchBucketException e) {
                s3.createBucket(b -> b.bucket(bucket));
            } catch (S3Exception e) {
                if (e.statusCode() != 404) throw e;
                s3.createBucket(b -> b.bucket(bucket));
            }
            UUID id = UUID.randomUUID();
            String object = tenant + "/" + java.time.LocalDate.now(java.time.ZoneOffset.UTC) + "/" + id + ".ndjson.gz";
            String sha = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(compressed));
            s3.putObject(b -> b.bucket(bucket).key(object).contentType("application/gzip").metadata(Map.of("sha256", sha)), RequestBody.fromBytes(compressed));
            // S3 success precedes manifest commit: DB failure may leave an orphan, never delete hot data here.
            db.update("insert into archive_manifests(id,tenant,object_key,sha256,event_count) values(?,?,?,?,?)", id, tenant, object, sha, hits.size());
            return Map.of("id", id, "events", hits.size(), "cursor", hits.get(hits.size() - 1).path("sort"), "sha256", sha);
        } finally {
            slots.release();
        }
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/{id}/restore")
    Object restore(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) throws Exception {
        Api.admin(jwt);
        String tenant = Api.tenant(jwt);
        acquire();
        try {
            var rows = db.queryForList("select * from archive_manifests where id=? and tenant=? for update", id, tenant);
            if (rows.size() != 1)
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
            var manifest = rows.getFirst();
            byte[] bytes;
            try (var in = s3.getObject(b -> b.bucket(bucket).key(manifest.get("object_key").toString()))) {
                bytes = in.readNBytes(8388609);
            }
            if (bytes.length > 8388608) throw new IllegalArgumentException("Compressed size limit");
            String sha = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
            if (!sha.equals(manifest.get("sha256"))) throw new IllegalStateException("Archive integrity failure");
            byte[] raw;
            try (var in = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                raw = in.readNBytes(33554433);
            }
            if (raw.length > 33554432) throw new IllegalArgumentException("Expanded size limit");
            String[] lines = new String(raw, java.nio.charset.StandardCharsets.UTF_8).split("\n");
            if (lines.length > 500) throw new IllegalArgumentException("Record limit");
            // Validate every tenant before any publication. Restores preserve immutable IDs and timestamps.
            var policies = db.queryForList("select days from retention_policies where tenant=?", tenant);
            int days = policies.isEmpty() ? 30 : ((Number) policies.getFirst().get("days")).intValue();
            List<Events.Event> events = new ArrayList<>();
            for (String line : lines) {
                var event = json.readValue(line, Events.Event.class);
                if (event.event_timestamp().isBefore(java.time.Instant.now().minus(Duration.ofDays(days))))
                    throw new IllegalArgumentException("Archive outside hot retention; restore denied");
                if (!tenant.equals(event.tenant_id())) throw new IllegalArgumentException("Archive tenant mismatch");
                events.add(event);
            }
            List<CompletableFuture<?>> sends = new ArrayList<>();
            for (var event : events)
                sends.add(kafka.send("logs-v1", event.source_id() + ":" + event.log_id().substring(0, 2), json.writeValueAsString(event)));
            CompletableFuture.allOf(sends.toArray(CompletableFuture[]::new)).get(12, TimeUnit.SECONDS);
            return Map.of("accepted", events.size(), "milestone", "kafka-acknowledged");
        } finally {
            slots.release();
        }
    }

    @org.springframework.transaction.annotation.Transactional
    @DeleteMapping("/{id}")
    Object delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        Api.admin(jwt);
        String tenant = Api.tenant(jwt);
        var rows = db.queryForList("select * from archive_manifests where id=? and tenant=? for update", id, tenant);
        if (rows.isEmpty())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        s3.deleteObject(b -> b.bucket(bucket).key(rows.getFirst().get("object_key").toString()));
        db.update("delete from archive_manifests where id=? and tenant=?", id, tenant);
        return Map.of("deleted", id);
    }
}
