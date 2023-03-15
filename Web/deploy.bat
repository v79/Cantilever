REM copy build files to destination bucket
ECHO Uploading to S3...
aws s3 cp .\build s3://cantileverstack-cantilevereditor7fe8443c-2187oqpgdvmj/ --recursive
ECHO Done.
