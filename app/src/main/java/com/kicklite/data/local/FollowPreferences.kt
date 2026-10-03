package com.kicklite.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "follow_preferences")

@Singleton
class FollowPreferences @Inject constructor(
    private val context: Context
) {
    private val FOLLOWED_CHANNELS_KEY = stringSetPreferencesKey("followed_channels")
    private val FOLLOWED_USERNAMES_KEY = stringPreferencesKey("followed_usernames")

    data class FollowedChannel(
        val channelId: String,
        val username: String
    )

    fun getFollowedChannels(): Flow<Set<String>> {
        return context.dataStore.data.map { preferences ->
            preferences[FOLLOWED_CHANNELS_KEY] ?: emptySet()
        }
    }

    fun getFollowedUsernames(): Flow<Map<String, String>> {
        return context.dataStore.data.map { preferences ->
            val usernamesString = preferences[FOLLOWED_USERNAMES_KEY] ?: ""
            if (usernamesString.isEmpty()) {
                emptyMap()
            } else {
                usernamesString.split(",").associate { pair ->
                    val parts = pair.split(":")
                    if (parts.size == 2) {
                        parts[0] to parts[1] // channelId to username
                    } else {
                        "" to ""
                    }
                }.filterKeys { it.isNotEmpty() }
            }
        }
    }

    fun getFollowedChannelsWithUsernames(): Flow<List<FollowedChannel>> {
        return combine(getFollowedChannels(), getFollowedUsernames()) { channels, usernames ->
            channels.mapNotNull { channelId ->
                val username = usernames[channelId]
                if (username != null) {
                    FollowedChannel(channelId, username)
                } else {
                    null
                }
            }
        }
    }

    suspend fun followChannel(channelId: String, username: String) {
        println("DEBUG: FollowPreferences - Following channel $channelId with username '$username'")
        context.dataStore.edit { preferences ->
            val currentFollowed = preferences[FOLLOWED_CHANNELS_KEY] ?: emptySet()
            preferences[FOLLOWED_CHANNELS_KEY] = currentFollowed + channelId
            println("DEBUG: Updated followed channels: ${currentFollowed + channelId}")
            
            // Update usernames mapping
            val currentUsernames = preferences[FOLLOWED_USERNAMES_KEY] ?: ""
            val usernameMap = if (currentUsernames.isEmpty()) {
                mutableMapOf<String, String>()
            } else {
                currentUsernames.split(",").associate { pair ->
                    val parts = pair.split(":")
                    if (parts.size == 2) parts[0] to parts[1] else "" to ""
                }.filterKeys { it.isNotEmpty() }.toMutableMap()
            }
            
            usernameMap[channelId] = username
            val newUsernamesString = usernameMap.entries.joinToString(",") { "${it.key}:${it.value}" }
            preferences[FOLLOWED_USERNAMES_KEY] = newUsernamesString
            println("DEBUG: Updated usernames mapping: $newUsernamesString")
        }
    }

    suspend fun followChannel(channelId: String) {
        // Backward compatibility - try to find username from existing data
        followChannel(channelId, "unknown_$channelId")
    }

    suspend fun unfollowChannel(channelId: String) {
        println("DEBUG: FollowPreferences - Unfollowing channel $channelId")
        context.dataStore.edit { preferences ->
            val currentFollowed = preferences[FOLLOWED_CHANNELS_KEY] ?: emptySet()
            preferences[FOLLOWED_CHANNELS_KEY] = currentFollowed - channelId
            println("DEBUG: Updated followed channels: ${currentFollowed - channelId}")
            
            // Remove from usernames mapping
            val currentUsernames = preferences[FOLLOWED_USERNAMES_KEY] ?: ""
            if (currentUsernames.isNotEmpty()) {
                val usernameMap = currentUsernames.split(",").associate { pair ->
                    val parts = pair.split(":")
                    if (parts.size == 2) parts[0] to parts[1] else "" to ""
                }.filterKeys { it.isNotEmpty() && it != channelId }.toMutableMap()
                
                val newUsernamesString = usernameMap.entries.joinToString(",") { "${it.key}:${it.value}" }
                preferences[FOLLOWED_USERNAMES_KEY] = newUsernamesString
                println("DEBUG: Updated usernames mapping: $newUsernamesString")
            }
        }
    }

    suspend fun isChannelFollowed(channelId: String): Boolean {
        val result = getFollowedChannels().map { followedChannels ->
            println("DEBUG: All followed channels: $followedChannels")
            println("DEBUG: Checking if channelId '$channelId' is in followed channels")
            followedChannels.contains(channelId)
        }.first()
        println("DEBUG: isChannelFollowed result for '$channelId': $result")
        return result
    }
}
