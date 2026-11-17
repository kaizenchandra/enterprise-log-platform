package dev.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootApplication
@EnableScheduling
public class Demo {
    long sequence;

    public static void main(String[] a) {
        SpringApplication.run(Demo.class, a);
    }

    @Scheduled(fixedDelay = 2000)
    public void emit() {
        long n = sequence++;
        String service = System.getenv().getOrDefault("SERVICE_NAME", "payment-service");
        var id = UUID.randomUUID().toString();
        var record = Map.of("timeUnixNano", String.valueOf(Instant.now().getEpochSecond() * 1000000000L), "severityText", n % 5 == 0 ? "ERROR" : "INFO", "body", Map.of("stringValue", service + " " + (n % 5 == 0 ? "provider timeout" : "request completed")), "attributes", List.of(Map.of("key", "event.id", "value", Map.of("stringValue", id)), Map.of("key", "demo.service", "value", Map.of("stringValue", service))));
        try {
            RestClient.create(System.getenv().getOrDefault("COLLECTOR_URL", "http://localhost:4318")).post().uri("/v1/logs").body(Map.of("resourceLogs", List.of(Map.of("scopeLogs", List.of(Map.of("logRecords", List.of(record))))))).retrieve().toBodilessEntity();
        } catch (Exception e) {
            System.err.println("Demo export failed; this generator has no durable retry buffer");
        }
    }
}
