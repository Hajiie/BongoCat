package com.example.bongocat

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.components.ServiceManager
import com.intellij.ui.content.ContentFactory
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.JLabel
import javax.swing.ImageIcon
import java.awt.event.ActionEvent
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.AbstractAction
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import java.awt.event.KeyEvent
import java.awt.event.KeyAdapter
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.text.Document
import javax.swing.Timer
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.DocumentEvent
import java.util.LinkedList


class BongoCatToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel()
        val label = JLabel()

//        val bongoLeft = ImageIcon(javaClass.classLoader.getResource("BongoCat_img/bongo_left.png"))
//        val bongoRight = ImageIcon(javaClass.classLoader.getResource("BongoCat_img/bongo_right.png"))
//        val bongoMiddle = ImageIcon(javaClass.classLoader.getResource("BongoCat_img/bongo_middle.png"))


        val bongoLeft = ImageIcon(ImageIO.read(javaClass.classLoader.getResource("BongoCat_img/bongo_left.png")))
        val bongoRight = ImageIcon(ImageIO.read(javaClass.classLoader.getResource("BongoCat_img/bongo_right.png")))
        val bongoMiddle = ImageIcon(ImageIO.read(javaClass.classLoader.getResource("BongoCat_img/bongo_middle.png")))

        label.icon = bongoMiddle

        panel.add(label)

        val contentFactory = ServiceManager.getService(ContentFactory::class.java)
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)

        // 포커스 설정
        panel.isFocusable = true
        panel.requestFocusInWindow()

        // 키 이벤트 추가
        var switchFlag = true
        val idleTimer = Timer(500) {
            label.icon = bongoMiddle
        }

        idleTimer.isRepeats = false

        // 키 입력 시간을 저장하는 큐
        val keyPressTimes: LinkedList<Long> = LinkedList()

        val documentListener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                idleTimer.restart()

                // 현재 시간을 큐에 추가
                val currentTime = System.currentTimeMillis()
                keyPressTimes.addLast(currentTime)

                // 100ms 이내의 키 입력만 유지
                keyPressTimes.removeIf { it < currentTime - 100}

                // 빠른 키 입력 감지
                if (keyPressTimes.size >= 1) {
                    label.icon = if (label.icon === bongoLeft) bongoRight else bongoLeft
                }
            }
        }

        // 모든 열려있는 에디터에 DocumentListener 추가
        val editorFactory = EditorFactory.getInstance()
        for (editor in editorFactory.allEditors) {
            editor.document.addDocumentListener(documentListener)
        }


    }
}
