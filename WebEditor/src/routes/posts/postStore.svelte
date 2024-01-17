<script context="module" lang="ts">
	import { writable } from 'svelte/store';
	import type { PostNode } from '../../models/posts.svelte';
	import { resolve } from 'path';

	// complete set of post metadata
	export const posts = writable<PostNode[]>();

	export async function fetchPosts(token: string): Promise<number | Error | undefined> {
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
				posts.set(data);
				resolve(data.length);
			} else {
				throw new Error('Failed to fetch posts');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}
</script>
