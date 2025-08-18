# ContentTree usage

## Via APIController

- loadContentTree()
  - GeneratorController::clearGeneratedImages() - _DynamoDB isn't tracking image usage yet_

## Via MetadataController

- rebuildFromSources()
  - loads and saves metadata.json   - this should be my next target to fix

## Via ImageProcessorHandler

- processImageResize()
  - loads domain.yaml
  - loads & saves metadata.json
- processImageCopy()
  - loads domain.yaml
- calculateFilename
  - _None_

## Via MarkdownProcessorHandler

- handleRequest()
  - _None!_
- processPageUpload()
  - _None!_
- processPostUpload()
  - _None!_
- copyImages()
  - _None!_

## Via TemplateProcessorHandler

- handleRequest()
  - _None!_
- renderPage()
  - loads domain.yaml
  - loads metadata.json
- renderPost()
  - loads domain.yaml
  - loads metadata.json
- renderStatic()
  - loads domain.yaml

## Via NavigationBuilder

- Filters metadata.json for pages and posts

## metadata.json use cases

- Getting next and previous posts
  - Given we know the `date` for a post, how to find the post which immediately precedes it?
  - Can Query for `domain#post` and add a FilterExpression `date < post.date` which will return a list of all early posts
  - This could return a very large, paginated list, however
  - I could create a GSI based around `type` and `date` (would need to add those columns to all post nodes). That would allow me to query by type and `<= post.date`. DynamoDB will return them in a fixed order; query results are always sorted by the range key (date). So asking for limit=1 during the query should return just the single closest post. Reverse the ordering for `>= post.date` next post.
    - Actually, this sounds like a use case for a Local Secondary Index, which allows me to have the same original partition key (`domain#type`) but a different sort key (`date`)
- Image and static references
- Page folder children / page parent links ( / page siblings would be nice too)
- 