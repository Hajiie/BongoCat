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
import com.intellij.util.ui.ImageUtil
import com.intellij.util.ui.UIUtil
import java.awt.Image
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.Timer

class BongoCatToolWindow : ToolWindowFactory,FileEditorManagerListener, ComponentAdapter() {
    // 이미지 아이콘
    private var bongoLeft = ImageIcon(ImageIO.read(javaClass.classLoader.getResource("BongoCat_img/bongo_left.png")))
    private var bongoRight = ImageIcon(ImageIO.read(javaClass.classLoader.getResource("BongoCat_img/bongo_right.png")))
    private var bongoMiddle =
            ImageIcon(ImageIO.read(javaClass.classLoader.getResource("BongoCat_img/bongo_middle.png")))


    private var scaledBongoLeft = bongoLeft
    private var scaledBongoRight = bongoRight
    private var scaledBongoMiddle = bongoMiddle
    // 키 입력 시간을 저장하는 큐
    private val keyPressTimes: LinkedList<Long> = LinkedList()

    // 500ms 이내의 키 입력을 감지하기 위한 타이머
    private val idleTimer: Timer
    private val label: JLabel = JLabel()

    // DocumentListener를 저장하는 맵
    private val documentListenersMap: MutableMap<Document, DocumentListener> = mutableMapOf()

    private var resizeTimer : Timer? = null

    // 생성
    init {
        label.icon = scaledBongoMiddle

        idleTimer = Timer(500) {
            label.icon = scaledBongoMiddle
        }

        idleTimer.isRepeats = false
    }



    //Debounce 적용하기

    //이미지 아이콘 크기 조절 메서드
    private fun resizeImageIcon(toolWindow: ToolWindow){
        val toolWindowWidth = toolWindow.component.width
        val toolWindowHeight = toolWindow.component.height


        if(toolWindowWidth>0&&toolWindowHeight>0){
            val originalWidth = bongoLeft.image.getWidth(null)
            val originalHeight = bongoLeft.image.getHeight(null)

            // 원본 이미지 비율 계산
            val widthRatio = toolWindowWidth.toDouble() / originalWidth
            val heightRatio = toolWindowHeight.toDouble() / originalHeight

            // 더 작은 비율로 이미지 크기 조절
            val minRatio = minOf(widthRatio, heightRatio)


            val scaledWidth= (originalWidth * minRatio).toInt()
            val scaledHeight = (originalHeight * minRatio).toInt()


            // 비율 유지하며 크기 조절
            scaledBongoLeft = createScaledImage(bongoLeft, scaledWidth, scaledHeight)

            // 나머지 이미지도 같은 비율로 크기 조절
            scaledBongoRight = createScaledImage(bongoRight, scaledWidth, scaledHeight)
            scaledBongoMiddle = createScaledImage(bongoMiddle, scaledWidth, scaledHeight)


            label.revalidate()
            label.repaint()
        }
    }

    private fun createScaledImage(icon: Icon, width:Int, height:Int):ImageIcon{
        val image = (icon as ImageIcon).image
        val scaledImage = ImageUtil.createImage(width, height, BufferedImage.TYPE_INT_ARGB)
        return ImageIcon(scaledImage)
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
                        label.icon = if (label.icon === scaledBongoLeft) scaledBongoRight else scaledBongoLeft
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
                resizeTimer?.stop()

                resizeTimer = Timer(500) {
                    resizeImageIcon(toolWindow)
                }
                resizeTimer?.isRepeats = false
                resizeTimer?.start()
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