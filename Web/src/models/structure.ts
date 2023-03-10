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

export interface Post {
    title: string;
    srcKey: string;
    url: string,
    templateKey: string,
    date: string,
    lastUpdated: string;
}

export interface MarkdownPost {
    post: Post;
    body: String;
}