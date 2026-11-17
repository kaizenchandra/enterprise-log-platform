package dev.logplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class IndexerTest {
    ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    String event(String id) throws Exception {
        return json.writeValueAsString(Events.normalize(new Events.Input(id, null, "INFO", "hello", null, null, null), "t", "s", "v", "p", Instant.now()));
    }

    @Test
    void retryOnlyEligibleFailures() throws Exception {
        Search search = mock(Search.class);
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        when(search.request(eq("POST"), eq("/_bulk"), any())).thenReturn(json.readTree("{\"items\":[{\"create\":{\"status\":201}},{\"create\":{\"status\":429}},{\"create\":{\"status\":400}}]}"), json.readTree("{\"items\":[{\"create\":{\"status\":201}}]}"));
        when(kafka.send(eq("logs-quarantine-v1"), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        String a = event("a"), b = event("b"), c = event("c");
        new Indexer(search, json, kafka).index(List.of(a, b, c));
        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(search, times(2)).request(eq("POST"), eq("/_bulk"), captor.capture());
        assertThat(captor.getAllValues().get(1).toString()).contains(b).doesNotContain(a, c);
        verify(kafka).send(eq("logs-quarantine-v1"), anyString(), anyString());
    }

    @Test
    void quarantineFailurePreventsCommit() throws Exception {
        Search search = mock(Search.class);
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));
        assertThatThrownBy(() -> new Indexer(search, json, kafka).index(List.of("malformed"))).isInstanceOf(ExecutionException.class);
        verifyNoInteractions(search);
    }
}
