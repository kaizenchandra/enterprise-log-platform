package dev.logplatform;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public final class Events {
    private static final java.util.regex.Pattern REDACT_0 = java.util.regex.Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._~+/-]+=*");
    private static final java.util.regex.Pattern REDACT_1 = java.util.regex.Pattern.compile("(?i)(password|token|api[_-]?key|authorization|cookie)([\\s\"':=]+)[^\\s,;\"}]+");
    private static final java.util.regex.Pattern REDACT_2 = java.util.regex.Pattern.compile("(?<![A-Za-z0-9._%+-])[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,253}\\.[A-Za-z]{2,63}");

    public static String hash(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String redact(String s) {
        String masked = REDACT_1.matcher(REDACT_0.matcher(s).replaceAll("$1[REDACTED]")).replaceAll("$1$2[REDACTED]");
        return masked.indexOf('@') < 0 ? masked : REDACT_2.matcher(masked).replaceAll("[EMAIL]");
    }

    public static Event normalize(Input i, String tenant, String source, String service, String env, Instant now) {
        if (i.message() == null || i.message().length() > 16384 || i.eventId() == null || !i.eventId().matches("[A-Za-z0-9._:-]{1,128}"))
            throw new IllegalArgumentException("message/eventId invalid");
        Instant ts = i.timestamp() == null ? now : Instant.parse(i.timestamp());
        if (ts.isAfter(now.plusSeconds(300)) || ts.isBefore(now.minus(Duration.ofDays(30))))
            throw new IllegalArgumentException("timestamp outside acceptance window");
        String level = Objects.toString(i.severity(), "INFO").toUpperCase(Locale.ROOT);
        if (level.equals("WARN")) level = "WARNING";
        if (!Set.of("TRACE", "DEBUG", "INFO", "WARNING", "ERROR", "FATAL").contains(level))
            throw new IllegalArgumentException("severity invalid");
        Map<String, String> attrs = new TreeMap<>();
        if (i.attributes() != null) {
            if (i.attributes().size() > 32) throw new IllegalArgumentException("too many attributes");
            i.attributes().forEach((k, v) -> {
                if (!k.matches("[A-Za-z][A-Za-z0-9_.-]{0,63}") || v instanceof Map || v instanceof Collection)
                    throw new IllegalArgumentException("attribute invalid");
                String val = String.valueOf(v);
                if (val.length() > 1024) throw new IllegalArgumentException("attribute too large");
                attrs.put(k, k.toLowerCase(Locale.ROOT).matches(".*(password|token|secret|key|authorization|cookie).*") ? "[REDACTED]" : redact(val));
            });
        }
        return new Event(hash(tenant + "\0" + source + "\0" + i.eventId()), tenant, source, service, env, level, ts, now, redact(i.message()), attrs, validId(i.traceId(), 32), validId(i.spanId(), 16), 1);
    }

    static String validId(String s, int n) {
        if (s == null) return null;
        if (!s.matches("[a-f0-9]{" + n + "}")) throw new IllegalArgumentException("trace/span invalid");
        return s;
    }

    public record Input(String eventId, String timestamp, String severity, String message,
                        Map<String, Object> attributes, String traceId, String spanId) {
    }

    public record Event(String log_id, String tenant_id, String source_id, String service_name, String environment,
                        String severity, Instant event_timestamp, Instant ingestion_timestamp, String message,
                        Map<String, String> attributes, String trace_id, String span_id, int schema_version) {
    }
}
