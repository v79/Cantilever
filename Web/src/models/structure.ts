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
	metadata: TemplateMetadata;
	lastUpdated: Date;

	constructor(key: string, lastUpdated: Date, metadata: TemplateMetadata) {
		this.key = key;
		this.lastUpdated = lastUpdated;
		this.metadata = metadata;
	}
}


/**
 * Yaml frontmatter for Templates
 */
export class TemplateMetadata {
	name: string;
	sections: Array<string>;

	constructor(name: string, sections: Array<string>) {
		this.name = name;
		this.sections = sections;
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
 * List of valid things that can be loaded and edited by the web app
 */
export enum FileType {
	Post = 'post',
	Page = 'page',
	Template = 'template'
}

/**
 * Parent class for the tree of pages and folders
 */
export class PageTree {
	lastUpdated: Date;
	container: FolderNode;

	constructor(lastUpdated: Date, containerNode: FolderNode) {
		this.lastUpdated = lastUpdated;
		this.container = containerNode;
	}
}

/**
 * FolderNode represents a folder, or more accurately a shared prefix in S3
 */
export class FolderNode implements TreeNode {
	nodeType: string;
	srcKey: string;
	count: number;
	children: TreeNode[];

	constructor(type: string, srcKey: string, count: number, children: TreeNode[]) {
		this.nodeType = type;
		this.srcKey = srcKey;
		this.count = count;
		this.children = children;
	}

	depthSort() {
		return this.children.sort((a: TreeNode, b: TreeNode) => {
			if (a.nodeType === 'folder' && b.nodeType === 'folder') {
				const aFolder = a as FolderNode;
				const bFolder = b as FolderNode;
				if (aFolder.srcKey.split('/').length > bFolder.srcKey.split('/').length) return 1;
				if (aFolder.srcKey.split('/').length < bFolder.srcKey.split('/').length) return -1;
				return 0;
			}
			return 0;
		});
	}
}

/**
 * Common interface for Pages and FolderNodes so they can be valid children of FolderNodes
 */
export interface TreeNode {
	nodeType: string;
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
	name: string;
	shortName: string;
	lastUpdated: Date;

	constructor(key: string, name: string, lastUpdated: Date) {
		this.key = key;
		this.name = name;
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
export class Page implements MetadataItem, TreeNode {
	nodeType: string;
	title: string;
	srcKey: string;
	templateKey: string;
	url: string;
	lastUpdated: Date;
	attributes: Map<string, string>;
	sections: Map<string, string>;

	constructor(
		nodeType: string,
		title: string,
		srcKey: string,
		templateKey: string,
		url: string,
		lastUpdated: Date,
		attributes: Map<string, string>,
		sections: Map<string, string>
	) {
		this.nodeType = nodeType;
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
	This class contains a template, and the body (the raw HTML)
*/
export class HandlebarsTemplate {
	template: Template;
	body: string;

	constructor(template: Template, body: string) {
		this.template = template;
		this.body = body;
	}
}

/*
	The [CantileverProject] represents some metadata about the entire project.
*/
export class CantileverProject {
	projectName: string;
	author: string;
	dateFormat: string;
	dateTimeFormat: string;
	imageResolutions: Map<string, ImgRes>;
	attributes: Map<string, string>;

	constructor(
		projectName: string,
		author: string,
		dateFormat: string,
		dateTimeFormat: string,
		imageResolutions: Map<string, ImgRes>,
		attributes: Map<string, string>
	) {
		this.projectName = projectName;
		this.author = author;
		this.dateFormat = dateFormat;
		this.dateTimeFormat = dateTimeFormat;
		this.imageResolutions = imageResolutions;
		this.attributes = attributes;
	}
}

/*
	The [ImgRes] Represents an image resolution in pixels.
*/
export class ImgRes {
	x: number | undefined;
	y: number | undefined;

	constructor(x: number, y: number) {
		this.x = x;
		this.y = y;
	}

	toJSON(): string {
		return this.getStringX() + 'x' + this.getStringY();
	}

	getStringX(): string {
		if (isNaN(this.x!!)) {
			return '';
		} else {
			return '' + this.x;
		}
	}
	getStringY(): string {
		if (isNaN(this.y!!)) {
			return '';
		} else {
			return '' + this.y;
		}
	}
}

/**
 * Convert a string like "640x480" into a [ImgRes] object with values x=640, y=480
 * @param resString
 * @returns an [ImgRes] object with the appropriate dimensions. If a dimension is not found, it will be returned as NaN.
 */
export function parseResString(resString: string) {
	let xS: string = resString.substring(0, resString.indexOf('x'));
	let yS: string = resString.substring(resString.indexOf('x') + 1);
	return new ImgRes(parseInt(xS), parseInt(yS));
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
