package com.example.bongocat

import java.io.BufferedInputStream
import java.util.concurrent.ThreadLocalRandom
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

// Bongo Cat의 좌/우 입력 효과음을 재생한다.
class BongoCatSoundPlayer {
    private val leftClips = createClipPool("BongoCat_sound/BongoSound/BongoSoundL.wav")
    private val rightClips = createClipPool("BongoCat_sound/BongoSound/BongoSoundR.wav")
    private val keyboardClips = KEYBOARD_SOUND_PATHS.map { createClipPool(it) }
    private val spaceClips = createClipPool("BongoCat_sound/KeyboardSound/Keyboard_Space.wav")
    private val enterClips = createClipPool("BongoCat_sound/KeyboardSound/Keyboard_Enter.wav")
    private val backspaceClips = createClipPool("BongoCat_sound/KeyboardSound/Keyboard_BackSpace.wav")
    private val keepAliveClip = createSilentClip()

    fun playLeft() {
        play(leftClips)
    }

    fun playRight() {
        play(rightClips)
    }

    fun playKeyboard(type: KeyboardSoundType) {
        when (type) {
            KeyboardSoundType.CHARACTER -> playRandomKeyboard()
            KeyboardSoundType.SPACE -> play(spaceClips)
            KeyboardSoundType.ENTER -> play(enterClips)
            KeyboardSoundType.BACKSPACE -> play(backspaceClips)
        }
    }

    // 사운드가 켜져 있을 때 무음 클립을 반복 재생해서 첫 타건 지연을 줄인다.
    @Synchronized
    fun startKeepAlive() {
        if (!keepAliveClip.isRunning) {
            keepAliveClip.loop(Clip.LOOP_CONTINUOUSLY)
        }
    }

    // 사운드가 꺼지면 무음 keep-alive도 멈춘다.
    @Synchronized
    fun stopKeepAlive() {
        keepAliveClip.stop()
        keepAliveClip.framePosition = 0
    }

    // Tool Window가 정리될 때 열어둔 오디오 리소스를 닫는다.
    @Synchronized
    fun dispose() {
        stopKeepAlive()
        val keyboardPoolClips = keyboardClips.flatten()
        (leftClips + rightClips + keyboardPoolClips + spaceClips + enterClips + backspaceClips + keepAliveClip).forEach { clip ->
            clip.stop()
            clip.close()
        }
    }

    // 일반 문자 입력은 여러 키보드 사운드 중 하나를 랜덤으로 재생한다.
    private fun playRandomKeyboard() {
        val index = ThreadLocalRandom.current().nextInt(keyboardClips.size)
        play(keyboardClips[index])
    }

    // 빠른 연타에도 소리가 겹쳐 나도록 재생 가능한 Clip을 하나 골라 즉시 재생한다.
    @Synchronized
    private fun play(clips: List<Clip>) {
        val clip = clips.firstOrNull { !it.isRunning } ?: clips.first()

        clip.stop()
        clip.framePosition = 0
        clip.start()
    }

    // 같은 효과음을 여러 Clip으로 미리 열어두어 재생 지연을 줄인다.
    private fun createClipPool(path: String): List<Clip> =
        List(CLIP_POOL_SIZE) {
            createClip(path)
        }

    // 리소스에서 wav 파일을 읽고 즉시 재생 가능한 Clip으로 준비한다.
    private fun createClip(path: String): Clip {
        val resource = javaClass.classLoader.getResource(path)
            ?: error("BongoCat sound resource is missing: $path")

        val clip = AudioSystem.getClip()
        BufferedInputStream(resource.openStream()).use { inputStream ->
            AudioSystem.getAudioInputStream(inputStream).use { audioInputStream ->
                clip.open(audioInputStream)
            }
        }

        return clip
    }

    // 실제 소리는 내지 않고 오디오 라인만 활성 상태로 유지하는 짧은 무음 Clip을 만든다.
    private fun createSilentClip(): Clip {
        val format = AudioFormat(
            SAMPLE_RATE,
            SAMPLE_SIZE_BITS,
            CHANNELS,
            true,
            false,
        )
        val silentBuffer = ByteArray((SAMPLE_RATE / 10).toInt() * CHANNELS * BYTES_PER_SAMPLE)
        val clip = AudioSystem.getClip()
        clip.open(format, silentBuffer, 0, silentBuffer.size)
        return clip
    }

    private companion object {
        private const val CLIP_POOL_SIZE = 6
        private const val SAMPLE_RATE = 44_100f
        private const val SAMPLE_SIZE_BITS = 16
        private const val CHANNELS = 1
        private const val BYTES_PER_SAMPLE = SAMPLE_SIZE_BITS / 8
        private val KEYBOARD_SOUND_PATHS = (1..13).map { number ->
            "BongoCat_sound/KeyboardSound/Keyboard_$number.wav"
        }
    }
}

enum class KeyboardSoundType {
    CHARACTER,
    SPACE,
    ENTER,
    BACKSPACE,
}
