package com.textimage.processor

import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.awt.image.RescaleOp

object ImageProcessor {

    fun process(src: BufferedImage, params: ProcessParams): BufferedImage {
        var image = src

        // 1. Deskew (выравнивание перекоса)
        if (params.deskew) {
            image = deskew(image)
        }

        // 2. Изменение размера
        if (params.targetWidth > 0 && params.targetHeight > 0) {
            image = resize(image, params.targetWidth, params.targetHeight, params.keepAspect)
        }

        // 3. Яркость и контраст
        if (params.brightness != 0f || params.contrast != 1f) {
            val scaleFactor = params.contrast
            val offset = params.brightness * 255
            val op = RescaleOp(scaleFactor, offset, null)
            image = op.filter(image, null)
        }

        // 4. Бинаризация (чёрно-белое по порогу)
        if (params.threshold != 128) { // 128 — нейтральное значение
            image = binarize(image, params.threshold)
        }

        return image
    }

    fun resize(src: BufferedImage, targetWidth: Int, targetHeight: Int, keepAspect: Boolean): BufferedImage {
        var w = targetWidth
        var h = targetHeight

        if (keepAspect) {
            val aspect = src.width.toDouble() / src.height.toDouble()
            if (w.toDouble() / h > aspect) {
                w = (h * aspect).toInt()
            } else {
                h = (w / aspect).toInt()
            }
        }

        val resized = BufferedImage(w, h, src.type)
        val g = resized.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(src, 0, 0, w, h, null)
        g.dispose()
        return resized
    }

    fun binarize(src: BufferedImage, threshold: Int): BufferedImage {
        val gray = BufferedImage(src.width, src.height, BufferedImage.TYPE_BYTE_GRAY)
        val g = gray.createGraphics()
        g.drawImage(src, 0, 0, null)
        g.dispose()

        val binary = BufferedImage(src.width, src.height, BufferedImage.TYPE_BYTE_BINARY)
        for (y in 0 until gray.height) {
            for (x in 0 until gray.width) {
                val rgb = gray.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val gVal = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF
                val luminance = (0.299 * r + 0.587 * gVal + 0.114 * b).toInt()
                val newRgb = if (luminance < threshold) Color.BLACK.rgb else Color.WHITE.rgb
                binary.setRGB(x, y, newRgb)
            }
        }
        return binary
    }

    fun deskew(src: BufferedImage): BufferedImage {
        val deskewer = ImageDeskew(src)
        val angle = deskewer.skewAngle
        if (Math.abs(angle) < 0.1) return src

        // Поворачиваем изображение на вычисленный угол
        val transform = AffineTransform()
        transform.rotate(Math.toRadians(angle), src.width / 2.0, src.height / 2.0)

        val op = AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR)
        val bounds = op.getBounds2D(src).bounds

        val result = BufferedImage(bounds.width, bounds.height, src.type)
        val g2 = result.createGraphics()
        g2.transform = AffineTransform.getTranslateInstance((-bounds.x).toDouble(), (-bounds.y).toDouble())
        g2.drawImage(src, op, 0, 0)
        g2.dispose()

        return result
    }
}