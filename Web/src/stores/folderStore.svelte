<script lang="ts" context="module">
	/** This store manages the list of folders that pages live in */
	import { writable } from 'svelte/store';
	import { FolderNode, PageTree } from '../models/structure';

	//@ts-ignore
	export const pageTreeStore = writable<PageTree>({ lastUpdated: new Date(), rootFolder: null });

	export async function fetchFolderList(token: string): Promise<FolderNode[] | Error | undefined> {
		console.log('fetching folder list');

		return new Promise((resolve) => {
			fetch('https://api.cantilevers.org/project/pages/folders', {
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
					console.log('Loaded folder list');
					console.dir(data.data);
					// deserialize
					var tempFolders = new Array<FolderNode>();
					for (const f of data.data.folders) {
						tempFolders.push(new FolderNode(f.srcKey, f.lastUpdated, [], f.indexPage));
					}
                    var rootFolder = new FolderNode('folder', 'sources/pages/', tempFolders, null);
					// set pageTreeStore
					pageTreeStore.set({
						lastUpdated: data.lastUpdated,
						rootFolder: rootFolder
					});
					resolve(tempFolders);
				})
				.catch((error: Error) => {
					console.log(error);
					resolve(error);
				});
		});
	}
</script>
