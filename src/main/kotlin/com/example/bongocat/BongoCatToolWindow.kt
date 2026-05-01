package com.example.bongocat

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Image
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer

class BongoCatToolWindow : ToolWindowFactory {
    // ToolWindowFactory는 상태를 가지지 않고, 실제 UI 상태는 content 객체가 관리한다.
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        BongoCatToolWindowContent(project, toolWindow).create()
    }
}

// Bongo Cat Tool Window의 UI, 이미지, 리스너, 타이머 상태를 관리하는 클래스
private class BongoCatToolWindowContent(
    private val project: Project,
    private val toolWindow: ToolWindow,
) {
    // 원본 이미지 아이콘
    private val originalIcons = BongoCatIcons(
        left = loadIcon("BongoCat_img/bongo_left.png"),
        middle = loadIcon("BongoCat_img/bongo_middle.png"),
        right = loadIcon("BongoCat_img/bongo_right.png"),
    )
    // Tool Window 크기에 맞춰 조절된 이미지 아이콘
    private var scaledIcons = originalIcons

    // 좌/우 이미지에 맞춰 재생할 Bongo Cat 효과음
    private val soundPlayer = BongoCatSoundPlayer()

    // IDE 재시작 후에도 유지되는 사용자 설정
    private val settings = BongoCatSettings.getInstance()

    // Bongo Cat 이미지를 보여주는 라벨
    private val label = JLabel(scaledIcons.middle)

    // 입력이 멈추면 가운데 이미지로 되돌리는 타이머
    private val idleTimer = Timer(IDLE_DELAY_MS) {
        label.icon = scaledIcons.middle
    }.apply {
        isRepeats = false
    }
    private val soundTimer = Timer(SOUND_DELAY_MS) {
        playPendingSound()
    }.apply {
        isRepeats = false
    }

    private var resizeTimer: Timer? = null
    private var pendingKeyboardSoundType: KeyboardSoundType? = null
    private var pendingBongoHit: Boolean = false

    // Tool Window에 들어갈 콘텐츠와 필요한 리스너를 생성한다.
    fun create() {
        val disposable = Disposer.newDisposable("BongoCat tool window")
        val registeredDocuments = mutableSetOf<Document>()

        val panel = JPanel(BorderLayout()).apply {
            add(createSoundToolbar(), BorderLayout.NORTH)
            add(label, BorderLayout.CENTER)
            isFocusable = true
        }

        val content = ContentFactory.getInstance().createContent(panel, "", false).apply {
            setDisposer(disposable)
        }
        toolWindow.contentManager.addContent(content)

        // 리사이즈 이벤트는 짧게 debounce해서 이미지 스케일링 비용을 줄인다.
        val resizeListener = object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                scheduleResize()
            }
        }
        toolWindow.component.addComponentListener(resizeListener)

        // 새로 열린 파일에도 DocumentListener를 등록한다.
        val editorListener = object : FileEditorManagerListener {
            override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                registerDocumentListener(file, disposable, registeredDocuments)
            }
        }
        project.messageBus.connect(disposable)
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, editorListener)

        // Tool Window 콘텐츠가 제거될 때 타이머와 Swing 리스너를 정리한다.
        Disposer.register(disposable) {
            idleTimer.stop()
            soundTimer.stop()
            resizeTimer?.stop()
            soundPlayer.dispose()
            toolWindow.component.removeComponentListener(resizeListener)
        }

        // 이미 열려 있는 파일에 대한 DocumentListener도 등록한다.
        FileEditorManager.getInstance(project).openFiles.forEach { file ->
            registerDocumentListener(file, disposable, registeredDocuments)
        }

        panel.requestFocusInWindow()
        SwingUtilities.invokeLater {
            resizeImageIcon()
        }
        updateSoundKeepAlive()
    }

    // 이미지 리사이즈 요청을 debounce한다.
    private fun scheduleResize() {
        resizeTimer?.stop()
        resizeTimer = Timer(RESIZE_DELAY_MS) {
            resizeImageIcon()
        }.apply {
            isRepeats = false
            start()
        }
    }

    // 원본 비율을 유지하며 Tool Window 크기에 맞게 이미지를 조절한다.
    private fun resizeImageIcon() {
        val component = toolWindow.component
        if (component.width <= 0 || component.height <= 0) {
            return
        }

        val originalWidth = originalIcons.left.iconWidth
        val originalHeight = originalIcons.left.iconHeight
        val scale = minOf(
            component.width.toDouble() / originalWidth,
            component.height.toDouble() / originalHeight,
        )

        val scaledWidth = (originalWidth * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (originalHeight * scale).toInt().coerceAtLeast(1)

        scaledIcons = BongoCatIcons(
            left = createScaledImage(originalIcons.left, scaledWidth, scaledHeight),
            middle = createScaledImage(originalIcons.middle, scaledWidth, scaledHeight),
            right = createScaledImage(originalIcons.right, scaledWidth, scaledHeight),
        )

        label.icon = scaledIcons.middle
        label.revalidate()
        label.repaint()
    }

    // 파일의 Document 변경 이벤트를 감지해 좌/우 이미지를 번갈아 보여준다.
    private fun registerDocumentListener(
        file: VirtualFile,
        disposable: Disposable,
        registeredDocuments: MutableSet<Document>,
    ) {
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return
        if (!registeredDocuments.add(document)) {
            return
        }

        document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                idleTimer.restart()

                when (settings.soundMode) {
                    SoundMode.BONGO -> scheduleBongoHit()
                    SoundMode.KEYBOARD -> {
                        label.icon = nextBongoIcon()
                        scheduleKeyboardSound(event.keyboardSoundType())
                    }
                    SoundMode.OFF -> {
                        label.icon = nextBongoIcon()
                    }
                }
            }
        }, disposable)
    }

    // 효과음 선택 메뉴를 Tool Window 상단에 배치한다.
    private fun createSoundToolbar(): JPanel =
        JPanel(FlowLayout(FlowLayout.RIGHT, 8, 4)).apply {
            isOpaque = false
            add(createSoundModeSelector())
        }

    // 사용할 효과음 종류를 선택하는 콤보박스를 생성한다.
    private fun createSoundModeSelector(): JComboBox<SoundMode> =
        JComboBox(SoundMode.entries.toTypedArray()).apply {
            selectedItem = settings.soundMode
            addActionListener {
                settings.soundMode = selectedItem as SoundMode
                updateSoundKeepAlive()
            }
        }

    // 사운드 설정에 따라 첫 재생 지연을 줄이는 keep-alive 상태를 맞춘다.
    private fun updateSoundKeepAlive() {
        if (settings.soundMode != SoundMode.OFF) {
            soundPlayer.startKeepAlive()
        } else {
            soundPlayer.stopKeepAlive()
        }
    }

    // 한글 IME처럼 짧은 시간에 몰린 DocumentEvent를 봉고 타격 하나로 합친다.
    private fun scheduleBongoHit() {
        pendingBongoHit = true
        pendingKeyboardSoundType = null
        soundTimer.restart()
    }

    // IME 조합처럼 짧은 시간에 몰린 DocumentEvent는 마지막 소리 하나로 합친다.
    private fun scheduleKeyboardSound(keyboardSoundType: KeyboardSoundType) {
        pendingBongoHit = false
        pendingKeyboardSoundType = keyboardSoundType
        soundTimer.restart()
    }

    // 대기 중인 효과음을 실제로 재생한다.
    private fun playPendingSound() {
        val shouldPlayBongo = pendingBongoHit
        val keyboardSoundType = pendingKeyboardSoundType

        pendingBongoHit = false
        pendingKeyboardSoundType = null

        if (shouldPlayBongo && settings.soundMode == SoundMode.BONGO) {
            val nextIcon = nextBongoIcon()
            label.icon = nextIcon
            playBongoSoundFor(nextIcon)
        } else if (keyboardSoundType != null && settings.soundMode == SoundMode.KEYBOARD) {
            soundPlayer.playKeyboard(keyboardSoundType)
        }
    }

    // 현재 이미지 방향에 맞춰 봉고 효과음을 즉시 재생한다.
    private fun playBongoSoundFor(icon: ImageIcon) {
        if (icon == scaledIcons.left) {
            soundPlayer.playLeft()
        } else if (icon == scaledIcons.right) {
            soundPlayer.playRight()
        }
    }

    // 현재 이미지의 반대 방향 봉고 이미지를 반환한다.
    private fun nextBongoIcon(): ImageIcon =
        if (label.icon == scaledIcons.right) {
            scaledIcons.left
        } else {
            scaledIcons.right
        }

    // Document 변경 내용을 기준으로 알맞은 키보드 효과음을 고른다.
    private fun DocumentEvent.keyboardSoundType(): KeyboardSoundType {
        val newText = newFragment.toString()
        val oldText = oldFragment.toString()

        return when {
            newText.contains('\n') -> KeyboardSoundType.ENTER
            newText.contains(' ') -> KeyboardSoundType.SPACE
            newText.isEmpty() && oldText.isNotEmpty() -> KeyboardSoundType.BACKSPACE
            else -> KeyboardSoundType.CHARACTER
        }
    }

    // 리소스에서 이미지 파일을 읽어 ImageIcon으로 변환한다.
    private fun loadIcon(path: String): ImageIcon {
        val resource = javaClass.classLoader.getResource(path)
            ?: error("BongoCat image resource is missing: $path")

        return ImageIcon(ImageIO.read(resource))
    }

    // ImageIcon의 실제 이미지를 지정한 크기로 스케일링한다.
    private fun createScaledImage(icon: Icon, width: Int, height: Int): ImageIcon {
        val image = (icon as ImageIcon).image
        val scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH)
        return ImageIcon(scaledImage)
    }

    private data class BongoCatIcons(
        val left: ImageIcon,
        val middle: ImageIcon,
        val right: ImageIcon,
    )

    private companion object {
        private const val IDLE_DELAY_MS = 500
        private const val RESIZE_DELAY_MS = 500
        private const val SOUND_DELAY_MS = 20
    }
}
