# Cantilever

An AWS Lambda driven static site generator written in Kotlin. Source `markdown` files will be converted to HTML 
using the [Flexmark-java](https://github.com/vsch/flexmark-java) library, and further processed using [handlebars.
java](https://github.com/jknack/handlebars.java), in AWS Lambda functions triggered by PUT events on a source S3 
bucket.

The resultant HTML will be written to a separate S3 bucket, configured as a static website.

I haven't quite worked out how to handle site navigation / internal links, but my current thinking is this will be 
fully browser-rendered in javascript, based on a generated site map file (or database?).

Longer term, I'd like to build several interfaces to creating and editing the markdown files, including a website 
and an Android phone app.

## Technology

I will be using AWS Cloudformation and the CDK to build this project. Although this ties me to AWS as a Cloud 
provider, I'm largely OK with that limitation.

I am writing primarily in Kotlin as it is my language of choice. I know that JVM-based Lambda functions are not the 
best performing, especially with start-up times, but I am not sure that performance is a priority for me. Perhaps 
with GraalVM is can be improved. I considered using a nodeJS/Javascript markdown-to-html Lambda function, but I am 
not a Javascript developer and have very little experience of the nodeJS ecosystem.

### Project structure

The root project will use AWS CDK to build the AWS Cloudformation templates, to set up S3 buckets, Lambda functions, 
API gateway, and so on. I have a little experience in using CDK for this.

Sub-modules in the project will provide the Lambda functions for HTML generation, templating, and other actions.

## Motivation

This could be considered a successor to my [bascule](https://github.com/v79/bascule-static) static site generator, a command-line driven application which 
got weighed down by poor program design and my own lack of experience. I have been keen to rebuild _bascule_ as 
web-first application for several years, frustrated by my inability to make website updates away from my PC.