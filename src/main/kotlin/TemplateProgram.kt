import ddf.minim.Minim
import ddf.minim.analysis.FFT
import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.draw.tint
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
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

    oliveProgram {

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
        var counter = 0
        var n = 2 // update every 5th sample
        var currentRadius = 0.0
        var nextRadius = 0.0
        var minRadius = Double.MAX_VALUE
        var maxRadius = Double.MIN_VALUE

        extend {
            drawer.clear(ColorRGBa.BLACK)

            fft.forward(lineIn.mix)

            drawer.stroke = ColorRGBa.PINK
            drawer.strokeWeight = 1.0
            val scaleFactor = 10.0  // Increase to "zoom in" on a frequency range
            val interpFactor = 1  // Number of interpolated l nes between each real band

            for (i in 0 until fft.specSize()) {
                val realBandHeight = fft.getBand(i)

                // Draw the real band
                drawer.lineSegment(
                    i * scaleFactor, height.toDouble(),
                    i * scaleFactor, (height - realBandHeight).toDouble()
                )

                // Interpolate between this band and the next band
//                if (i < fft.specSize() - 1) {
//                    val nextRealBandHeight = fft.getBand(i + 1) * 4
//                    for (j in 1 until interpFactor + 1) {
//                        val interpHeight =
//                            realBandHeight + (nextRealBandHeight - realBandHeight) * (j / (interpFactor + 1.0))
//                        drawer.lineSegment(
//                            (i * scaleFactor) + (scaleFactor / (interpFactor + 1) * j), height.toDouble(),
//                            (i * scaleFactor) + (scaleFactor / (interpFactor + 1) * j), height - interpHeight
//                        )
//                    }
//                }
            }


            if (counter % n == 0) {
                val lowerBound = (40 * fft.timeSize() / lineIn.sampleRate()).toInt()
                val upperBound = (60 * fft.timeSize() / lineIn.sampleRate()).toInt()
                nextRadius = (lowerBound..upperBound).map { fft.getBand(it) }.average()

                // Update min and max for normalization
                minRadius = min(minRadius, nextRadius)
                maxRadius = max(maxRadius, nextRadius)

                val alpha = 0.3  // Smoothing factor between 0 and 1
                currentRadius = alpha * nextRadius + (1 - alpha) * currentRadius  // EMA smoothing

                // Normalize
                if (maxRadius > minRadius) {
                    currentRadius = (currentRadius - minRadius) / (maxRadius - minRadius)
                }
            }
            counter++

            println(currentRadius)

            drawer.stroke = null
            drawer.fill = ColorRGBa.PINK
            drawer.circle(drawer.bounds.center, currentRadius * 300.0)


//            val lowerBound = (40 * fft.timeSize() / lineIn.sampleRate()).toInt()
//            val upperBound = (60 * fft.timeSize() / lineIn.sampleRate()).toInt()
//            val kickAvg = (lowerBound..upperBound).map { fft.getBand(it) }.average()
//            drawer.circle(drawer.bounds.center * Vector2(0.2, 1.0), kickAvg * 1.0)


//            drawer.circle(drawer.bounds.center * Vector2(0.5, 1.0), fft.getBand((fft.specSize()* 0.5).toInt()).toDouble() * 20.0)
//            drawer.circle(drawer.bounds.center * Vector2(0.8, 1.0), fft.getBand((fft.specSize()* 0.8).toInt()).toDouble() * 20.0)
        }
    }
}
