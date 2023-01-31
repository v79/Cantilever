<script context="module" lang="ts">
	import { writable } from 'svelte/store';
	import type { Post, Structure } from '../models/structure';

	export const structureStore = writable<Structure>({ layouts: [], posts: [], postCount: 0 });
	export const postStore = writable<Post[]>();
</script>
