package com.textimage.processor

import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.JPanel

class ImagePanel : JPanel() {

    private var image: BufferedImage? = null
    private var scale = 1.0

    fun setImage(img: BufferedImage?) {
        image = img
        scale = 1.0
        preferredSize = img?.let { Dimension(it.width, it.height) } ?: Dimension(400, 300)
        revalidate()
        repaint()
    }

    fun zoom(factor: Double) {
        scale *= factor
        scale = scale.coerceIn(0.1, 10.0)
        image?.let {
            preferredSize = Dimension((it.width * scale).toInt(), (it.height * scale).toInt())
        }
        revalidate()
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        image?.let { img ->
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            val panelWidth = width
            val panelHeight = height
            val imgWidth = (img.width * scale).toInt()
            val imgHeight = (img.height * scale).toInt()

            // Центрирование изображения
            val x = (panelWidth - imgWidth) / 2
            val y = (panelHeight - imgHeight) / 2

            g2.drawImage(img, x, y, imgWidth, imgHeight, null)
        }
    }
}