import org.bytedeco.opencv.opencv_core.*
import org.bytedeco.opencv.global.opencv_core
import org.bytedeco.opencv.global.opencv_imgcodecs
import org.bytedeco.opencv.global.opencv_imgproc
import java.io.File
import kotlin.math.abs

/**
 * Предобработчик изображений с текстом с использованием JavaCV (OpenCV).
 * Выполняет выравнивание (deskew), масштабирование, коррекцию яркости/контрастности и бинаризацию.
 */
object ImageTextPreprocessor {

    /**
     * Определение угла наклона изображения (в градусах) на основе линий Hough.
     * Возвращает угол в градусах (от -45 до 45).
     */
    fun detectSkewAngle(src: Mat): Double {
        // Преобразование в оттенки серого
        val gray = Mat()
        opencv_imgproc.cvtColor(src, gray, opencv_imgproc.COLOR_BGR2GRAY)

        // Бинаризация (Otsu)
        val binary = Mat()
        opencv_imgproc.threshold(gray, binary, 0.0, 255.0, opencv_imgproc.THRESH_BINARY_INV + opencv_imgproc.THRESH_OTSU)

        // Поиск линий с помощью преобразования Хафа
        val lines = Mat()
        opencv_imgproc.HoughLines(binary, lines, 1.0, Math.PI / 180.0, 150)

        if (lines.rows() == 0) return 0.0

        // Вычисление среднего угла
        var sumAngle = 0.0
        var count = 0
        for (i in 0 until lines.rows()) {
            val rho = lines.get(i, 0)[0]
            val theta = lines.get(i, 0)[1]
            // Преобразование радиан в градусы и нормализация (угол наклона от -45 до 45)
            var angle = theta * 180.0 / Math.PI - 90.0
            if (abs(angle) > 45.0) angle = 90.0 - angle
            if (abs(angle) < 45.0) {
                sumAngle += angle
                count++
            }
        }

        return if (count > 0) sumAngle / count else 0.0
    }

    /**
     * Поворот изображения на заданный угол (в градусах) без обрезки.
     */
    fun rotateImage(src: Mat, angle: Double): Mat {
        val center = Point(src.cols() / 2.0, src.rows() / 2.0)
        val rotMat = opencv_imgproc.getRotationMatrix2D(center, angle, 1.0)
        val dst = Mat()
        opencv_imgproc.warpAffine(src, dst, rotMat, src.size())
        return dst
    }

    /**
     * Масштабирование изображения до заданной ширины (высота подстраивается пропорционально).
     */
    fun resizeToWidth(src: Mat, targetWidth: Int): Mat {
        val aspectRatio = src.rows().toDouble() / src.cols()
        val newHeight = (targetWidth * aspectRatio).toInt()
        val dst = Mat()
        opencv_imgproc.resize(src, dst, Size(targetWidth.toDouble(), newHeight.toDouble()))
        return dst
    }

    /**
     * Корректировка яркости и контрастности: new = alpha * pixel + beta
     * alpha (контраст) > 1.0 увеличивает контраст, beta (яркость) смещает.
     */
    fun adjustBrightnessContrast(src: Mat, alpha: Double, beta: Int): Mat {
        val dst = Mat()
        src.convertTo(dst, -1, alpha, beta.toDouble())
        return dst
    }

    /**
     * Бинаризация изображения (адаптивный порог или метод Otsu).
     * Если adaptive = true, используется адаптивный порог, иначе Otsu.
     */
    fun binarize(src: Mat, adaptive: Boolean = true): Mat {
        val gray = Mat()
        if (src.channels() > 1) {
            opencv_imgproc.cvtColor(src, gray, opencv_imgproc.COLOR_BGR2GRAY)
        } else {
            src.copyTo(gray)
        }

        val dst = Mat()
        if (adaptive) {
            // Адаптивный порог (средний по окрестности)
            opencv_imgproc.adaptiveThreshold(gray, dst, 255.0, opencv_imgproc.ADAPTIVE_THRESH_MEAN_C,
                opencv_imgproc.THRESH_BINARY, 15, 10)
        } else {
            // Глобальный порог по методу Otsu
            opencv_imgproc.threshold(gray, dst, 0.0, 255.0, opencv_imgproc.THRESH_BINARY + opencv_imgproc.THRESH_OTSU)
        }
        return dst
    }

