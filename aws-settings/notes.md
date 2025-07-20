This is the configuration for the api.cantilevers.org cloudfront distribution.

alternative domain name:    app.cantilevers.org
custom ssl cert:            *.cantilevers.org    arn:aws:acm:us-east-1:086949310404:certificate/9b8f27c6-87be-4c14-a368-e6ad3ac4fb68
origin domain:              cantileverstack-cantilevereditor7fe8443c-2187oqpgdvmj.s3-website.eu-west-2.amazonaws.com
behaviour:                  <origin domain> -> HTTPS, GET/HEAD only, CachingOptimised
