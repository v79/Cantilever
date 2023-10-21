package org.liamjd.cantilever.services.impl

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

    override fun putObject(key: String, bucket: String, contents: String, contentType: String?): Int {
        val bytes = contents.toByteArray(Charsets.UTF_8)
        println("S3Service: putObject: key: $key, ${bytes.size} bytes")

        val requestBuilder = PutObjectRequest.builder()
            .contentLength(bytes.size.toLong())
            .key(key)
            .bucket(bucket)
        contentType?.let {
            requestBuilder.contentType(it)
        }
        val request = requestBuilder.build()
        s3Client.putObject(request, RequestBody.fromBytes(bytes))
        return bytes.size
    }

    override fun getObject(key: String, bucket: String): GetObjectResponse? {
        val request = GetObjectRequest.builder()
            .key(key)
            .bucket(bucket)
            .build()
        return s3Client.getObject(request).response()
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
        val request =  s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).delimiter("/").build())
        if(request.hasCommonPrefixes()) {
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
}