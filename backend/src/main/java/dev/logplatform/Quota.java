package dev.logplatform;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class Quota {
    final JdbcTemplate db;

    Quota(JdbcTemplate d) {
        db = d;
    }

    void reserve(String tenant, int events, long bytes) {
        if (events > 5000 || bytes > 5242880)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
        int updated = db.update("insert into quota_windows(tenant,bucket,events,bytes) values(?,date_trunc('second',now()),?,?) on conflict(tenant,bucket) do update set events=quota_windows.events+excluded.events, bytes=quota_windows.bytes+excluded.bytes where quota_windows.events+excluded.events<=5000 and quota_windows.bytes+excluded.bytes<=5242880", tenant, events, bytes);
        if (updated != 1)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 60000)
    public void clean() {
        db.update("delete from quota_windows where bucket < now()-interval '2 minutes'");
    }
}
