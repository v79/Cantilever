<script context="module" lang="ts">
	import { writable } from 'svelte/store';
	import type { PostList } from '../../models/posts.svelte';
	import { PostItem, MarkdownContent } from '../../models/markdown';
	import { markdownStore } from '../../stores/contentStore.svelte';

	// complete set of post metadata
	export const posts = writable<PostList>();

	// fetch list of posts from server
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

	export async function fetchPost(srcKey: string, token: string): Promise<MarkdownContent | Error | undefined> {
		console.log('postStore: Fetching post', srcKey);
		try {
			const encodedKey = encodeURIComponent(srcKey);
			const response = await fetch(`https://api.cantilevers.org/posts/${encodedKey}`, {
				method: 'GET',
				headers: {
					Accept: 'application/json',
					Authorization: `Bearer ${token}`
				},
				mode: 'cors'
			});
			if (response.ok) {
				const data = await response.json();
				console.log('postStore: Fetched post', data.data);
				var tmpPost = new MarkdownContent(
					new PostItem(
						data.data.title,
						data.data.srcKey,
						data.data.templateKey,
						data.data.slug,
						data.data.lastUpdated,
						data.data.date
					),
					data.data.body
				);
				markdownStore.set(tmpPost);
			} else {
				throw new Error('Failed to fetch post');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}
</script>
