import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.random.Random


fun main() {
    val movie1FrameTimes =
        randomFrames(Path.of("Your"), 100, 1)
    val movie2FrameTimes = randomFrames(Path.of("videos"), 100, 2)
    val movie3FrameTimes = randomFrames(Path.of("here"), 100, 3)

    (0..100).forEachIndexed { index, _ ->
        createBingoCard(index)
    }

    val allFrames = movie1FrameTimes.entries + movie2FrameTimes.entries + movie3FrameTimes.entries
    val allFramesSorted = allFrames.sortedBy { it.key }

    allFramesSorted.joinToString("\n") { entry ->
            val movieNumber = when {
                movie1FrameTimes.containsKey(entry.key) -> 1
                movie2FrameTimes.containsKey(entry.key) -> 2
                movie3FrameTimes.containsKey(entry.key) -> 3
                else -> throw IllegalStateException("Frame not found in any movie")
            }
           "${entry.key} - $movieNumber ${timeStampToReadable(entry.value)}"
    }.let { File("all-frames.txt").writeText(it) }
}

private fun timeStampToReadable(timeStamp: Long): String {
    val totalSeconds = timeStamp / 1_000_000  // converting microseconds to seconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

private fun createBingoCard(cardNumber: Int) {
    val allFrames = File("frames").listFiles()!!.toList()

    val bingoCardFrames = (0..8)  // Change 9 to 8 as indices go from 0 to 8 for a total of 9 items
        .map { allFrames[Random.nextInt(0, allFrames.size)] }

    if (bingoCardFrames.distinct().size != 9) {
        return createBingoCard(cardNumber)
    }

    val tileSize = 300  // Set a fixed tile size, for example, 300x300 pixels
    val gridImage = BufferedImage(tileSize * 3, tileSize * 3, BufferedImage.TYPE_INT_ARGB)
    val g2d = gridImage.createGraphics()

    g2d.color = Color.WHITE  // Set the color to white
    g2d.fillRect(0, 0, gridImage.width, gridImage.height)  // Fill the entire image with white color

    for (row in 0 until 3) {
        for (col in 0 until 3) {
            val index = row * 3 + col
            val img = ImageIO.read(bingoCardFrames[index])
            g2d.drawImage(img, col * tileSize, row * tileSize, tileSize, tileSize, null)
        }
    }

    g2d.dispose()

    File("cards").mkdir()
    val outputFilePath = "cards/bingo-card-$cardNumber.png"
    ImageIO.write(gridImage, "png", File(outputFilePath))
    println("Bingo card saved as $outputFilePath")
}


fun randomFrames(videoPath: Path, numFrames: Int, movieNumber: Int): Map<String, Long> {
    val frameTimes = mutableMapOf<String, Long>()
    val grabber = FFmpegFrameGrabber(videoPath.toAbsolutePath().toString())
    grabber.start()

    val videoLengthInFrames = grabber.lengthInFrames

    for (i in 1..numFrames) {
        val frameNumber = Random.nextInt(videoLengthInFrames)
        grabber.frameNumber = frameNumber

        val frame = grabber.grabImage()
        if (frame != null) {
            val converter = Java2DFrameConverter()

            val framesDir = File("frames")
            framesDir.delete()
            framesDir.mkdir()

            val originalImage: BufferedImage = converter.convert(frame)
            val text = "${originalImage.hashCode()}"
            val font = Font("Arial", Font.PLAIN, 50)

            val textHeight = 70  // Adjust the height to fit the text
            val newImage = BufferedImage(
                originalImage.width,
                originalImage.height + textHeight,
                BufferedImage.TYPE_INT_ARGB
            )

            val g2d = newImage.createGraphics()
            g2d.drawImage(originalImage, 0, 0, null)
            g2d.color = Color.BLACK
            g2d.font = font
            g2d.drawString(text, 10, originalImage.height + textHeight - 5)  // Adjust the position to fit the text
            g2d.dispose()

            val outputFilePath = "frames/movie-random-frame-${originalImage.hashCode()}.png"

            frameTimes[outputFilePath] = frame.timestamp

            ImageIO.write(newImage, "png", File(outputFilePath))
            println("Frame $i saved as $outputFilePath")
        }
    }

    grabber.stop()

    return frameTimes
}