package com.textimage.processor

import java.awt.image.BufferedImage

object ImageUtil {
    fun isBlack(image: BufferedImage, x: Int, y: Int, luminanceCutOff: Int = 140): Boolean {
        if (x < 0 || y < 0 || x >= image.width || y >= image.height) return false
        return try {
            val rgb = image.getRGB(x, y)
            val r = (rgb shr 16) and 0xFF
            val g = (rgb shr 8) and 0xFF
            val b = rgb and 0xFF
            val luminance = r * 0.299 + g * 0.587 + b * 0.114
            luminance < luminanceCutOff
        } catch (e: Exception) {
            false
        }
    }
}