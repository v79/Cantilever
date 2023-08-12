/**
 * Obsolete parent class for the project
 */
export interface Structure {
	layouts: Layout[];
	posts: Post[];
	postCount: number;
}

export interface Layout {
	template: Template;
}

/**
 * A template file will end in .html.hbs and represents a handlebars template
 */
export class Template {
	key: string;
	lastUpdated: Date;

	constructor(key: string, lastUpdated: Date) {
		this.key = key;
		this.lastUpdated = lastUpdated;
	}
}

/**
 * Wrapper collection for Posts
 */
export interface AllPosts {
	count: number;
	lastUpdated: Date;
	posts: Array<Post>;
}

/**
 * Wrapper collection for pages
 */
export interface AllPages {
	count: number;
	lastUpdated: Date;
	pages: Array<Page>;
}

/**
 * Wrapper collection for Templates
 */
export interface AllTemplates {
	count: number;
	lastUpdated: Date;
	templates: Array<Template>;
}

/**
 * Abstract class which represents all the common elements of a file which contains Markdown content, and will be rendered with the help of a Template.
 * Implementations include Post and Page.
 */
export abstract class MetadataItem {
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
		this.lastUpdated = new Date(lastUpdated);
	}

	abstract getDateString(): string;
	abstract isValid(): boolean;
}

/**
 * A handlebars item is different from a MetadataItem as it does not contain markdown content; it will be an HTML file (other types may follow) in the Handlebars templating format
 */
export class HandlebarsItem {
	key: string;
	shortName: string;
	lastUpdated: Date;

	constructor(key: string, lastUpdated: Date) {
		this.key = key;
		this.lastUpdated = new Date(lastUpdated);
		this.shortName = key.split('/').slice(-1).join();
	}

	getDateString(): string {
		return this.lastUpdated.toLocaleDateString('en-GB');
	}
}

/**
 * A Post is a dated piece of content, can be considered a blog entry or news item perhaps.
 * This is metadata only, it does not contain the body content.
 */
export class Post extends MetadataItem {
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

	isValid(): boolean {
		return this.title != '' && this.date != null;
	}
}

/**
 * A Page is a static piece of content, generally unchanging content at a fixed URL.
 * This is metadata only, it does not contain the body content.
 */
export class Page implements MetadataItem {
	title: string;
	srcKey: string;
	templateKey: string;
	url: string;
	lastUpdated: Date;
	attributes: Map<string, string>;
	sections: Map<string, string>;

	constructor(
		title: string,
		srcKey: string,
		templateKey: string,
		url: string,
		lastUpdated: Date,
		attributes: Map<string, string>,
		sections: Map<string, string>
	) {
		this.title = title;
		this.srcKey = srcKey;
		this.templateKey = templateKey;
		this.url = url;
		this.lastUpdated = lastUpdated;
		this.attributes = attributes;
		this.sections = sections;
	}

	getDateString(): string {
		return this.lastUpdated.toLocaleDateString('en-GB');
	}

	isValid(): boolean {
		return this.title != '' && this.templateKey != '';
	}
}

/**
 * When editing a piece of content, we need the Metadata and the body. This class combines both. It can represent any valid content item.
 * Ideally metadata would not be null but I can't find a workaround.
 */
export class MarkdownContent {
	metadata: Post | Page | null;
	body: string;

	constructor(metadata: Post | Page, body: string) {
		this.metadata = metadata;
		this.body = body;
	}
}

/*
	The [Template] class essentially represents the metadata of a handlebars template. This class contains the body as well.
*/
export class HandlebarsContent {
	template: Template;
	body: string;

	constructor(template: Template, body: string) {
		this.template = template;
		this.body = body;
	}
}
/**
 * Deprecated utility function
 * @param item
 * @returns
 */
export function getDateString(item: MetadataItem) {
	console.log('Called getDateString function for item');
	console.dir(item);
	if (item instanceof Post) {
		console.log('Returning date string for Post');
		return (item as Post).getDateString();
	}
	console.log('Markdown item wasnt a Post');
	return '';
}
