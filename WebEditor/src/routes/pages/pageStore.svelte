<script lang="ts" context="module">
	import type { PageList, FolderList } from '$lib/models/pages.svelte';
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
</script>
