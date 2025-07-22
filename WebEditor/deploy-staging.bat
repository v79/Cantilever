REM copy build files to destination bucket
ECHO Uploading to S3...
aws s3 cp .\build s3://cantilever-dev-cantileverdeveditor090a12b8-xmsobjbji6ku/ --recursive
REM aws cloudfront create-invalidation --distribution-id E12W61TSPOUVVI --paths "/*"
ECHO Done.
