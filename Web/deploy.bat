REM copy build files to destination bucket
ECHO Uploading to S3...
aws s3 cp .\build s3://cantileverstack-cantileverwebsitea46551a2-jfq7mzrgggj9/app/ --recursive
ECHO Done.
