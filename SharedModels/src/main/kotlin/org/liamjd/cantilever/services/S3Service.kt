package org.liamjd.cantilever.services

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response

/**
 * Wrapper around AWS S3 to simplify interactions with S3
 */
interface S3Service {

    val s3Client: S3Client

    /**
     * Get an s3 Object and return it directly
     * @param key the S3 object key
     * @param bucket the S3 bucket name
     * @return the object response
     */
    fun getObject(key: String, bucket: String): GetObjectResponse?

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
     * @param contents the string of characters to write to the object
     * @param contentType the mime type of the file; optional
     */
    fun putObject(key: String, bucket: String, contents: String, contentType: String?): Int

    /**
     * Check to see if the object with the given key exists
     * @param key the object to look for
     * @param bucket the s3 bucket name
     * @return true if the object exists in the bucket, false otherwise
     */
    fun objectExists(key: String, bucket: String): Boolean

    /**
     * List all the objects with the given key prefix
     * @param prefix the common prefix to search for
     * @param bucket the s3 bucket name
     * @return a [ListObjectsV2Response] object which can be iterated over to get individual items
     */
    fun listObjects(prefix: String, bucket: String): ListObjectsV2Response

    /**
     * Delete the given object from S3
     * @param key the object to delete
     * @param bucket the s3 bucket name
     * @return the [DeleteObjectResponse], or null if there has been an exception
     */
    fun deleteObject(key: String, bucket: String): DeleteObjectResponse?
}