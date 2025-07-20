## 20/07/2025

- Created two separate stacks, "CantileverStack" for prod and "Cantilever-Dev-Stack" for dev
- Better naming of resources using 'stageName' variable, which is "cantilever-[dev|prod]"
- No longer using context env properties to distinguish between them - domains etc are specified in the stack
- Removed the CloudFrontSubstack for now as they aren't helping
- Deleted the live user pool (oops) so cannot log in just now. User pool now created through CDK. Development user pool is deletable and has no user defined.
- In web front end, added more ENV properties for the new Cognito user pools etc
- API gateway for dev should be at dev-api.cantilevers.org but this domain is not registered to Cloudfront...
- I've configured a Route53 A-record to point dev-api.cantilevers.org to the API gateway domain name
- But getting a 502 Bad Gateway error - it's an error with my authentication method as a call to /warm works. Cognito details not set in the lambda.
- Some issues remain, but the SQS queues and lambdas are processing a bunch of uploaded files and the localhost website is fetching from the new dev instance.
- Known issues:
  - Error on logout with Cognito
  - Web editor isn't always fetching everything
  - 404 errors when a route returns has no data (e.g. there are no templates at all; should probably return a 204 No Content)
