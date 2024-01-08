<script lang="ts" context="module">
	/** The page store keeps track of the Pages in the project */
	import { writable } from 'svelte/store';
	import type { AllPages, Page } from '../models/structure';

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
</script>
