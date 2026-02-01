package com.terminalindex.gitclock.data

enum class ComponentId {
    CLOCK,
    BATTERY, 
    STATS,
    COMMIT_BOARD
}

data class ComponentLayout(
    val id: ComponentId,
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f
)

data class LayoutConfig(
    val components: Map<ComponentId, ComponentLayout> = emptyMap()
)
