<script lang="ts" context="module">
	import { ContentNode } from '$lib/models/common.svelte';

	export interface PageList {
		count: number;
		pages: PageNode[];
	}

	export interface FolderList {
		count: number;
		folders: FolderNode[];
	}

	// Represented by 'rootfolder' in the returned JSON from the API
	export interface PageTree {
		lastUpdated: Date;
		srcKey: string;
		// for folders
		children?: PageTree[];
		indexPageKey?: string | null;

		// for pages
		isRoot?: boolean;
		parentFolder?: string;
		slug?: string;
		templateKey?: string;
		title?: string;
		type?: string;

	}

	export function getTreeItemType(item: PageTree): 'folder' | 'page' {
		// A folder has children, a page does not
		if ((item as any).children !== undefined) {
			return 'folder';
		} else {
			return 'page';
		}
	}

	export class PageNode extends ContentNode {
		title: string;
		templateKey: string;
		slug: string;
		isRoot: boolean;
		attributes: Map<string, string>;
		sections: Map<string, string>;
		parent: string | null;

		constructor(
			srcKey: string,
			lastUpdated: Date,
			url: string | undefined,
			title: string,
			templateKey: string,
			slug: string,
			isRoot: boolean,
			attributes: Map<string, string>,
			sections: Map<string, string>,
			parent: string | null
		) {
			super(srcKey, lastUpdated, url);
			this.title = title;
			this.templateKey = templateKey;
			this.slug = slug;
			this.isRoot = isRoot;
			this.attributes = attributes;
			this.sections = sections;
			this.parent = parent;
		}
	}

	export class FolderNode extends ContentNode {
		indexPage: string | null;
		children: string[];

		constructor(srcKey: string, lastUpdated: Date, indexPage: string | null, children: string[]) {
			super(srcKey, lastUpdated, '');
			this.indexPage = indexPage;
			this.children = children;
		}
	}

	export class ReassignIndexRequestDTO {
		from: string;
		to: string;
		folder: string;

		constructor(from: string, to: string, folder: string) {
			this.from = from;
			this.to = to;
			this.folder = folder;
		}
	}
</script>
