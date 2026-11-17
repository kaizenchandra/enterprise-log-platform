package dev.logplatform;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class Notifications {
    final JdbcTemplate db;
    final String url, secret;
    final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    Notifications(JdbcTemplate db, @Value("${NOTIFICATION_URL:}") String url, @Value("${NOTIFICATION_SECRET:}") String secret) {
        this.db = db;
        this.url = url;
        this.secret = secret;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void deliver() {
        if (url.isBlank() || secret.isBlank()) return;
        for (var row : db.queryForList("select * from alert_notifications where delivered=false and dead=false and next_attempt<=now() order by next_attempt limit 10 for update skip locked")) {
            boolean delivered = false;
            try {
                var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(2)).header("Content-Type", "application/json").header("X-Notification-Key", secret).header("Idempotency-Key", row.get("id").toString()).POST(HttpRequest.BodyPublishers.ofString(row.get("payload").toString())).build();
                int status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
                delivered = status >= 200 && status < 300;
            } catch (Exception e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            }
            int attempts = ((Number) row.get("attempts")).intValue() + 1;
            db.update("update alert_notifications set delivered=?,attempts=?,dead=?,next_attempt=now()+interval '30 seconds' where id=?", delivered, attempts, !delivered && attempts >= 8, row.get("id"));
        }
    }
}
