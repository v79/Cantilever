<script context="module" lang="ts">
    import {writable} from 'svelte/store';
    import type {AllPages, AllPosts, Page, Post} from '../models/structure';

    export const allPostsStore = writable<AllPosts>({ count: 0, lastUpdated: new Date(), posts: [] });
	export const postStore = writable<Post[]>();

	export const allPagesStore = writable<AllPages>({ count: 0, lastUpdated: new Date(), pages: [] });
	export const pageStore = writable<Page[]>();
</script>
