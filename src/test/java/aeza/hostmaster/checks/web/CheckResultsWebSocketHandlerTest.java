package aeza.hostmaster.checks.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class CheckResultsWebSocketHandlerTest {

    private CheckResultsWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CheckResultsWebSocketHandler(new ObjectMapper());
    }

    @Test
    void shouldSubscribeSessionAndSendSerializedPayload() throws Exception {
        UUID jobId = UUID.randomUUID();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");
        when(session.getUri()).thenReturn(new URI("ws://localhost/api/checks/socket/" + jobId));
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);

        Map<String, Object> payload = Map.of("status", "ok", "latency", 42);
        handler.sendResult(jobId, payload);

        verify(session).sendMessage(argThat(message -> {
            assertThat(message).isInstanceOf(TextMessage.class);
            String payloadText = ((TextMessage) message).getPayload();
            assertThat(payloadText).contains("\"status\":\"ok\"");
            assertThat(payloadText).contains("\"latency\":42");
            return true;
        }));
    }

    @Test
    void shouldSendRawStringPayloadUnchanged() throws Exception {
        UUID jobId = UUID.randomUUID();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-raw");
        when(session.getUri()).thenReturn(new URI("ws://localhost/api/checks/socket/" + jobId));
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);

        String jsonPayload = "{\"type\":\"ping\"}";
        handler.sendResult(jobId, jsonPayload);

        verify(session).sendMessage(argThat(message -> {
            assertThat(message).isInstanceOf(TextMessage.class);
            return ((TextMessage) message).getPayload().equals(jsonPayload);
        }));
    }

    @Test
    void shouldRejectSessionWithInvalidJobId() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-2");
        when(session.getUri()).thenReturn(new URI("ws://localhost/api/checks/socket/not-a-uuid"));

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    void shouldNotSendMessagesAfterSessionClosed() throws Exception {
        UUID jobId = UUID.randomUUID();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-3");
        when(session.getUri()).thenReturn(new URI("ws://localhost/api/checks/socket/" + jobId));
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        handler.sendResult(jobId, "ignored");

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void shouldCloseAllSessionsWhenJobCompletes() throws Exception {
        UUID jobId = UUID.randomUUID();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-4");
        when(session.getUri()).thenReturn(new URI("ws://localhost/api/checks/socket/" + jobId));
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);

        handler.sendResult(jobId, Map.of("status", "before-close"));
        verify(session).sendMessage(any(TextMessage.class));
        clearInvocations(session);

        handler.completeJob(jobId);

        verify(session).close(CloseStatus.NORMAL);
        handler.sendResult(jobId, Map.of("status", "after-close"));
        verify(session, never()).sendMessage(any(TextMessage.class));
    }
}
