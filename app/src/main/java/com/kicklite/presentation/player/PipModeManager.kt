package com.kicklite.presentation.player

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipModeManager @Inject constructor() {
    
    private val _pipModeEvents = MutableSharedFlow<Boolean>(replay = 1)
    val pipModeEvents: SharedFlow<Boolean> = _pipModeEvents.asSharedFlow()
    
    fun setPipMode(isInPip: Boolean) {
        _pipModeEvents.tryEmit(isInPip)
    }
}
