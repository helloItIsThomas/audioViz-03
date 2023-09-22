import ddf.minim.Minim
import ddf.minim.analysis.FFT
import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.draw.tint
import org.openrndr.math.IntVector2
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
    configure {
        width = 608
        height = 342
        hideWindowDecorations = true
        windowAlwaysOnTop = true
        position = IntVector2(1285,110)
        windowTransparent = true
        multisample = WindowMultisample.SampleCount(4)
    }

    program {
        val image = loadImage("data/images/pm5544.png")
        val font = loadFont("data/fonts/default.otf", 64.0)

        val minim = Minim(object : Object() {
            fun sketchPath(fileName: String): String {
                return fileName
            }
            fun createInput(fileName: String): InputStream {
                return FileInputStream(File(fileName))
            }
        })
        val lineIn = minim.lineIn

        val fft = FFT(lineIn.bufferSize(), lineIn.sampleRate())

        extend {
            drawer.clear(ColorRGBa.TRANSPARENT)

            fft.forward(lineIn.mix)

            drawer.stroke = ColorRGBa.GREEN
            drawer.strokeWeight = 1.0
            val scaleFactor = 80.0  // Increase to "zoom in" on a frequency range
            val interpFactor = 3  // Number of interpolated lines between each real band

            for (i in 0 until fft.specSize()) {
                val realBandHeight = fft.getBand(i) * 4

                // Draw the real band
                drawer.lineSegment(
                    i * scaleFactor, height.toDouble(),
                    i * scaleFactor, (height - realBandHeight).toDouble()
                )

                // Interpolate between this band and the next band
                if (i < fft.specSize() - 1) {
                    val nextRealBandHeight = fft.getBand(i + 1) * 4
                    for (j in 1 until interpFactor + 1) {
                        val interpHeight =
                            realBandHeight + (nextRealBandHeight - realBandHeight) * (j / (interpFactor + 1.0))
                        drawer.lineSegment(
                            (i * scaleFactor) + (scaleFactor / (interpFactor + 1) * j), height.toDouble(),
                            (i * scaleFactor) + (scaleFactor / (interpFactor + 1) * j), height - interpHeight
                        )
                    }
                }
            }
        }
    }
}
