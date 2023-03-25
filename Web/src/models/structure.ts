export interface Structure {
    layouts: Layout[];
    posts: Post[];
    postCount: number;
}

export interface Layout {
    template: Template;
}

export interface Template {
    key: string;
    lastUpdated: string;
}

export interface AllPosts {
    count: number,
    posts: Array<Post>
}

export interface AllPages {
    count: number,
    pages: Array<Page>
}

export interface AllTemplates {
    count: number,
    templates: Array<Template>
}

export interface Post {
    title: string;
    srcKey: string;
    url: string,
    templateKey: string,
    date: string,
    lastUpdated: string;
}

export interface Page {
    srcKey: string,
    templateKey: string,
    url: string,
    lastUpdated: string,
    attributeKeys: Set<string>,
    sectionKeys: Set<string>
}

export interface MarkdownPost {
    post: Post;
    body: String;
}