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
-[ ] listAllNodesForProject(domain, type) -> List<ContentNode>

This should replace the existing 'listAllTemplates' and 'listAllPostsForProject' methods

### Pages only

-[ ] getParentNode(page, domain) -> ContentNode.FolderNode?

### Folders only

-[ ] getChildren(folder, domain) -> List<ContentNode>

How to do this?

### Counts of Nodes for Template or Static

-[ ] getCountOfPostsWithTemplate(srcKey, domain) -> Int
-[ ] getCountOfPagesWithTemplate(srcKey, domain) -> Int

They have to be separated because the partition key is by type

-[ ] getCountOf*WithImage(srcKey, domain) -> Int

_Hypothetical, not tracked at all yet_

### Posts only

-[ ] getPreviousPost(srcKey, domain, date) -> ContentNode.Post?
-[ ] getNextPost(srcKey, domain, date) - ContentNode.Post?

