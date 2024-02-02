<script lang="ts" context="module">
	import { MarkdownContent, PageItem } from '$lib/models/markdown';
	import { type PageList, type FolderList, PageNode } from '$lib/models/pages.svelte';
	import { markdownStore } from '$lib/stores/contentStore.svelte';
	import { writable, get } from 'svelte/store';

	export const pages = writable<PageList>();
	export const folders = writable<FolderList>();

	// fetch list of pages (not folders) from server
	export async function fetchPages(token: string): Promise<number | Error> {
		console.log('pageStore: Fetching pages and folders');
		try {
			const response = await fetch('https://api.cantilevers.org/pages', {
				method: 'GET',
				headers: {
					Accept: 'application/json',
					Authorization: `Bearer ${token}`
				},
				mode: 'cors'
			});
			if (response.ok) {
				/** @type {FolderList} */
				const data = await response.json();
				pages.set(data.data);
				return data.data.count as number;
			} else {
				throw new Error('Failed to fetch pages and folders');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}

	// fetch the list of folders from server
	export async function fetchFolders(token: string): Promise<number | Error> {
		console.log('pageStore: Fetching folders');
		try {
			const response = await fetch('https://api.cantilevers.org/folders', {
				method: 'GET',
				headers: {
					Accept: 'application/json',
					Authorization: `Bearer ${token}`
				},
				mode: 'cors'
			});
			if (response.ok) {
				/** @type {PageList} */
				const data = await response.json();
				folders.set(data.data);
				return data.data.count as number;
			} else {
				throw new Error('Failed to fetch folders');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}

	export async function fetchPage(srcKey: string, token: string): Promise<Error | string> {
		console.log('pageStore: Fetching page', srcKey);
		try {
			const encodedKey = encodeURIComponent(srcKey);
			const response = await fetch(`https://api.cantilevers.org/pages/${encodedKey}`, {
				method: 'GET',
				headers: {
					Accept: 'application/json',
					Authorization: `Bearer ${token}`
				},
				mode: 'cors'
			});
			if (response.ok) {
				const data = await response.json();
				var tmpPage = new MarkdownContent(
					new PageItem(
						data.data.metadata.title,
						data.data.metadata.srcKey,
						data.data.metadata.templateKey,
						data.data.metadata.url,
						data.data.metadata.lastUpdated,
						data.data.metadata.attributes,
						data.data.metadata.sections,
						data.data.metadata.isRoot,
						data.data.metadata.parent,
						false
					),
					data.data.body
				);
				markdownStore.set(tmpPage);
				return data.data.metadata.title;
			} else {
				throw new Error('Failed to fetch page');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}

	export async function savePage(srcKey: string, token: string): Promise<Error | string> {
		console.log('pageStore: Saving page');
		let content: MarkdownContent = get(markdownStore);
		if (content.metadata && content.metadata instanceof PageItem) {
			try {
				// the backend ContentNode.PageNode is slightly different from the frontend PageItem
				// remove isNew, set slug
				//@ts-ignore
				delete content.metadata.isNew; // or how about replacing content with a new object which does not contain isNew?
				content.metadata.slug = content.metadata.srcKey;
				const response = await fetch('https://api.cantilevers.org/pages/save', {
					method: 'POST',
					headers: {
						'Content-Type': 'application/json',
						Accept: 'text/plain',
						Authorization: `Bearer ${token}`
					},
					mode: 'cors',
					body: JSON.stringify(content.metadata)
				});
				if (response.ok) {
					const data = await response.text();
					return data;
				} else {
					throw new Error('Failed to save page');
				}
			} catch (error) {
				console.error(error);
				return error as Error;
			}
		} else {
			console.error('pageStore: savePage: metadata is not a PageItem');
			return new Error('pageStore: savePage: metadata is not a PageItem');
		}
	}
</script>
