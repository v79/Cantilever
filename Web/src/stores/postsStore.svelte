<script context="module" lang="ts">
    import {writable} from 'svelte/store';
    import type {AllPosts, Post} from '../models/structure';

    export const allPostsStore = writable<AllPosts>({ count: 0, posts: [] });
	export const postStore = writable<Post[]>();
</script>
