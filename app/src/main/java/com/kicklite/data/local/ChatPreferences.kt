package com.kicklite.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("chat_settings", Context.MODE_PRIVATE)

    private val _textSize = MutableStateFlow(prefs.getFloat(TEXT_SIZE, 15f))
    val textSize: StateFlow<Float> = _textSize.asStateFlow()

    private val _compact = MutableStateFlow(prefs.getBoolean(COMPACT, true))
    val compact: StateFlow<Boolean> = _compact.asStateFlow()

    private val _hideCommands = MutableStateFlow(prefs.getBoolean(HIDE_COMMANDS, false))
    val hideCommands: StateFlow<Boolean> = _hideCommands.asStateFlow()

    private val _showBadges = MutableStateFlow(prefs.getBoolean(SHOW_BADGES, true))
    val showBadges: StateFlow<Boolean> = _showBadges.asStateFlow()

    private val _bigEmotes = MutableStateFlow(prefs.getBoolean(BIG_EMOTES, false))
    val bigEmotes: StateFlow<Boolean> = _bigEmotes.asStateFlow()

    private val _hideBots = MutableStateFlow(prefs.getBoolean(HIDE_BOTS, false))
    val hideBots: StateFlow<Boolean> = _hideBots.asStateFlow()

    fun setHideBots(v: Boolean) { _hideBots.value = v; prefs.edit().putBoolean(HIDE_BOTS, v).apply() }

    fun setTextSize(v: Float) { _textSize.value = v; prefs.edit().putFloat(TEXT_SIZE, v).apply() }
    fun setCompact(v: Boolean) { _compact.value = v; prefs.edit().putBoolean(COMPACT, v).apply() }
    fun setHideCommands(v: Boolean) { _hideCommands.value = v; prefs.edit().putBoolean(HIDE_COMMANDS, v).apply() }
    fun setShowBadges(v: Boolean) { _showBadges.value = v; prefs.edit().putBoolean(SHOW_BADGES, v).apply() }
    fun setBigEmotes(v: Boolean) { _bigEmotes.value = v; prefs.edit().putBoolean(BIG_EMOTES, v).apply() }

    private companion object {
        const val HIDE_BOTS = "hide_bots"
        const val TEXT_SIZE = "text_size"
        const val COMPACT = "compact"
        const val HIDE_COMMANDS = "hide_commands"
        const val SHOW_BADGES = "show_badges"
        const val BIG_EMOTES = "big_emotes"
    }
}
