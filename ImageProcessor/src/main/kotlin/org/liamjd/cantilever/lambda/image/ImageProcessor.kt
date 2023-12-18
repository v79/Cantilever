package org.liamjd.cantilever.lambda.image

import com.amazonaws.services.lambda.runtime.LambdaLogger
import org.imgscalr.Scalr
import org.liamjd.cantilever.models.ImgRes
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class ImageProcessor(private val logger: LambdaLogger) {

    /**
     * Use the Scalr library to resize the image
     * @param res the ImgRes object containing the width and height to resize to
     * @param imageBytes the ByteArray containing the image to resize
     * @param formatName the "informal name" of the type of the image, which must be "jpg", "gif", or "png"
     * @return a ByteArray containing the resized image
     */
    fun resizeImage(res: ImgRes, imageBytes: ByteArray, formatName: String?): ByteArray {
        val srcImage = ImageIO.read(imageBytes.inputStream())
        val newWidth = res.w ?: srcImage.width
        val newHeight = res.h ?: srcImage.height
        logger.info("ImageProcessor: resize $formatName image from ${srcImage.width} x ${srcImage.height} to $newWidth x $newHeight")

        val scaledImage = Scalr.resize(srcImage, newWidth, newHeight) // Scale image
        val baos = ByteArrayOutputStream(imageBytes.size)
        ImageIO.write(scaledImage, formatName ?: "jpg", baos)
        return baos.toByteArray()
    }
}