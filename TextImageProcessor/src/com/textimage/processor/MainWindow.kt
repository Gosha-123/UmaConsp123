package com.textimage.processor

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

class MainWindow : JFrame("Text Image Processor") {

    private val imagePanel = ImagePanel()
    private val controlPanel = ControlPanel()
    private var currentImage: BufferedImage? = null
    private var originalImage: BufferedImage? = null

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        preferredSize = Dimension(1200, 700)
        layout = BorderLayout()

        // Панель инструментов
        val toolBar = JToolBar().apply {
            isFloatable = false
            add(createButton("Open", "Открыть изображение") { openImage() })
            add(createButton("Save", "Сохранить как...") { saveImage() })
            addSeparator()
            add(createButton("Reset", "Сбросить изменения") { resetImage() })
        }
        add(toolBar, BorderLayout.NORTH)

        // Центральная область с изображением
        add(JScrollPane(imagePanel).apply {
            preferredSize = Dimension(800, 600)
        }, BorderLayout.CENTER)

        // Панель управления справа
        add(controlPanel, BorderLayout.EAST)

        // Подключаем обработчики событий от панели управления
        controlPanel.onApply { params ->
            currentImage?.let { img ->
                val processed = ImageProcessor.process(img, params)
                imagePanel.setImage(processed)
                currentImage = processed
            }
        }
        controlPanel.onZoom { factor ->
            imagePanel.zoom(factor)
        }

        pack()
        setLocationRelativeTo(null)
    }

    private fun createButton(text: String, tooltip: String, action: () -> Unit): JButton {
        return JButton(text).apply {
            this.toolTipText = tooltip
            addActionListener { action() }
        }
    }

    private fun openImage() {
        val fileChooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter(
                "Изображения",
                *ImageIO.getReaderFileSuffixes()
            )
        }

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            try {
                originalImage = ImageIO.read(file)
                currentImage = originalImage
                imagePanel.setImage(originalImage)
                controlPanel.resetSliders()
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(
                    this,
                    "Не удалось загрузить изображение: ${e.message}",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }

    private fun saveImage() {
        currentImage?.let { img ->
            val fileChooser = JFileChooser().apply {
                fileFilter = FileNameExtensionFilter("PNG Image", "png")
            }
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                val file = fileChooser.selectedFile
                val outputFile = if (file.extension.isEmpty()) File("${file.absolutePath}.png") else file
                try {
                    ImageIO.write(img, "png", outputFile)
                    JOptionPane.showMessageDialog(this, "Изображение сохранено!")
                } catch (e: Exception) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Ошибка сохранения: ${e.message}",
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }

    private fun resetImage() {
        originalImage?.let {
            currentImage = it
            imagePanel.setImage(it)
            controlPanel.resetSliders()
        }
    }
}