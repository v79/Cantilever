<script lang="ts" context="module">
	import { ContentNode } from '$lib/models/common.svelte';

	export interface PostList {
		count: number;
		posts: PostNode[];
	}

	/**
	 * Blog post, which is a type of content node.
     * Does not contain the actual content of the post, only metadata.
	 */
	export class PostNode extends ContentNode {
		title: string;
		templateKey: string;
		slug: string;
		date: Date;
		attributes: Map<string, string>;

		constructor(
			srcKey: string,
			lastUpdated: Date,
			url: string | undefined,
			title: string,
			templateKey: string,
			slug: string,
			date: Date,
			attributes: Map<string, string>
		) {
			super(srcKey, lastUpdated, url);
			this.title = title;
			this.templateKey = templateKey;
			this.slug = slug;
			this.date = date;
			this.attributes = attributes;
		}
	}
</script>
