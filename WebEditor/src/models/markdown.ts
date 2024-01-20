/**
 * Abstract class which represents all the common elements of a file which contains Markdown content, and will be rendered with the help of a Template.
 * Implementations include Post and Page.
 */
export abstract class MetadataItem {
	title: string;
	srcKey: string;
	templateKey: string;
	slug: string;
	lastUpdated: Date;
	isNew: boolean = false;

	constructor(
		title: string,
		srcKey: string,
		templateKey: string,
		slug: string,
		lastUpdated: Date,
		isNew: boolean
	) {
		this.title = title;
		this.srcKey = srcKey;
		this.templateKey = templateKey;
		this.slug = slug;
		this.lastUpdated = new Date(lastUpdated);
		this.isNew = isNew;
	}

	abstract getDateString(): string;
	abstract isValid(): boolean;
}

/**
 * A PostItem is a dated piece of content, can be considered a blog entry or news item perhaps.
 * This is metadata only, it does not contain the body content.
 */
export class PostItem extends MetadataItem {
	date: Date;

	constructor(
		title: string,
		srcKey: string,
		templateKey: string,
		url: string,
		lastUpdated: Date,
		date: Date,
		isNew: boolean
	) {
		super(title, srcKey, templateKey, url, lastUpdated, isNew);
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
export class Page extends MetadataItem {
	type: string;
	slug: string;
	attributes: Map<string, string>;
	sections: Map<string, string>;
	isRoot: boolean;
	parent: string | null;

	constructor(
		nodeType: string,
		title: string,
		srcKey: string,
		templateKey: string,
		slug: string,
		lastUpdated: Date,
		attributes: Map<string, string>,
		sections: Map<string, string>,
		isRoot: boolean,
		parent: string | null,
		isNew: boolean
	) {
		super(title, srcKey, templateKey, slug, lastUpdated, isNew);
		this.type = nodeType;
		this.slug = slug;
		this.attributes = attributes;
		this.sections = sections;
		this.isRoot = isRoot;
		this.parent = parent;
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
 * Ideally metadata would not be undefined but I can't find a workaround.
 */
export class MarkdownContent {
	metadata: PostItem | Page | undefined;
	body: string;

	constructor(metadata: PostItem | Page, body: string) {
		this.metadata = metadata;
		this.body = body;
	}
}
