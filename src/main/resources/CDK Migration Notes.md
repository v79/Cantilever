## 20/07/2025

- [-] Better naming of resources using 'stageName' variable, which is "cantilever-[dev|prod]"
- [x] No longer using context env properties to distinguish between them - domains etc are specified in the stack
- [x] Removed the CloudFrontSubstack for now as they aren't helping
- [x] Deleted the live user pool (oops) so cannot log in just now. User pool now created through CDK. Development user pool is deletable and has no user defined.
- [x] In web front end, added more ENV properties for the new Cognito user pools etc
- [x] API gateway for dev should be at dev-api.cantilevers.org but this domain is not registered to Cloudfront...
- [x] I've configured a Route53 A-record to point dev-api.cantilevers.org to the API gateway domain name
- [x] But getting a 502 Bad Gateway error - it's an error with my authentication method as a call to /warm works. Cognito
- details not set in the lambda.
- [-] Some issues remain, but the SQS queues and lambdas are processing a bunch of uploaded files and the localhost
website is fetching from the new dev instance.

### Known issues:

- [-]Error on logout with Cognito
- [x] Created two separate stacks, "CantileverStack" for prod and "Cantilever-Dev-Stack" for dev
- [x] The dev bucket isn't public and doesn't have a cloudfront associated URL, so cannot run the editor environment in the cloud yet. Using a CDK level 2 construct to create the distribution. Would be nice if I could capture the domain name for this. I am hard-coding this for now, scripting it looks quite complicated.
- [x] Web editor isn't always fetching everything
- [-] 404 errors when a route returns has no data (e.g. there are no templates at all; should probably return a 204 No
Content)
- [-] API Gateway stages (not cdk stages!) Access-Control-Allow-Origin header for OPTIONS should be set to the cloudfront distribution domain? But also localhost? May need an API stage called "local"? CORS is a bugger.