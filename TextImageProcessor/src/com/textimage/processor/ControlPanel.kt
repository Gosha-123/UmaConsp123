package com.textimage.processor

import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*
import javax.swing.event.ChangeListener
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

data class ProcessParams(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val threshold: Int = 128,
    val deskew: Boolean = false,
    val targetWidth: Int = 0,
    val targetHeight: Int = 0,
    val keepAspect: Boolean = true
)

class ControlPanel : JPanel() {

    private val brightnessSlider = JSlider(-100, 100, 0)
    private val contrastSlider = JSlider(0, 200, 100)
    private val thresholdSlider = JSlider(0, 255, 128)
    private val widthField = JTextField(5)
    private val heightField = JTextField(5)
    private val keepAspectCheck = JCheckBox("Сохранять пропорции", true)
    private val deskewCheck = JCheckBox("Выровнять перекос (deskew)")

    private var onApplyCallback: ((ProcessParams) -> Unit)? = null

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createTitledBorder("Обработка изображения")
        preferredSize = Dimension(250, 0)

        // Яркость
        add(createLabeledComponent("Яркость:", brightnessSlider))
        brightnessSlider.paintTicks = true
        brightnessSlider.majorTickSpacing = 50
        brightnessSlider.paintLabels = true
        brightnessSlider.addChangeListener { notifyParamsChanged() }

        // Контраст
        add(createLabeledComponent("Контраст:", contrastSlider))
        contrastSlider.paintTicks = true
        contrastSlider.majorTickSpacing = 50
        contrastSlider.paintLabels = true
        contrastSlider.addChangeListener { notifyParamsChanged() }

        // Порог бинаризации (ч/б)
        add(createLabeledComponent("Порог ч/б:", thresholdSlider))
        thresholdSlider.paintTicks = true
        thresholdSlider.majorTickSpacing = 64
        thresholdSlider.paintLabels = true
        thresholdSlider.addChangeListener { notifyParamsChanged() }

        // Изменение размера
        val sizePanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(2, 2, 2, 2)
            anchor = GridBagConstraints.WEST
        }

        sizePanel.add(JLabel("Ширина:"), gbc)
        gbc.gridx = 1
        sizePanel.add(widthField, gbc)
        gbc.gridx = 2
        sizePanel.add(JLabel("Высота:"), gbc)
        gbc.gridx = 3
        sizePanel.add(heightField, gbc)
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 4
        sizePanel.add(keepAspectCheck, gbc)

        add(Box.createVerticalStrut(10))
        add(sizePanel)

        // Deskew
        add(Box.createVerticalStrut(10))
        add(deskewCheck)
        deskewCheck.addItemListener { notifyParamsChanged() }

        // Подписываемся на изменения текстовых полей
        val textFieldListener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = notifyParamsChanged()
            override fun removeUpdate(e: DocumentEvent) = notifyParamsChanged()
            override fun changedUpdate(e: DocumentEvent) = notifyParamsChanged()
        }
        widthField.document.addDocumentListener(textFieldListener)
        heightField.document.addDocumentListener(textFieldListener)
        keepAspectCheck.addItemListener { notifyParamsChanged() }

        // Кнопки масштабирования (не влияют на обработку, только на отображение)
        add(Box.createVerticalStrut(10))
        val zoomPanel = JPanel()
        zoomPanel.add(JButton("Zoom +").apply {
            addActionListener { fireZoom(1.2) }
        })
        zoomPanel.add(JButton("Zoom -").apply {
            addActionListener { fireZoom(0.8) }
        })
        zoomPanel.add(JButton("100%").apply {
            addActionListener { fireZoom(1.0) }
        })
        add(zoomPanel)

        // Кнопка "Сбросить" (опционально)
        add(Box.createVerticalStrut(15))
        val resetButton = JButton("Сбросить настройки")
        resetButton.alignmentX = CENTER_ALIGNMENT
        resetButton.addActionListener {
            resetSliders()
            notifyParamsChanged()
        }
        add(resetButton)
    }

    private fun createLabeledComponent(label: String, component: JComponent): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(JLabel(label))
            add(component)
            alignmentX = LEFT_ALIGNMENT
        }
    }

    private fun notifyParamsChanged() {
        onApplyCallback?.invoke(getCurrentParams())
    }

    private fun getCurrentParams(): ProcessParams {
        return ProcessParams(
            brightness = brightnessSlider.value / 100f,
            contrast = contrastSlider.value / 100f,
            threshold = thresholdSlider.value,
            deskew = deskewCheck.isSelected,
            targetWidth = widthField.text.toIntOrNull() ?: 0,
            targetHeight = heightField.text.toIntOrNull() ?: 0,
            keepAspect = keepAspectCheck.isSelected
        )
    }

    fun resetSliders() {
        brightnessSlider.value = 0
        contrastSlider.value = 100
        thresholdSlider.value = 128
        deskewCheck.isSelected = false
        widthField.text = ""
        heightField.text = ""
    }

    fun onApply(callback: (ProcessParams) -> Unit) {
        onApplyCallback = callback
    }

    // Калбэк для масштабирования (можно подключить в MainWindow)
    private var zoomCallback: ((Double) -> Unit)? = null
    fun onZoom(callback: (Double) -> Unit) {
        zoomCallback = callback
    }
    private fun fireZoom(factor: Double) {
        zoomCallback?.invoke(factor)
    }
}