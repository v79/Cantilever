package org.liamjd.cantilever.lambda.image

import org.imgscalr.Scalr
import org.liamjd.cantilever.models.ImgRes
import org.liamjd.cantilever.models.SrcKey
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class ImageProcessor {
    /**
     * Call the standard Java 7 BufferedImage scale function to resize the image
     */
    fun resizeImage(res: ImgRes, imageBytes: ByteArray, srcKey: SrcKey): ByteArray {
        val srcImage = ImageIO.read(imageBytes.inputStream())
        println("ImageProcessor: resizeImage from ${srcImage.width} x ${srcImage.height} to ${res.w} x ${res.h}")
        val newWidth = res.w ?: srcImage.width
        val newHeight = res.h ?: srcImage.height

        val scaledImage = Scalr.resize(srcImage, newWidth, newHeight) // Scale image
        val baos = ByteArrayOutputStream(imageBytes.size)
        ImageIO.write(scaledImage, "jpg", baos)
        return baos.toByteArray()
    }
}