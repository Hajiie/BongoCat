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
import java.awt.Image
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.ImageIcon
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

    // Bongo Cat 이미지를 보여주는 라벨
    private val label = JLabel(scaledIcons.middle)

    // 입력이 멈추면 가운데 이미지로 되돌리는 타이머
    private val idleTimer = Timer(IDLE_DELAY_MS) {
        label.icon = scaledIcons.middle
    }.apply {
        isRepeats = false
    }

    private var resizeTimer: Timer? = null

    // Tool Window에 들어갈 콘텐츠와 필요한 리스너를 생성한다.
    fun create() {
        val disposable = Disposer.newDisposable("BongoCat tool window")
        val registeredDocuments = mutableSetOf<Document>()

        val panel = JPanel(BorderLayout()).apply {
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
            resizeTimer?.stop()
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
                label.icon = if (label.icon == scaledIcons.right) {
                    scaledIcons.left
                } else {
                    scaledIcons.right
                }
            }
        }, disposable)
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
    }
}
