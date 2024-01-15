<script context="module" lang="ts">
	import { writable } from 'svelte/store';
	import type { AllPosts, Page, Post } from '../models/structure';

	export const allPostsStore = writable<AllPosts>({ count: 0, lastUpdated: new Date(), posts: [] });
	export const postStore = writable<Post[]>();

	export const POSTS_CLEAR: AllPosts = { count: 0, lastUpdated: new Date(), posts: [] };
</script>
