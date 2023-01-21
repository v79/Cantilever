package org.liamjd.cantilever.services

import software.amazon.awssdk.services.s3.S3Client

/**
 * Wrapper around AWS S3 to simplify interactions with S3
 */
interface S3Service {

     val s3Client: S3Client

    /**
     * Get an S3 Object, read its bytestream, and return it as a String
     * @param key the S3 object key
     * @param bucket the S3 bucket name
     * @return the contents of the file as a String, or an empty String if failed
     */
    fun getObjectAsString(key: String, bucket: String): String

    /**
     * Write a string to an S3 bucket
     * @param key the S3 object key to write
     * @param bucket the s3 bucket name
     * @param string the string of characters to write to the object
     * @param contentType the mime type of the file; optional
     */
    fun putObject(key: String, bucket: String, string: String, contentType: String?)

    /**
     * Check to see if the object with the given key exists
     * @param key the object to look for
     * @param bucket the s3 bucket name
     * @return true if the object exists in the bucket, false otherwise
     */
    fun objectExists(key: String, bucket: String): Boolean

}