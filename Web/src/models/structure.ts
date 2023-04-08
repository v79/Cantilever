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

export abstract class MarkdownItem {
	title: string;
	srcKey: string;
	templateKey: string;
	url: string;
	lastUpdated: Date;

	constructor(title: string, srcKey: string, templateKey: string, url: string, lastUpdated: Date) {
		this.title = title;
		this.srcKey = srcKey;
		this.templateKey = templateKey;
		this.url = url;
		this.lastUpdated = lastUpdated;
	}

	abstract getDateString(): string;
}

export class Post extends MarkdownItem {
	date: Date;

	constructor(
		title: string,
		srcKey: string,
		templateKey: string,
		url: string,
		lastUpdated: Date,
		date: Date
	) {
		super(title, srcKey, templateKey, url, lastUpdated);

		this.date = date;
	}

	getDateString(): string {
		return this.date.toLocaleDateString('en-GB');
	}
}

export function getDateString(item: MarkdownItem) {
	console.log('Called getDateString function for item');
	console.dir(item);
	if (item instanceof Post) {
		console.log('Returning date string for Post');
		return (item as Post).getDateString();
	}
	console.log('Markdown item wasnt a Post');
	return '';
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
