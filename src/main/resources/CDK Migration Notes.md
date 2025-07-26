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
- [-] API Gateway stages (not cdk stages!) Access-Control-Allow-Origin header for OPTIONS should be set to the cloudfront distribution domain? But also localhost? May need an API stage called "local"? CORS is a bugger. Gemini has suggested a localhost URL rewriting proxy on vite dev that would require all my API calls to have a prefix (/api/project/list for instance). This seems to work but will need careful configuration.

## 22/07/2025

- [X] Logout error seems to have been a CORS issue
- [-] There are some bugs in the API code, e.g. around the location of metadata.json
- [-] Discovered that I used to have a 'name' claim available in Cognito, but not existing any more. Only used in logs though?
- [-] Working on creating enough content in the system to verify the editor works

## 23/07/2025

- [X] Fixes around the location of metadata.json - I was so inconsistent! But as this will be replaced by a database, I'm not going to get too worked up about it
- [-] Can create and save templates and pages, but the New Post function doesn't work here either! Fix this then I will be done...
- [X] I think I have fixed new post by splitting the function into several steps

## 25/07/2025

Lots of research on dynamodb partition keys. Many discussions with LLMs. Trying to understand what is "good" high cardinality and "poor" high cardinality. For my files in S3, it's been suggested that "<domain>#<path>" is a good partition key, with "<type>#<leaf>" as a good sort key. Or "<domain>" and "<type>#<path".
If I go with "<domain>#<type>" & "<path>" then I get reasonable cardinality, partitioned by project and file type, and the sort key allows me pick the specific file still. It supports queries such as "get all posts for this domain".
Create a GSI as "<domain>" & "<path>" allows for simpler queries?
Only thing missing is sorting by "lastUpdated".
