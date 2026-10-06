package com.kicklite.data.remote

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.kicklite.domain.model.LiveStream
import com.kicklite.domain.model.StreamCategory
import com.kicklite.domain.model.StreamChannel
import com.kicklite.domain.model.StreamThumbnail
import com.kicklite.domain.model.Pagination
import com.kicklite.domain.model.StreamsResponse

/** Defensive JSON mapping for the official API so small schema additions don't break the app. */
object OfficialApiParser {
    fun livestreams(root: JsonObject): StreamsResponse {
        val data = root.getAsJsonArray("data") ?: root.getAsJsonArray("livestreams")
        val list = data?.mapNotNull { it.asJsonObjectOrNull()?.let(::stream) } ?: emptyList()
        val pagination = root.getAsJsonObject("pagination")
            ?: root.getAsJsonObject("meta")
        val cursor = pagination?.firstString("next_cursor", "nextCursor", "cursor")
        return StreamsResponse(list, Pagination(cursor))
    }

    private fun stream(o: JsonObject): LiveStream {
        val channel = o.obj("channel")
        val category = o.obj("category")
        val thumbnail = o.obj("thumbnail")
        val channelId = o.long("broadcaster_user_id", "user_id") ?: channel?.long("user_id", "id") ?: 0L
        val channelSlug = o.string("channel_slug", "slug") ?: channel?.string("slug", "username") ?: ""
        val username = o.string("broadcaster_username", "username") ?: channel?.string("username", "slug") ?: channelSlug
        return LiveStream(
            id = o.string("id", "stream_id") ?: "",
            title = o.string("title", "session_title") ?: "",
            viewerCount = o.int("viewer_count", "viewers") ?: 0,
            thumbnail = thumbnail?.let { parseThumbnail(it) },
            startTime = o.string("started_at", "start_time", "created_at") ?: "",
            channel = StreamChannel(channelId, channelSlug, channel?.string("profile_picture", "profile_pic", "avatar") ?: "", username),
            category = StreamCategory(
                category?.long("id") ?: o.long("category_id") ?: 0L,
                category?.string("name", "title") ?: o.string("category_name") ?: "Unknown",
                category?.string("slug") ?: ""
            ),
            language = o.string("language") ?: "",
            isMature = o.bool("is_mature", "mature") ?: false,
            tags = o.arrayStrings("tags")
        )
    }

    private fun parseThumbnail(o: JsonObject): StreamThumbnail = StreamThumbnail(
        src = o.string("url", "src")
    )

    private fun JsonObject.obj(key: String): JsonObject? = get(key)?.asJsonObjectOrNull()
    private fun JsonObject.string(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
        get(key)?.takeIf { !it.isJsonNull }?.asString?.takeIf { it.isNotBlank() }
    }
    private fun JsonObject.long(vararg keys: String): Long? = keys.firstNotNullOfOrNull { key ->
        get(key)?.takeIf { !it.isJsonNull }?.let { runCatching { it.asLong }.getOrNull() }
    }
    private fun JsonObject.int(vararg keys: String): Int? = keys.firstNotNullOfOrNull { key ->
        get(key)?.takeIf { !it.isJsonNull }?.let { runCatching { it.asInt }.getOrNull() }
    }
    private fun JsonObject.bool(vararg keys: String): Boolean? = keys.firstNotNullOfOrNull { key ->
        get(key)?.takeIf { !it.isJsonNull }?.let { runCatching { it.asBoolean }.getOrNull() }
    }
    private fun JsonObject.arrayStrings(key: String): List<String> = getAsJsonArray(key)?.mapNotNull {
        runCatching { it.asString }.getOrNull()
    } ?: emptyList()
    private fun JsonObject.firstString(vararg keys: String): String? = string(*keys)
    private fun JsonElement.asJsonObjectOrNull(): JsonObject? = if (isJsonObject) asJsonObject else null
}
