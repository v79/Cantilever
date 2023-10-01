<script context="module" lang="ts">
	import { writable } from 'svelte/store';
	import type {
		AllTemplates,
		AllPages,
		AllPosts,
		Page,
		Post,
		Template,
		PageTree
	} from '../models/structure';

	export const allPostsStore = writable<AllPosts>({ count: 0, lastUpdated: new Date(), posts: [] });
	export const postStore = writable<Post[]>();

	export const allPagesStore = writable<AllPages>({ count: 0, lastUpdated: new Date(), pages: [] });
	export const pageStore = writable<Page[]>();

	//@ts-ignore
	export const pageTreeStore = writable<PageTree>({ lastUpdated: new Date(), root: {} });

	export const allTemplatesStore = writable<AllTemplates>({
		count: 0,
		lastUpdated: new Date(),
		templates: []
	});
	export const templateStore = writable<Template[]>();
</script>
