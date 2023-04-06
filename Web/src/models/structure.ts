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

	getDateString(): string;
}

export class Post implements MarkdownItem {
	title: string;
	srcKey: string;
	templateKey: string;
	url: string;
	lastUpdated: Date;
	date: Date;

	constructor(
		title: string,
		srcKey: string,
		templateKey: string,
		url: string,
		lastUpdated: Date,
		date: Date
	) {
		this.title = title;
		this.srcKey = srcKey;
		this.templateKey = templateKey;
		this.url = url;
		this.lastUpdated = lastUpdated;
		this.date = date;
	}

	getDateString(): string {
		return this.date.toLocaleDateString('en-GB');
	}
}

export class Page implements MarkdownItem {
	title: string;
	srcKey: string;
	templateKey: string;
	url: string;
	lastUpdated: Date;
	attributeKeys: Set<string>;
	sectionKeys: Set<string>;

	constructor(
		title: string,
		srcKey: string,
		templateKey: string,
		url: string,
		lastUpdated: Date,
		attributeKeys: Set<string>,
		sectionKeys: Set<string>
	) {
		this.title = title;
		this.srcKey = srcKey;
		this.templateKey = templateKey;
		this.url = url;
		this.lastUpdated = lastUpdated;
		this.attributeKeys = attributeKeys;
		this.sectionKeys = sectionKeys;
	}

	getDateString(): string {
		return this.lastUpdated.toLocaleDateString('en-GB');
	}
}

export interface MarkdownPost {
	post: Post;
	body: String;
}
