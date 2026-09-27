package com.divergent.genesis

import kotlinx.coroutines.flow.MutableStateFlow

object AgentState {
    val currentTask = MutableStateFlow<String?>(null)
    val currentTargetApp = MutableStateFlow<String?>(null)
}
