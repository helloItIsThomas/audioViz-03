
import classes.CButton
import classes.CSlider
import ddf.minim.Minim
import ddf.minim.analysis.FFT
import demos.classes.Animation
import kotlinx.coroutines.DelicateCoroutinesApi
import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.draw.font.loadFace
import org.openrndr.extra.envelopes.ADSRTracker
import org.openrndr.extra.noise.random
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.shapes.grid
import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.math.IntVector2
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.scale
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.findShapes
import org.openrndr.svg.loadSVG
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min


@OptIn(DelicateCoroutinesApi::class)
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
// MOUSE STUFF //////
        var mouseClick = false
        var mouseState = "up"
        mouse.dragged.listen { mouseState = "drag" }
        mouse.exited.listen { mouseState = "up" }
        mouse.buttonUp.listen { mouseState = "up"; mouseClick = true }
        mouse.buttonDown.listen { mouseState = "down" }
        mouse.moved.listen { mouseState = "move" }
// END //////////////
        val columnCount = 3
        val rowCount = 3
        val marginX = 10.0
        val marginY = 10.0
        val gutterX = 3.0
        val gutterY = 3.0
        var grid = drawer.bounds.grid(columnCount, rowCount, marginX, marginY, gutterX, gutterY)
        val flatGrid = grid.flatten()

        val incremCheck = onceObj()
        var palette = listOf(ColorRGBa.fromHex(0xF1934B), ColorRGBa.fromHex(0x0E8847), ColorRGBa.fromHex(0xD73E1C), ColorRGBa.fromHex(0xF4ECDF), ColorRGBa.fromHex(0x552F20))
        val white = ColorRGBa.WHITE
        val black = ColorRGBa.BLACK
        val animation = Animation()
        val loopDelay = 3.0
        val message = "hello"
        animation.loadFromJson(File("data/keyframes/keyframes-0.json"))
        val svgA = loadSVG(File("data/fonts/a.svg"))
        val firstShape = svgA.root.findShapes()[0]
        val firstContour = firstShape.shape.contours[0]

        val image = loadImage("data/images/cheeta.jpg")
        val scale: DoubleArray = typeScale(3, 100.0, 3)
        val typeFace: Pair<List<FontMap>, List<FontImageMap>> = defaultTypeSetup(scale, listOf("reg", "reg", "bold"))
        val animArr = mutableListOf<Animation>()
        val randNums = mutableListOf<Double>()
        val charArr = message.toCharArray()
        charArr.forEach { e ->
            animArr.add(Animation())
            randNums.add(random(0.0, 1.0))
        }
        animArr.forEach { a ->
            a.loadFromJson(File("data/keyframes/keyframes-0.json"))
        }
        val globalSpeed = 0.01



        // AUDIO STUFF

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
        var minAverage = Double.MAX_VALUE
        var maxAverage = Double.MIN_VALUE
        var normalizedAverage = 0.0

        val tracker = ADSRTracker(this)
        tracker.attack = 0.00
        tracker.decay = 0.55
        tracker.sustain = 0.0
        tracker.release = 0.4
        var isTriggerOn = false
        val kickThresh = 0.75


        // // // // //



        extend {
            animArr.forEachIndexed { i, a ->
//                a((randNums[i] * 0.3 + frameCount * globalSpeed) % loopDelay)
                a((randNums[i] * 0.3 + tracker.value()) % loopDelay)
            }
            drawer.clear(ColorRGBa.TRANSPARENT)
            drawer.circle(drawer.bounds.center, 10.0)
            drawer.stroke = white

            drawer.clear(ColorRGBa.BLACK)

            fft.forward(lineIn.mix)

            drawer.strokeWeight = 1.0
            drawer.stroke = null //ColorRGBa.PINK
            val scaleFactor = 800.0  // Increase to "zoom in" on a frequency range
            val interpFactor = 1  // Number of interpolated l nes between each real band

            var sum = 0.0
            var count = 0

            for (i in 0 until fft.specSize()) {
                val realBandHeight = fft.getBand(i) * 4

                if (i < fft.specSize() * 0.01) {
                    sum += realBandHeight
                    count++
//                    drawer.lineSegment(
//                        x0 = i * scaleFactor, y0 = height.toDouble(),
//                        i * scaleFactor, (height - realBandHeight).toDouble()
//                    )
                }
            }

            val average = if (count > 0) sum / count else 0.0

            // Update min and max for normalization
            minAverage = min(minAverage, average)
            maxAverage = max(maxAverage, average)

            // Normalize
            if (maxAverage > minAverage) {
                normalizedAverage = (average - minAverage) / (maxAverage - minAverage)
            }


//            println( normalizedAverage )
//            if(normalizedAverage > 0.6){
//                drawer.circle(drawer.bounds.center, 100.0)
//            }
            if (normalizedAverage > kickThresh && !isTriggerOn) {
                tracker.triggerOn()
                isTriggerOn = true
            } else if (normalizedAverage <= kickThresh && isTriggerOn) {
                tracker.triggerOff()
                isTriggerOn = false
            }

            println(sum)


            flatGrid.forEach{ r ->
                drawer.circle(
                    (drawer.bounds.center * 0.5 * Vector2(animArr[0].pathSlider, 1.0) + Vector2(r.x, 0.0)),
                    animArr[0].pathSlider * 60.0
                )// + (sum * 0.005))
//            drawer.pushTransforms()

//            drawer.translate(
//                drawer.bounds.center * Vector2(tracker.value(), 1.0)
//            )
            }
//            drawer.popTransforms()




            // THIS NEEDS TO STAY AT THE END //
            if (mouseClick) mouseClick = false
            // END END ////////////////////////
        }
    }
}