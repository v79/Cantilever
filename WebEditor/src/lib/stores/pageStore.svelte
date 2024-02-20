<script lang="ts" context="module">
	import { MarkdownContent, PageItem } from '$lib/models/markdown';
	import {
		ReassignIndexRequestDTO,
		type FolderList,
		type PageList
	} from '$lib/models/pages.svelte';
	import { markdownStore } from '$lib/stores/contentStore.svelte';
	import { get, writable } from 'svelte/store';

	export const pages = createPagesStore();
	export const folders = createFoldersStore();
	const CLEAR_PAGES = { count: 0, pages: [] };
	const CLEAR_FOLDERS = { count: 0, folders: [] };

	function createPagesStore() {
		const { subscribe, set, update } = writable<PageList>();
		return {
			subscribe,
			set,
			update,
			clear: () => set(CLEAR_PAGES),
			isEmpty: () => get(pages).count === 0
		};
	}

	function createFoldersStore() {
		const { subscribe, set, update } = writable<FolderList>();
		return {
			subscribe,
			set,
			update,
			clear: () => set(CLEAR_FOLDERS),
			isEmpty: () => get(folders).count === 0
		};
	}

	// fetch list of pages (not folders) from server
	export async function fetchPages(token: string, projectDomain: string): Promise<number | Error> {
		console.log('pageStore: Fetching pages and folders');
		try {
			const response = await fetch('https://api.cantilevers.org/pages', {
				method: 'GET',
				headers: {
					Accept: 'application/json',
					Authorization: `Bearer ${token}`,
					'cantilever-project-domain': projectDomain
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
	export async function fetchFolders(
		token: string,
		projectDomain: string
	): Promise<number | Error> {
		console.log('pageStore: Fetching folders');
		try {
			const response = await fetch('https://api.cantilevers.org/folders', {
				method: 'GET',
				headers: {
					Accept: 'application/json',
					Authorization: `Bearer ${token}`,
					'cantilever-project-domain': projectDomain
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

	// fetch a single page from server, converting it to a MarkdownContent object
	export async function fetchPage(
		srcKey: string,
		token: string,
		projectDomain: string
	): Promise<Error | string> {
		console.log('pageStore: Fetching page', srcKey);
		try {
			const encodedKey = encodeURIComponent(srcKey);
			const response = await fetch(`https://api.cantilevers.org/pages/${encodedKey}`, {
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

	// create a page on the server
	export async function savePage(
		srcKey: string,
		token: string,
		projectDomain: string
	): Promise<Error | string> {
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
						Authorization: `Bearer ${token}`,
						'cantilever-project-domain': projectDomain
					},
					mode: 'cors',
					body: JSON.stringify(content.metadata)
				});
				if (response.ok) {
					const msg = await response.text();
					return msg;
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

	// create a new folder on the server
	export async function createFolder(
		srcKey: string,
		token: string,
		projectDomain: string
	): Promise<Error | string> {
		console.log('pageStore: Creating folder ' + srcKey);
		try {
			const encodedKey = encodeURIComponent(srcKey);
			const response = await fetch('https://api.cantilevers.org/pages/folder/new/' + encodedKey, {
				method: 'PUT',
				headers: {
					'Content-Type': 'application/json',
					Accept: 'text/plain',
					Authorization: `Bearer ${token}`,
					'cantilever-project-domain': projectDomain,
					'X-Content-Length': '0'
				},
				mode: 'cors'
			});
			if (response.ok) {
				const msg = await response.text();
				return msg;
			} else {
				throw new Error('Failed to create folder');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}

	// delete a page from the server
	export async function deletePage(
		srcKey: string,
		token: string,
		projectDomain: string
	): Promise<Error | string> {
		console.log('pageStore: Deleting page ' + srcKey);
		try {
			const encodedKey = encodeURIComponent(srcKey);
			const response = await fetch('https://api.cantilevers.org/pages/' + encodedKey, {
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
				throw new Error('Failed to delete page');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}

	// delete a folder from the server
	export async function deleteFolder(
		srcKey: string,
		token: string,
		projectDomain: string
	): Promise<Error | string> {
		console.log('pageStore: Deleting folder ' + srcKey);
		try {
			const encodedKey = encodeURIComponent(srcKey);
			const response = await fetch('https://api.cantilevers.org/pages/folder/' + encodedKey, {
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
				throw new Error('Failed to delete folder');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}

	// reassign the index page for a folder
	export async function switchIndexPage(
		from: string,
		to: string,
		folder: string,
		token: string,
		projectDomain: string
	): Promise<Error | string> {
		let dto = new ReassignIndexRequestDTO(from, to, folder);
		console.log(
			'pageStore: Reassiging index page for folder ' + folder + ' from ' + from + ' to ' + to
		);
		try {
			const response = await fetch('https://api.cantilevers.org/pages/reassignIndex', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					Accept: 'text/plain',
					Authorization: `Bearer ${token}`,
					'cantilever-project-domain': projectDomain
				},
				body: JSON.stringify(dto),
				mode: 'cors'
			});
			if (response.ok) {
				const msg = await response.text();
				return msg;
			} else {
				throw new Error('Failed to reassign index page');
			}
		} catch (error) {
			console.error(error);
			return error as Error;
		}
	}
</script>
