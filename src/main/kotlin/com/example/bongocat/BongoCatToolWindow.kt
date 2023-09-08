package com.example.bongocat

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
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
    // 이미지 아이콘
    private val bongoLeft = ImageIcon(ImageIO.read(javaClass.classLoader.getResource("BongoCat_img/bongo_left.png")))
    private val bongoRight = ImageIcon(ImageIO.read(javaClass.classLoader.getResource("BongoCat_img/bongo_right.png")))
    private val bongoMiddle =
            ImageIcon(ImageIO.read(javaClass.classLoader.getResource("BongoCat_img/bongo_middle.png")))

    // 키 입력 시간을 저장하는 큐
    private val keyPressTimes: LinkedList<Long> = LinkedList()

    // 500ms 이내의 키 입력을 감지하기 위한 타이머
    private val idleTimer: Timer
    private val label: JLabel = JLabel()

    // DocumentListener를 저장하는 맵
    private val documentListenersMap: MutableMap<Document, DocumentListener> = mutableMapOf()

    // 생성
    init {
        label.icon = bongoMiddle

        idleTimer = Timer(500) {
            label.icon = bongoMiddle
        }

        idleTimer.isRepeats = false


    }

    //registerDocumentListener 메서드
    private fun registerDocumentListener(file: VirtualFile) {
        val document = FileDocumentManager.getInstance().getDocument(file)
        if (document != null) {
            // 이미 등록된 DocumentListener인 경우 제거
            documentListenersMap[document]?.let {
                document.removeDocumentListener(it)
                documentListenersMap.remove(document)
            }

            // DocumentListener를 구현
            val documentListener = object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    idleTimer.restart()
                    val currentTime = System.currentTimeMillis()
                    keyPressTimes.addLast(currentTime)
                    keyPressTimes.removeIf { it < currentTime - 100 }

                    if (keyPressTimes.size >= 1) {
                        label.icon = if (label.icon === bongoLeft) bongoRight else bongoLeft
                    }
                }
            }

            // 새로운 DocumentListener 등록
            document.addDocumentListener(documentListener)
            documentListenersMap[document] = documentListener
        }
    }

    // ToolWindowFactory의 메서드를 구현
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel()

        panel.add(label)

        val contentFactory = ServiceManager.getService(ContentFactory::class.java)
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)

        // 포커스 설정
        panel.isFocusable = true
        panel.requestFocusInWindow()

        // FileEditorManagerListener 등록
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)

        // 현재 열려 있는 파일 에디터 목록에 대한 이벤트 리스너 등록
        val fileEditorManager = FileEditorManager.getInstance(project)
        for (file in fileEditorManager.openFiles) {
            registerDocumentListener(file)
        }

        fileEditorManager.addFileEditorManagerListener(this)
    }



    // FileEditorManagerListener의 메서드를 구현
    override fun fileOpenedSync(
            source: FileEditorManager,
            file: VirtualFile,
            editorsWithProviders: MutableList<FileEditorWithProvider>
    ) {
        // 중복 등록을 방지하고 DocumentListener를 등록
        registerDocumentListener(file)

    }
}