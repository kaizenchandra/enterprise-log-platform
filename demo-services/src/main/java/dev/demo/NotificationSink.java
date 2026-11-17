package dev.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class NotificationSink {
    final Set<String> ids = Collections.synchronizedSet(new LinkedHashSet<>());

    @PostMapping("/notifications")
    ResponseEntity<?> accept(@RequestHeader("X-Notification-Key") String key, @RequestHeader("Idempotency-Key") String id, @RequestBody Map<String, Object> body) {
        if (!"local-notification-key".equals(key)) return ResponseEntity.status(401).build();
        synchronized (ids) {
            ids.add(id);
            while (ids.size() > 1000) ids.remove(ids.iterator().next());
        }
        return ResponseEntity.ok(Map.of("received", id));
    }

    @GetMapping("/notifications")
    Object list() {
        synchronized (ids) {
            return List.copyOf(ids);
        }
    }
}
