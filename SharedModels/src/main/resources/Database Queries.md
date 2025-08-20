## Database queries

### Projects

Projects are not ContentNodes, though I suppose they could be.

-[x] getProject(domain) -> CantileverProject
-[x] saveProject(domain) -> Boolean
-[x] listAllProjects -> List<CantileverProject>

### General Node operations

ContentNodes represent Posts, Pages, Templates, Statics, Images, Folders

-[x] upsertContentNode(srcKey, domain, type, node, attributes) -> Boolean
-[x] deleteContentNode(srcKey, domain, type)
-[x] getContentNode(srcKey, domain, type) -> ContentNode?
-[x] getNodeCount(domain, type) -> Int
-[x] listAllNodesForProject(domain, type) -> List<ContentNode>

### Pages only

-[ ] getParentNode(page, domain) -> ContentNode.FolderNode?

### Folders only

-[ ] getChildren(folder, domain) -> List<ContentNode>

How to do this?

### Counts of Nodes for Template or Static

-[ ] getKeyListMatchingTemplate(domain, type?, templateKey) -> List<SrcKey>

-[x] getKeyListMatchingAttributes(domain, type, attributesMap) -> List<SrcKey> 

This works for custom attributes on posts, as they will be keyed as "attr#<keyname>". But does not solve the template query, because template is not a custom attribute, but a known key for ContentNode.PostNode.
-[ ] getCountOf*WithImage(srcKey, domain) -> Int

_Hypothetical, not tracked at all yet_

### Posts only

-[ ] getPreviousPost(srcKey, domain, date) -> ContentNode.Post?
-[ ] getNextPost(srcKey, domain, date) - ContentNode.Post?