    /**
     * Полный цикл предобработки изображения.
     * @param inputPath путь к исходному изображению
     * @param outputPath путь для сохранения результата
     * @param deskew выполнять ли выравнивание
     * @param targetWidth целевая ширина (0 = не менять разрешение)
     * @param alpha контраст (1.0 = без изменений)
     * @param beta яркость (0 = без изменений)
     * @param binarizePerform выполнять ли бинаризацию
     * @param adaptiveThreshold использовать адаптивный порог при бинаризации
     */
    fun preprocess(
        inputPath: String,
        outputPath: String,
        deskew: Boolean = true,
        targetWidth: Int = 0,
        alpha: Double = 1.0,
        beta: Int = 0,
        binarizePerform: Boolean = true,
        adaptiveThreshold: Boolean = true
    ) {
        val src = opencv_imgcodecs.imread(inputPath)
        if (src.empty()) {
            println("Ошибка: не удалось загрузить изображение $inputPath")
            return
        }

        var processed = src.clone()

        // 1. Выравнивание
        if (deskew) {
            val angle = detectSkewAngle(processed)
            if (abs(angle) > 0.1) {
                println("Обнаружен угол наклона: $angle градусов, выполняется поворот")
                processed = rotateImage(processed, angle)
            } else {
                println("Угол наклона незначителен (${angle}°), поворот пропущен")
            }
        }

        // 2. Масштабирование
        if (targetWidth > 0) {
            processed = resizeToWidth(processed, targetWidth)
        }

        // 3. Коррекция яркости/контрастности
        if (alpha != 1.0 || beta != 0) {
            processed = adjustBrightnessContrast(processed, alpha, beta)
        }

        // 4. Бинаризация (если требуется)
        if (binarizePerform) {
            processed = binarize(processed, adaptiveThreshold)
        }

        // Сохранение результата
        if (opencv_imgcodecs.imwrite(outputPath, processed)) {
            println("Обработанное изображение сохранено в: $outputPath")
        } else {
            println("Ошибка сохранения в $outputPath")
        }

        // Освобождение памяти (опционально, GC справится)
        src.release()
        processed.release()
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty() || args[0] == "-h" || args[0] == "--help") {
        println("""
            Использование: java -jar program.jar [опции] входное_изображение выходное_изображение
            
            Опции:
              --no-deskew         Отключить выравнивание (по умолчанию включено)
              --width <пиксели>   Изменить ширину (пропорционально)
              --alpha <значение>  Контраст (по умолчанию 1.0, >1 увеличивает)
              --beta <значение>   Яркость (смещение, по умолчанию 0)
              --no-binarize       Отключить бинаризацию
              --adaptive          Использовать адаптивный порог при бинаризации (включен по умолчанию)
              --otsu              Использовать метод Otsu вместо адаптивного
              
            Примеры:
              # Базовое использование (выравнивание, бинаризация адаптивная)
              java -jar program.jar scan.jpg result.jpg
              
              # Только выравнивание и масштабирование до ширины 1000px, без бинаризации
              java -jar program.jar --width 1000 --no-binarize scan.jpg result.jpg
              
              # Увеличить контраст, использовать Otsu
              java -jar program.jar --alpha 1.5 --beta 10 --otsu scan.jpg result.jpg
        """.trimIndent())
        return
    }

    // Парсинг аргументов
    var inputPath: String? = null
    var outputPath: String? = null
    var deskew = true
    var targetWidth = 0
    var alpha = 1.0
    var beta = 0
    var binarizePerform = true
    var adaptiveThreshold = true

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--no-deskew" -> deskew = false
            "--width" -> {
                if (i + 1 < args.size) targetWidth = args[++i].toIntOrNull() ?: 0
                else println("Предупреждение: не указана ширина для --width")
            }
            "--alpha" -> {
                if (i + 1 < args.size) alpha = args[++i].toDoubleOrNull() ?: 1.0
                else println("Предупреждение: не указано значение alpha")
            }
            "--beta" -> {
                if (i + 1 < args.size) beta = args[++i].toIntOrNull() ?: 0
                else println("Предупреждение: не указано значение beta")
            }
            "--no-binarize" -> binarizePerform = false
            "--adaptive" -> adaptiveThreshold = true
            "--otsu" -> adaptiveThreshold = false
            else -> {
                if (inputPath == null) inputPath = args[i]
                else if (outputPath == null) outputPath = args[i]
                else println("Лишний аргумент: ${args[i]}")
            }
        }
        i++
    }

    if (inputPath == null || outputPath == null) {
        println("Ошибка: необходимо указать входной и выходной файлы.")
        return
    }

    if (!File(inputPath).exists()) {
        println("Ошибка: входной файл не существует: $inputPath")
        return
    }

    // Выполнение предобработки
    ImageTextPreprocessor.preprocess(
        inputPath = inputPath,
        outputPath = outputPath,
        deskew = deskew,
        targetWidth = targetWidth,
        alpha = alpha,
        beta = beta,
        binarizePerform = binarizePerform,
        adaptiveThreshold = adaptiveThreshold
    )
}