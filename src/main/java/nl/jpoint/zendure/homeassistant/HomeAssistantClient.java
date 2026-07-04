package nl.jpoint.zendure.homeassistant;

import nl.jpoint.zendure.domain.event.HomeAssistantAuthenticatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket client for Home Assistant connection.
 * Handles authentication handshake, message sequencing, and reconnection.
 */
@Component
public class HomeAssistantClient extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(HomeAssistantClient.class);

    private final HomeAssistantProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final StateChangedEventDispatcher stateChangedDispatcher;
    private final AtomicLong messageIdCounter = new AtomicLong(1);
    private final ReconnectScheduler reconnectScheduler;

    private WebSocketSession currentSession;

    public HomeAssistantClient(
        HomeAssistantProperties properties,
        ApplicationEventPublisher eventPublisher,
        ObjectMapper objectMapper,
        StateChangedEventDispatcher stateChangedDispatcher
    ) {
        this.properties = properties;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.stateChangedDispatcher = stateChangedDispatcher;
        this.reconnectScheduler = new ReconnectScheduler(this::reconnect);
    }

    /**
     * Connect on application startup.
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("Connecting to Home Assistant at {}", properties.websocketUrl());
        connect();
    }

    /**
     * Reconnect to Home Assistant (used by scheduler).
     */
    private void reconnect() {
        log.info("Reconnecting to Home Assistant...");
        connect();
    }

    /**
     * Establish WebSocket connection and begin auth handshake.
     */
    public void connect() {
        try {
            WebSocketClient client = new StandardWebSocketClient();
            client.doHandshake(this, properties.websocketUrl())
                .addCallback(
                    session -> {
                        log.debug("WebSocket connected, starting authentication");
                        currentSession = session;
                        reconnectScheduler.reset();
                    },
                    ex -> {
                        log.warn("Failed to connect to Home Assistant: {}", ex.getMessage(), ex);
                        reconnectScheduler.scheduleReconnect();
                    }
                );
        } catch (Exception e) {
            log.error("Error initiating WebSocket connection", e);
            reconnectScheduler.scheduleReconnect();
        }
    }

    /**
     * Called when WebSocket connection is established (before auth).
     * Send the authentication message.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.debug("WebSocket connection established, sending auth");
        HomeAssistantMessage authMsg = HomeAssistantMessage.auth(properties.token());
        sendMessage(session, authMsg);
    }

    /**
     * Handle inbound messages (auth response, events).
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            HomeAssistantMessage msg = objectMapper.readValue(message.getPayload(), HomeAssistantMessage.class);
            handleMessage(session, msg);
        } catch (Exception e) {
            log.warn("Failed to parse incoming message: {}", e.getMessage(), e);
        }
    }

    /**
     * Route incoming message based on type.
     */
    private void handleMessage(WebSocketSession session, HomeAssistantMessage msg) throws IOException {
        switch (msg.type()) {
            case "auth_ok" -> {
                log.info("Home Assistant authentication successful");
                publishAuthenticatedEvent();
            }
            case "auth_required" -> {
                log.debug("Home Assistant sent auth_required (expected on connection)");
            }
            case "auth_invalid" -> {
                log.error("Home Assistant authentication failed: invalid token");
                session.close(CloseStatus.POLICY_VIOLATION);
            }
            case "result" -> {
                log.debug("Received result for message id {}: success={}", msg.id(), msg.success());
            }
            case "event" -> {
                handleEventMessage(msg);
            }
            default -> {
                log.debug("Received unknown message type: {}", msg.type());
            }
        }
    }

    /**
     * Handle event-type messages from Home Assistant.
     * Filters for state_changed events and dispatches to interested listeners.
     */
    private void handleEventMessage(HomeAssistantMessage msg) {
        if (msg.event() == null) {
            return;
        }

        try {
            var eventObj = objectMapper.convertValue(msg.event(), java.util.Map.class);
            String eventType = (String) eventObj.get("event_type");

            if ("state_changed".equals(eventType)) {
                var eventData = eventObj.get("data");
                stateChangedDispatcher.processStateChangedEvent(eventData);
            }
        } catch (Exception e) {
            log.debug("Error processing event message: {}", e.getMessage());
        }
    }

    /**
     * Handle connection close.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.warn("WebSocket connection closed: {} - {}", status.getCode(), status.getReason());
        if (currentSession == session) {
            currentSession = null;
        }
        reconnectScheduler.scheduleReconnect();
    }

    /**
     * Handle transport or protocol errors.
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error", exception);
        reconnectScheduler.scheduleReconnect();
    }

    /**
     * Send a message on the current session.
     */
    public void send(HomeAssistantMessage message) throws IOException {
        if (currentSession != null && currentSession.isOpen()) {
            sendMessage(currentSession, message);
        } else {
            log.warn("Cannot send message: no active session");
        }
    }

    /**
     * Get the next message ID for outbound messages.
     */
    public long nextMessageId() {
        return messageIdCounter.getAndIncrement();
    }

    /**
     * Check if currently authenticated.
     */
    public boolean isAuthenticated() {
        return currentSession != null && currentSession.isOpen();
    }

    /**
     * Publish the authenticated event to inform dependent subsystems.
     */
    private void publishAuthenticatedEvent() {
        eventPublisher.publishEvent(new HomeAssistantAuthenticatedEvent(this));
    }

    /**
     * Send message helper: serialize and transmit.
     */
    private void sendMessage(WebSocketSession session, HomeAssistantMessage message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(json));
        log.debug("Sent message: type={}, id={}", message.type(), message.id());
    }

    /**
     * Reconnect scheduler with bounded backoff.
     */
    private static class ReconnectScheduler {
        private static final long[] BACKOFF_DELAYS = {
            1000,      // 1 second
            2000,      // 2 seconds
            5000,      // 5 seconds
            10000,     // 10 seconds
            30000,     // 30 seconds
            60000      // 60 seconds (max)
        };

        private int backoffIndex = 0;
        private final Runnable reconnectAction;

        ReconnectScheduler(Runnable reconnectAction) {
            this.reconnectAction = reconnectAction;
        }

        void scheduleReconnect() {
            long delay = BACKOFF_DELAYS[Math.min(backoffIndex, BACKOFF_DELAYS.length - 1)];
            backoffIndex++;
            log.info("Scheduling reconnect in {}ms (attempt #{})", delay, backoffIndex);
            new Thread(() -> {
                try {
                    Thread.sleep(delay);
                    reconnectAction.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        void reset() {
            backoffIndex = 0;
        }
    }
}
