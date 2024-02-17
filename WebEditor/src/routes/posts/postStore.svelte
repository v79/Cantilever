<script context="module" lang="ts">
	import { writable, get } from 'svelte/store';
	import type { PostList } from '$lib/models/posts.svelte';
	import { PostItem, MarkdownContent } from '$lib/models/markdown';
	import { markdownStore } from '$lib/stores/contentStore.svelte';

	// complete set of post metadata
	export const posts = writable<PostList>();

	// fetch list of posts from server
	export async function fetchPosts(token: string, projectDomain: string): Promise<number | Error> {
		console.log('postStore: Fetching posts for project ' + projectDomain);
		try {
			const response = await fetch('https://api.cantilevers.org/posts', {
				method: 'GET',
				headers: {
					Accept: 'application/json',
					Authorization: `Bearer ${token}`,
					'cantilever-project-domain': projectDomain
				},
				mode: 'cors'
			});
			if (response.ok) {
				/** @type {PostList} */
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

	// load a post from the server
	export async function fetchPost(
		srcKey: string,
		token: string,
		projectDomain
	): Promise<Error | string> {
		console.log('postStore: Fetching post', srcKey);
		try {
			const encodedKey = encodeURIComponent(srcKey);
			const response = await fetch(`https://api.cantilevers.org/posts/${encodedKey}`, {
				method: 'GET',
				headers: {
					Accept: 'application/json',
					Authorization: `Bearer ${token}`,
					'cantilever-project-domain': projectDomain
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

	// save a post to the server
	export async function savePost(
		srcKey: string,
		token: string,
		projectDomain: string
	): Promise<Error | string> {
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
						'Content-Type': 'application/json',
						'cantilever-project-domain': projectDomain
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

	// delete a post from the server
	export async function deletePost(
		srcKey: string,
		token: string,
		projectDomain: string
	): Promise<Error | string> {
		console.log('pageStore: Deleting post ' + srcKey);
		try {
			const encodedKey = encodeURIComponent(srcKey);
			const response = await fetch('https://api.cantilevers.org/posts/' + encodedKey, {
				method: 'DELETE',
				headers: {
					Accept: 'text/plain',
					Authorization: `Bearer ${token}`,
					'cantilever-project-domain': projectDomain
				},
				mode: 'cors'
			});
			if (response.ok) {
				const msg = await response.text();
				return msg;
			} else {
				throw new Error('Failed to delete post');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}
</script>
