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
    lastUpdated: Date;
}

export interface AllPosts {
    count: number,
    lastUpdated: Date,
    posts: Array<Post>
}

export interface AllPages {
    count: number,
    lastUpdated: Date,
    pages: Array<Page>
}

export interface AllTemplates {
    count: number,
    lastUpdated: Date,
    templates: Array<Template>
}

export interface Post {
    title: string;
    srcKey: string;
    url: string,
    templateKey: string,
    date: Date,
    lastUpdated: Date;
}

export interface Page {
    srcKey: string,
    templateKey: string,
    url: string,
    lastUpdated: Date,
    attributeKeys: Set<string>,
    sectionKeys: Set<string>
}

export interface MarkdownPost {
    post: Post;
    body: String;
}