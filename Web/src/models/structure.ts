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
	count: number;
	lastUpdated: Date;
	posts: Array<Post>;
}

export interface AllPages {
	count: number;
	lastUpdated: Date;
	pages: Array<Page>;
}

export interface AllTemplates {
	count: number;
	lastUpdated: Date;
	templates: Array<Template>;
}

export interface MarkdownItem {
	title: string;
	srcKey: string;
	templateKey: string;
	url: string;
	lastUpdated: Date;
}

export interface Post extends MarkdownItem {
	date: Date;
}

export interface Page extends MarkdownItem {
	attributeKeys: Set<string>;
	sectionKeys: Set<string>;
}

export interface MarkdownPost {
	post: Post;
	body: String;
}
