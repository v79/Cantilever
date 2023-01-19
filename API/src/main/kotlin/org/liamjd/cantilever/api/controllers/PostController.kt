package org.liamjd.cantilever.api.controllers

import io.moia.router.Request
import io.moia.router.ResponseEntity
import org.liamjd.cantilever.api.MyRequest
import org.liamjd.cantilever.api.MyResponse

class PostController {

    fun newPost(request: Request<MyRequest>): ResponseEntity<MyResponse> {
        println("controller POST /new $request")
        return ResponseEntity.created(body = MyResponse("Created " + request.body.message))
    }

}