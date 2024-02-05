package org.liamjd.cantilever.services.impl

import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import org.liamjd.cantilever.services.S3Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*

class S3ServiceImpl(region: Region) : S3Service {

    override val s3Client: S3Client = S3Client.builder().region(region).build()

    override fun getObjectAsString(key: String, bucket: String): String {
        val request = GetObjectRequest.builder()
            .key(key)
            .bucket(bucket)
            .build()
        val bytes = s3Client.getObjectAsBytes(request).asByteArray()
        return String(bytes)
    }

    override fun putObjectAsString(key: String, bucket: String, contents: String, contentType: String?): Int {
        val bytes = contents.toByteArray(Charsets.UTF_8)
        println("S3Service: putObject: key: $key, ${bytes.size} bytes")

        val requestBuilder = byteArrayBuilder(bytes, key, bucket, contentType)
        val request = requestBuilder.build()
        s3Client.putObject(request, RequestBody.fromBytes(bytes))
        return bytes.size
    }

    override fun putObjectAsBytes(key: String, bucket: String, contents: ByteArray, contentType: String?): Int {
        println("S3Service: putObjectAsBytes: key: $key, ${contents.size} bytes")
        val requestBuilder = byteArrayBuilder(contents, key, bucket, contentType)
        val request = requestBuilder.build()
        s3Client.putObject(request, RequestBody.fromBytes(contents))
        return contents.size
    }

    private fun byteArrayBuilder(
        contents: ByteArray,
        key: String,
        bucket: String,
        contentType: String?
    ): PutObjectRequest.Builder {
        val requestBuilder = PutObjectRequest.builder()
            .contentLength(contents.size.toLong())
            .key(key)
            .bucket(bucket)
        contentType?.let {
            requestBuilder.contentType(it)
        }
        return requestBuilder
    }

    override fun getObject(key: String, bucket: String): GetObjectResponse? {
        val request = GetObjectRequest.builder()
            .key(key)
            .bucket(bucket)
            .build()
        return s3Client.getObject(request).response()
    }

    override fun getObjectAsBytes(key: String, bucket: String): ByteArray {
        val request = GetObjectRequest.builder()
            .key(key)
            .bucket(bucket)
            .build()
        return s3Client.getObjectAsBytes(request).asByteArray()
    }

    override fun objectExists(key: String, bucket: String): Boolean {
        var exists = true
        try {
            s3Client
                .headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build())
        } catch (e: NoSuchKeyException) {
            exists = false
        } catch (se: S3Exception) {
            exists = false
        }
        return exists
    }

    override fun listObjects(prefix: String, bucket: String): ListObjectsV2Response {
        return s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build())
    }

    override fun listFolders(prefix: String, bucket: String): List<String> {
        val request =
            s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).delimiter("/").build())
        if (request.hasCommonPrefixes()) {
            return request.commonPrefixes().map { it.prefix() }.toList()
        }
        return emptyList()
    }

    override fun deleteObject(key: String, bucket: String): DeleteObjectResponse? {
        val request = DeleteObjectRequest.builder()
            .key(key)
            .bucket(bucket)
            .build()
        return s3Client.deleteObject(request)
    }

    override fun createFolder(key: String, bucket: String): Int {
        val requestBuilder = PutObjectRequest.builder()
            .contentLength(0L)
            .key("$key/")
            .bucket(bucket)
            .build()
        s3Client.putObject(requestBuilder, RequestBody.empty())
        return 0
    }

    override fun getUpdatedTime(key: String, bucket: String): Instant {
        val request = HeadObjectRequest.builder()
            .key(key)
            .bucket(bucket)
            .build()
        val response = s3Client.headObject(request)
        return response.lastModified().toKotlinInstant()
    }

    override fun getMetadata(key: String, bucket: String, metadataKey: String): String? {
        val request = HeadObjectRequest.builder()
            .key(key)
            .bucket(bucket)
            .build()
        val response = s3Client.headObject(request)
        if (response.hasMetadata()) {
            val metadata = response.metadata()
            if (metadata.containsKey(metadataKey)) {
                return metadata[metadataKey]
            }
        }
        return null
    }

    override fun getContentType(key: String, bucket: String): String? {
        val request = HeadObjectRequest.builder()
            .key(key)
            .bucket(bucket)
            .build()
        val response = s3Client.headObject(request)
        return response.contentType()
    }

    override fun copyObject(srcKey:String, destKey: String, bucket: String): Int {
        val request = CopyObjectRequest.builder()
            .sourceBucket(bucket)
            .sourceKey(srcKey)
            .destinationKey(destKey)
            .destinationBucket(bucket)
            .build()
        val response = s3Client.copyObject(request)
        return if (response.copyObjectResult() != null)
            0
        else -1
    }

    override fun copyObject(srcKey: String, destKey: String, srcBucket: String, destBucket: String): Int {
        val request = CopyObjectRequest.builder()
            .sourceBucket(srcBucket)
            .sourceKey(srcKey)
            .destinationKey(destKey)
            .destinationBucket(destBucket)
            .build()
        val response = s3Client.copyObject(request)
        return if (response.copyObjectResult() != null)
            0
        else -1
    }
}