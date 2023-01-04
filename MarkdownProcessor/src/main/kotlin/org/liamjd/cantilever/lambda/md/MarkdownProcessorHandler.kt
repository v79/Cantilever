package org.liamjd.cantilever.lambda.md

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent

/**
 * Respond to the SQSEvent which will contain the S3 key of the markdown file to parse
 * Read the content of the file and pass it to the `convertMDToHTML` function
 * Then write the resultant file to the destination bucket
 */
class MarkdownProcessorHandler : RequestHandler<SQSEvent, String> {

    override fun handleRequest(event: SQSEvent, context: Context): String {
        val sourceBucket = System.getenv("source_bucket")
        val destinationBucket = System.getenv("destination_bucket")
        val logger = context.logger
        var response = "200 OK"

//        try {
        val eventRecord = event.records[0]
        logger.log("MarkdownProcessorHandler RECORD.BODY=${eventRecord.body}")

        val html = convertMDToHTML(log = logger, mdSource = "**This came from** the _MarkdownProcessorHandler_. It does not yet actually read any source files.")
        logger.log("MarkdownProcessorHandler HTML OUTPUT=$html")
//        }


        return response
    }
}