package aeza.hostmaster.checks.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Handles WebSocket connections that stream agent check results for a specific job.
 */
@Component
public class CheckResultsWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CheckResultsWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final Map<UUID, Set<WebSocketSession>> sessionsByJob = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionJobMap = new ConcurrentHashMap<>();

    public CheckResultsWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID jobId = resolveJobId(session.getUri());
        if (jobId == null) {
            log.warn("Rejecting websocket session {} due to invalid job id in path {}", session.getId(), session.getUri());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        sessionsByJob.computeIfAbsent(jobId, id -> ConcurrentHashMap.newKeySet()).add(session);
        sessionJobMap.put(session.getId(), jobId);
        log.debug("WebSocket session {} subscribed to job {}", session.getId(), jobId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        UUID jobId = sessionJobMap.remove(session.getId());
        if (jobId == null) {
            return;
        }

        Set<WebSocketSession> sessions = sessionsByJob.get(jobId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                sessionsByJob.remove(jobId);
            }
        }
        log.debug("WebSocket session {} closed for job {}", session.getId(), jobId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("Transport error in websocket session {}: {}", session.getId(), exception.getMessage());
    }

    public void sendResult(UUID jobId, Object payload) {
        if (jobId == null || payload == null) {
            return;
        }

        String message;
        try {
            if (payload instanceof String str) {
                message = str;
            } else {
                message = objectMapper.writeValueAsString(payload);
            }
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialise payload for job {}: {}", jobId, ex.getOriginalMessage());
            return;
        }

        sendText(jobId, message);
    }

    public void completeJob(UUID jobId) {
        if (jobId == null) {
            return;
        }

        Set<WebSocketSession> sessions = sessionsByJob.remove(jobId);
        if (sessions == null) {
            return;
        }

        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.close(CloseStatus.NORMAL);
                }
            } catch (IOException ex) {
                log.debug("Failed to close websocket session {} for job {}: {}", session.getId(), jobId, ex.getMessage());
            }
        }
    }

    private void sendText(UUID jobId, String message) {
        Set<WebSocketSession> sessions = sessionsByJob.get(jobId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        sessions.removeIf(session -> !session.isOpen());

        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException ex) {
                log.warn("Failed to send websocket message to session {} for job {}: {}", session.getId(), jobId, ex.getMessage());
            }
        }
    }

    private UUID resolveJobId(URI uri) {
        if (uri == null) {
            return null;
        }

        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            return null;
        }

        String[] segments = path.split("/");
        for (int i = segments.length - 1; i >= 0; i--) {
            String segment = segments[i];
            if (segment == null || segment.isBlank()) {
                continue;
            }
            try {
                return UUID.fromString(segment);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }
}
