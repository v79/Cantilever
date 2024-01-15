<script lang="ts" context="module">
	/** The page store keeps track of the Pages in the project */
	import { writable } from 'svelte/store';
	import { MarkdownContent, type AllPages, Page } from '../models/structure';
	import { markdownStore } from './markdownContentStore.svelte';

	export const allPagesStore = writable<AllPages>({ count: 0, lastUpdated: new Date(), pages: [] });
	export const pageStore = writable<Page[]>();

	export const PAGES_CLEAR: AllPages = { count: 0, lastUpdated: new Date(), pages: [] };

	/**
	 * Save the page to the project
	 * @param token
	 * @param pageJson the JSON string of the page to save
	 * @returns a message of success, or an error
	 */
	export async function savePage(token: string, pageJson: string): Promise<string | Error> {
		return new Promise((resolve) => {
			fetch('https://api.cantilevers.org/project/pages/', {
				method: 'POST',
				headers: {
					Accept: 'text/plain',
					Authorization: 'Bearer ' + token,
					'Content-Type': 'application/json'
				},
				body: pageJson,
				mode: 'cors'
			})
				.then((response) => response.text())
				.then((data) => {
					if (data === undefined) {
						throw new Error('No response to save page request');
					} else {
						resolve(data);
					}
				})
				.catch((error) => {
					console.log(error);
					resolve(new Error(error));
				});
		});
	}

	/**
	 * Fetch the complete page list and store it in the allPagesStore
	 * @param token
	 */
	export async function fetchPageList(token: string): Promise<string | Error> {
		return new Promise((resolve) => {
			fetch('https://api.cantilevers.org/project/pages', {
				method: 'GET',
				headers: {
					Accept: 'application/json',
					Authorization: 'Bearer ' + token
				},
				mode: 'cors'
			})
				.then((response) => response.json())
				.then((data) => {
					console.log('Loaded all pages json');
					// console.dir(data);
					if (data.data === undefined) {
						throw new Error(data.message);
					}
					allPagesStore.set(data.data);
					resolve('Loaded ' + data.data.count + ' pages');
				})
				.catch((error) => {
					console.log(error);
					resolve(new Error(error));
					return {};
				});
		});
	}

	export async function fetchPage(
		token: string,
		srcKey: string
	): Promise<MarkdownContent | Error | undefined> {
		return new Promise((resolve) => {
			fetch('https://api.cantilevers.org/project/pages/' + encodeURIComponent(srcKey), {
				method: 'GET',
				headers: {
					Accept: 'application/json',
					Authorization: 'Bearer ' + token
				},
				mode: 'cors'
			})
				.then((response) => response.json())
				.then((data) => {
					if (data.data === undefined) {
						throw new Error(data.message);
					}
					var tmpPage = new MarkdownContent(
						new Page(
							'page',
							data.data.metadata.title,
							data.data.metadata.srcKey,
							data.data.metadata.templateKey,
							data.data.metadata.slug,
							data.data.metadata.lastUpdated,
							new Map<string, string>(Object.entries(data.data.metadata.attributes)),
							new Map<string, string>(Object.entries(data.data.metadata.sections)),
							data.data.metadata.isRoot,
							data.data.metadata.parent
						),
						''
					);
					markdownStore.set(tmpPage);
					resolve(tmpPage);
				})
				.catch((error) => {
					console.log(error);
					resolve(new Error(error));
				});
		});
	}
</script>
