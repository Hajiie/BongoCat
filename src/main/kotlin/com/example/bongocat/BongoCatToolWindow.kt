package com.example.bongocat

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.util.*
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.Timer

class BongoCatToolWindow : ToolWindowFactory,FileEditorManagerListener {
    private val bongoLeft = ImageIcon(ImageIO.read(javaClass.classLoader.getResource("BongoCat_img/bongo_left.png")))
    private val bongoRight = ImageIcon(ImageIO.read(javaClass.classLoader.getResource("BongoCat_img/bongo_right.png")))
    private val bongoMiddle =
        ImageIcon(ImageIO.read(javaClass.classLoader.getResource("BongoCat_img/bongo_middle.png")))

    // 키 입력 시간을 저장하는 큐
    private val keyPressTimes: LinkedList<Long> = LinkedList()
    // 모든 열려있는 에디터에 DocumentListener 추가

    private val idleTimer: Timer
    private val label: JLabel = JLabel()

    private val documentListeners : MutableList<DocumentListener> = mutableListOf()

    // 생성자
    init {
        label.icon = bongoMiddle

        idleTimer = Timer(500) {
            label.icon = bongoMiddle
        }

        idleTimer.isRepeats = false

    }


    // DocumentListener를 구현
    private val documentListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            idleTimer.restart()

            // 현재 시간을 큐에 추가
            val currentTime = System.currentTimeMillis()
            keyPressTimes.addLast(currentTime)

            // 100ms 이내의 키 입력만 유지
            keyPressTimes.removeIf { it < currentTime - 100 }

            // 빠른 키 입력 감지
            if (keyPressTimes.size >= 1) {
                label.icon = if (label.icon === bongoLeft) bongoRight else bongoLeft
            }
        }
    }


    // EditorFactoryListener의 메서드를 구현
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel()

        panel.add(label)

        val contentFactory = ServiceManager.getService(ContentFactory::class.java)
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)

        // 포커스 설정
        panel.isFocusable = true
        panel.requestFocusInWindow()

        val editorFactory = EditorFactory.getInstance()
        for (editor in editorFactory.allEditors) {
            editor.document.addDocumentListener(documentListener)
        }
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
    }

    override fun fileOpenedSync(
        source: FileEditorManager,
        file: VirtualFile,
        editorsWithProviders: MutableList<FileEditorWithProvider>
    ) {
        val document = source.selectedTextEditor?.document
        if(document!=null&&!documentListeners.contains(documentListener)){
            document.addDocumentListener(documentListener)
        }
    }

}
