package dev.logplatform;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

@RestController
public class LiveTail {
    final Search store;
    final Semaphore slots = new Semaphore(16);

    LiveTail(Search s) {
        store = s;
    }

    @GetMapping(value = "/api/v1/logs/stream", produces = "text/event-stream")
    ResponseEntity<StreamingResponseBody> stream(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String service, @RequestParam(required = false) String severity) {
        String tenant = Api.tenant(jwt);
        if (!slots.tryAcquire())
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(out -> {
            try {
                Set<String> seen = new LinkedHashSet<>();
                for (int n = 0; n < 60; n++) {
                    var now = Instant.now();
                    var result = store.search(tenant, new Search.Query(now.minusSeconds(30).toString(), now.toString(), null, service, severity, null, null, 100));
                    StringBuilder data = new StringBuilder(": heartbeat\n\n");
                    if (result.path("hits").path("total").path("value").asLong() > 100)
                        data.append("event: gap\ndata: {\"recovery\":\"search\"}\n\n");
                    for (var hit : result.path("hits").path("hits")) {
                        String id = hit.path("_id").asText();
                        if (seen.add(id)) data.append("data: ").append(hit.path("_source").toString()).append("\n\n");
                    }
                    while (seen.size() > 1000) seen.remove(seen.iterator().next());
                    out.write(data.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    out.flush();
                    Thread.sleep(1000);
                }
            } catch (Exception e) {/* disconnect or storage failure closes stream; recover with search */} finally {
                slots.release();
            }
        });
    }
}
