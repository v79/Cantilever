<script context="module" lang="ts">
	import { writable } from 'svelte/store';
	import type { PostList } from '../../models/posts.svelte';

	// complete set of post metadata
	export const posts = writable<PostList>();

	export async function fetchPosts(token: string): Promise<number | Error | undefined> {
		console.log('postStore: Fetching posts');
		try {
			const response = await fetch('https://api.cantilevers.org/posts', {
				method: 'GET',
				headers: {
					Accept: 'application/json',
					Authorization: `Bearer ${token}`
				},
				mode: 'cors'
			});
			if (response.ok) {
				const data = await response.json();
				posts.set(data.data);
				return data.data.length;
			} else {
				throw new Error('Failed to fetch posts');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}
</script>
