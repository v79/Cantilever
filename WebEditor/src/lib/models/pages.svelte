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
</script>
