## Scratchpad for notes

## 04/01/2023

I have been planning on using the [flexmark-java](https://github.com/vsch/flexmark-java) library for my markdown-to-HTML conversion, but I am concerned 
for the future of the project. The lead developer [has not been active on github](https://github.
com/vsch/flexmark-java/issues/541) for a year now, which means pull requests are not being merged and the project 
has not moved at all. Alternatives include [commonmark-java](https://github.com/commonmark/commonmark-java) (last 
updated November '22) and [Jetbrains's markdown](https://github.com/JetBrains/markdown) (a kotlin project, last 
updated December '22 but not nearly as feature complete).

I'll stick to **flexmark-java** for now.

I've been wondering about the structure/architecture of my AWS lambdas. I'd like to avoid the monolith that 
_bascule_ became, so I am wondering about splitting the code into lots of smaller lambda functions. I don't know if 
that's testable, or sensible - more lambdas means more cold-start waiting time, and it might me more complicated 
testing. But it could make the project more modular, and more microservice based.

So my first question is - should the lambda which responds to file uploads be responsible for markdown conversion? I 
am going to say 'no' and trial some ideas. File uploads could be other files besides markdown (images, etc), so 
perhaps the upload handler just handles the uploads, and passes it to another lambda for processing.

But how to pass execution to another lambda? Perhaps a message queue like AWS SQS or SNS?

## 03/01/2023

Created a basic project structure with a CDK application stack consisting of:

- 2 S3 buckets (source and destination)
- A test lambda function ("Markdown Processor") which responds to S3 PUT or PUSH events to the source bucket
- - it reads the contents of the file, and, for now, pretends to transform it into an HTML file
- - that output is then written to destination bucket

I briefly explored the Kotlin AWS SDK, but it's very coroutine heavy, which doesn't seem necessary in an 
event-driven Lambda world. I'm making the first git commit at this stage.