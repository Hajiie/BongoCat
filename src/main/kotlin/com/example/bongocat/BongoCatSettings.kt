package com.example.bongocat

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

// IDE를 다시 시작해도 유지되어야 하는 Bongo Cat 설정을 저장한다.
@Service(Service.Level.APP)
@State(
    name = "BongoCatSettings",
    storages = [Storage("BongoCatSettings.xml")],
)
class BongoCatSettings : PersistentStateComponent<BongoCatSettings.State> {
    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    var soundMode: SoundMode
        get() = runCatching { SoundMode.valueOf(state.soundMode) }.getOrDefault(
            if (state.soundEnabled) SoundMode.BONGO else SoundMode.OFF
        )
        set(value) {
            state.soundMode = value.name
            state.soundEnabled = value != SoundMode.OFF
        }

    data class State(
        var soundEnabled: Boolean = false,
        var soundMode: String = SoundMode.OFF.name,
    )

    companion object {
        fun getInstance(): BongoCatSettings =
            ApplicationManager.getApplication().getService(BongoCatSettings::class.java)
    }
}

enum class SoundMode(
    val label: String,
) {
    OFF("Off"),
    BONGO("Bongo"),
    KEYBOARD("Keyboard");

    override fun toString(): String = label
}
