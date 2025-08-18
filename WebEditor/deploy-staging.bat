REM copy build files to destination bucket
ECHO Uploading to S3...
aws s3 cp .\build s3://cantilever-dev-cantileverdeveditorb5f60ea1-kaxexysiy7p2/ --recursive
REM aws cloudfront create-invalidation --distribution-id E2XF3PLP5DQPWO --paths "/*"
ECHO Done.
