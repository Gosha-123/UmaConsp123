package com.textimage.processor

import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main() {
    // Устанавливаем системный Look & Feel для привычного вида
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (e: Exception) {
        e.printStackTrace()
    }

    SwingUtilities.invokeLater {
        MainWindow().isVisible = true
    }
}