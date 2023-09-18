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
import java.awt.Image
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.*
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.Timer

class BongoCatToolWindow : ToolWindowFactory,FileEditorManagerListener, ComponentAdapter() {
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




    init {
        idleTimer.isRepeats = false
    }

    //Debounce 적용하기
    //이미지 아이콘 크기 조절 메서드
    private fun resizeImageIcon(toolWindow: ToolWindow){
        val toolWindowWidth = toolWindow.component.width
        val toolWindowHeight = toolWindow.component.height


        if(toolWindowWidth>0&&toolWindowHeight>20){
            val originalWidth = bongoLeft.image.getWidth(null)
            val originalHeight = bongoLeft.image.getHeight(null)



            // 원본 이미지 비율 계산
            val widthRatio = toolWindowWidth.toDouble() / originalWidth
            val heightRatio = toolWindowHeight.toDouble() / originalHeight

            println("original width : $originalWidth")
            println("original height : $originalHeight")
            println("label width : $toolWindowWidth")
            println("label height : $toolWindowHeight")
            println("widthRatio : $widthRatio")
            println("heightRatio : $heightRatio")

            // 비율 유지하며 크기 조절
            val scaledBongoLeft = bongoLeft.image.getScaledInstance(
                (originalWidth * widthRatio).toInt()-50,
                (originalHeight * heightRatio).toInt()-20,
                Image.SCALE_SMOOTH
            )

            // 나머지 이미지도 같은 비율로 크기 조절
            val scaledBongoRight = bongoRight.image.getScaledInstance(
                (originalWidth * widthRatio).toInt()-50,
                (originalHeight * heightRatio).toInt()-20,
                Image.SCALE_SMOOTH
            )
            val scaledBongoMiddle = bongoMiddle.image.getScaledInstance(
                (originalWidth * widthRatio).toInt()-50,
                (originalHeight * heightRatio).toInt()-20,
                Image.SCALE_SMOOTH
            )
            println("scaledbongoleft width : ${scaledBongoLeft.getWidth(null)}")
            println("scaledbongoleft height : ${scaledBongoLeft.getHeight(null)}")

            println("bongoleft width : ${bongoLeft.image.getWidth(null)}")
            println("bongoleft height : ${bongoLeft.image.getHeight(null)}")

            bongoLeft.image = scaledBongoLeft
            bongoRight.image = scaledBongoRight
            bongoMiddle.image = scaledBongoMiddle

            label.repaint()
        }
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

        toolWindow.component.addComponentListener(object: ComponentAdapter(){
            override fun componentResized(e: ComponentEvent?) {
                resizeImageIcon(toolWindow)
            }
        })
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