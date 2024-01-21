<script context="module" lang="ts">
	import { writable, get } from 'svelte/store';
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
				return data.data.count as number;
			} else {
				throw new Error('Failed to fetch posts');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}

	export async function fetchPost(srcKey: string, token: string): Promise<Error | string> {
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
				// console.log('postStore: Fetched post', data.data);
				// date is a YYYY-MM-DD string, convert to Date
				data.data.date = new Date(data.data.date);
				var tmpPost = new MarkdownContent(
					new PostItem(
						data.data.title,
						data.data.srcKey,
						data.data.templateKey,
						data.data.slug,
						data.data.lastUpdated,
						data.data.date,
						false
					),
					data.data.body
				);
				markdownStore.set(tmpPost);
				return 'Loaded post ' + srcKey;
			} else {
				throw new Error('Failed to fetch post');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}

	export async function savePost(srcKey: string, token: string): Promise<Error | string> {
		console.log('postStore: Saving post', srcKey);
		let content: MarkdownContent = get(markdownStore);
		if (content.metadata && content.metadata instanceof PostItem) {
			try {
				// unwrap the MarkdownContent into something we can send to the server
				let postToSave = {
					title: content.metadata.title,
					templateKey: content.metadata.templateKey,
					srcKey: content.metadata.srcKey,
					slug: content.metadata.slug,
					// date is a Kotlin.LocalDate at the back end, so convert to YYYY-MM-DD string
					date: content.metadata.date.toISOString().split('T')[0],
					body: content.body
				};

				let postJson = JSON.stringify(postToSave);
				const response = await fetch(`https://api.cantilevers.org/posts/save`, {
					method: 'POST',
					headers: {
						Accept: 'text/plain',
						Authorization: `Bearer ${token}`,
						'Content-Type': 'application/json'
					},
					mode: 'cors',
					body: postJson
				});
				if (response.ok) {
					const data = await response.text();
					console.log('postStore: Saved post', data);
					return data;
				} else {
					console.log(response);
					throw new Error('Failed to save post ' + srcKey);
				}
			} catch (error) {
				console.error(error);
				return error as Error;
			}
		} else {
			console.error('postStore: savePost: metadata is not a PostItem');
			return new Error('postStore: savePost: metadata is not a PostItem');
		}
	}
</script>
