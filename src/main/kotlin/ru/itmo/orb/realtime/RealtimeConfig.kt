package ru.itmo.orb.realtime

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.handler.TextWebSocketHandler
import ru.itmo.orb.service.DataChanged
import java.util.concurrent.ConcurrentHashMap

@Component
class EventWebSocket : TextWebSocketHandler() {
    private val log = LoggerFactory.getLogger(javaClass)
    private val sessions = ConcurrentHashMap.newKeySet<WebSocketSession>()
    override fun afterConnectionEstablished(session: WebSocketSession) {
        if (session.principal == null) { session.close(CloseStatus.POLICY_VIOLATION); return }
        sessions.add(session)
    }
    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) { sessions.remove(session) }
    override fun handleTransportError(session: WebSocketSession, exception: Throwable) { sessions.remove(session) }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onDataChanged(event: DataChanged) {
        // Only IDs / action are broadcast; client refetches authoritative data through REST.
        val message = "{\"entity\":\"${event.entity}\",\"id\":\"${event.id}\",\"action\":\"${event.action}\"}"
        for (session in sessions) {
            try {
                synchronized(session) { if (session.isOpen) session.sendMessage(TextMessage(message)) }
            } catch (ex: Exception) { sessions.remove(session); log.warn("WebSocket session disconnected") }
        }
    }
}

@Configuration
@EnableWebSocket
class WebSocketConfig(private val handler: EventWebSocket) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(handler, "/ws/events") // Default same-origin check + authenticated HTTP handshake
    }
}
