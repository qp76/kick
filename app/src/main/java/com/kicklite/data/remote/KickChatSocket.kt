package com.kicklite.data.remote

import com.kicklite.domain.model.ChatBadge
import com.kicklite.domain.model.ChatIdentity
import com.kicklite.domain.model.ChatMessage
import com.kicklite.domain.model.ChatSender
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Live Kick chat over Kick's Pusher WebSocket. No login needed. Reconnects by itself. */
@Singleton
class KickChatSocket @Inject constructor(client: OkHttpClient) {

    sealed interface Event {
        object Connected : Event
        object Disconnected : Event
        data class Message(val message: ChatMessage) : Event
        data class Pinned(val message: ChatMessage?) : Event
    }

    private val wsClient: OkHttpClient = client.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    fun connect(chatroomId: Long): Flow<Event> = callbackFlow {
        var socket: WebSocket? = null

        val job = launch {
            while (isActive) {
                val finished = CompletableDeferred<Unit>()
                val request = Request.Builder().url(URL).build()
                socket = wsClient.newWebSocket(request, object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        try {
                            val json = JSONObject(text)
                            val name = json.optString("event")
                            when {
                                name == "pusher:connection_established" -> {
                                    val sub = JSONObject()
                                        .put("event", "pusher:subscribe")
                                        .put(
                                            "data",
                                            JSONObject().put("auth", "").put("channel", "chatrooms.$chatroomId.v2")
                                        )
                                    webSocket.send(sub.toString())
                                }
                                name == "pusher_internal:subscription_succeeded" -> trySend(Event.Connected)
                                name == "pusher:ping" ->
                                    webSocket.send("""{"event":"pusher:pong","data":{}}""")
                                name.endsWith("PinnedMessageCreatedEvent") -> {
                                    val msg = JSONObject(json.optString("data")).optJSONObject("message")
                                    trySend(Event.Pinned(msg?.let { parseMessageObject(it) }))
                                }
                                name.endsWith("PinnedMessageDeletedEvent") -> trySend(Event.Pinned(null))
                                name.endsWith("ChatMessageEvent") -> {
                                    parseMessage(json.optString("data"))?.let { trySend(Event.Message(it)) }
                                }
                            }
                        } catch (_: Exception) {
                            // Ignore malformed frames
                        }
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        finished.complete(Unit)
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        finished.complete(Unit)
                    }
                })
                finished.await()
                trySend(Event.Disconnected)
                delay(3000)
            }
        }

        awaitClose {
            job.cancel()
            socket?.cancel()
        }
    }

    private fun parseMessage(data: String): ChatMessage? = parseMessageObject(JSONObject(data))

    private fun parseMessageObject(o: JSONObject): ChatMessage? {
        val id = o.optString("id")
        if (id.isEmpty()) return null
        val type = o.optString("type", "message")
        if (type != "message" && type != "reply") return null
        val s = o.optJSONObject("sender") ?: return null
        val identity = s.optJSONObject("identity")
        val badges = ArrayList<ChatBadge>()
        identity?.optJSONArray("badges")?.let { arr ->
            for (i in 0 until arr.length()) {
                val b = arr.optJSONObject(i) ?: continue
                badges.add(ChatBadge(b.optString("type"), b.optString("text"), if (b.has("count")) b.optInt("count") else null))
            }
        }
        val meta = o.opt("metadata")?.takeIf { it != JSONObject.NULL }?.toString()
        return ChatMessage(
            id = id,
            chatId = o.optLong("chatroom_id"),
            userId = s.optLong("id"),
            content = o.optString("content"),
            type = type,
            metadata = meta,
            sender = ChatSender(
                id = s.optLong("id"),
                slug = s.optString("slug"),
                username = s.optString("username"),
                identity = ChatIdentity(identity?.optString("color") ?: "", badges)
            ),
            createdAt = o.optString("created_at")
        )
    }

    private companion object {
        const val URL = "wss://ws-us2.pusher.com/app/32cbd69e4b950bf97679?protocol=7&client=js&version=8.4.0&flash=false"
    }
}
