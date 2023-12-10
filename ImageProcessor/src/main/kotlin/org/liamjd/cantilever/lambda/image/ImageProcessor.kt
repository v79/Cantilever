package org.liamjd.cantilever.lambda.image

import org.liamjd.cantilever.models.ImgRes
import org.liamjd.cantilever.models.SrcKey
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class ImageProcessor {
    /**
     * Call the standard Java 7 BufferedImage scale function to resize the image
     */
    fun resizeImage(res: ImgRes, imageBytes: ByteArray, srcKey: SrcKey): ByteArray {
        val original = ImageIO.read(imageBytes.inputStream())
        println("ImageProcessor: resizeImage from ${original.width} x ${original.height} to ${res.w} x ${res.h}")
        val newWidth = res.w ?: original.width
        val newHeight = res.h ?: original.height

        val resized = BufferedImage(newWidth, newHeight, original.type)
        val g = resized.createGraphics()
        g.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR
        )
        g.drawImage(
            original, 0, 0, newWidth, newHeight, 0, 0, original.width,
            original.height, null
        )

        val outStream = ByteArrayOutputStream()
        ImageIO.write(resized, original.type.toString(), outStream)
        outStream.flush()
        g.dispose()
        return outStream.toByteArray()
    }
}