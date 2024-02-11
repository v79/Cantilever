<script lang="ts" context="module">
	import { ContentNode } from '$lib/models/common.svelte';

	export interface TemplateList {
		count: number;
		templates: TemplateNode[];
	}

	/**
	 * Handlebars template, which is a type of content node.
	 * This is the actual content of the template, though the body will  not be included when fetching the list of templates
	 */
	export class TemplateNode extends ContentNode {
		title: string;
		sections: Array<string>;
		body: string;

		constructor(
			srcKey: string,
			lastUpdated: Date,
			title: string,
			sections: Array<string>,
			body: string
		) {
			super(srcKey, lastUpdated, '');
			this.title = title;
			this.sections = sections;
			this.body = body;
		}
	}

	/**
	 * DTO for template usage, lists the keys of the pages and posts which use a given template
	 */
	export class TemplateUsageDTO {
		count: number;
		pages: Array<string>;
		posts: Array<string>;

		constructor(count: number, pages: Array<string>, posts: Array<string>) {
			this.count = count;
			this.pages = pages;
			this.posts = posts;
		}
	}
</script>
