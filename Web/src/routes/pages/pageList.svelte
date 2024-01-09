<script lang="ts">
	import { Modal } from 'flowbite-svelte';
	import { onDestroy, tick } from 'svelte';
	import TextInput from '../../components/forms/textInput.svelte';
	import { spinnerStore } from '../../components/utilities/spinnerWrapper.svelte';
	import type { Template, TreeNode } from '../../models/structure';
	import {
		FileType,
		FolderNode,
		MarkdownContent,
		Page,
		PageTree,
		TemplateMetadata
	} from '../../models/structure';
	import { activeStore } from '../../stores/appStatusStore.svelte';
	import { markdownStore } from '../../stores/markdownContentStore.svelte';
	import { notificationStore } from '../../stores/notificationStore.svelte';
	import {
		allTemplatesStore,
		fetchTemplateMetadata,
		fetchTemplates
	} from '../../stores/templateStore.svelte';
	import { userStore } from '../../stores/userStore.svelte';
	import { fetchFolderList, pageTreeStore } from '../../stores/folderStore.svelte';
	import PageTreeView from './pageTreeView.svelte';
	import { allPagesStore, fetchPage, fetchPageList } from '../../stores/pagesStore.svelte';

	$: rootFolder = $pageTreeStore.rootFolder;

	let newFolderModal = false;
	let newFolderName = '';
	let newPageModal = false;
	let folders: FolderNode[] = [];
	let templates: Template[] = [];
	let selectedParentFolder: FolderNode | undefined = undefined;
	let selectedTemplate: Template | undefined = undefined;

	$: newFolderValid = selectedParentFolder && newFolderName !== '';
	$: newPageValid = selectedParentFolder && selectedTemplate;

	async function loadAllPages() {
		console.log('Loading all pages json...');
		let token = $userStore.token;
		notificationStore.set({ shown: false, message: '', type: 'info' });

		// fetch the page list from the server
		let response = await fetchPageList($userStore.token);
		if (response instanceof Error) {
			notificationStore.set({
				message: response.message,
				shown: true,
				type: 'error'
			});
			$spinnerStore.shown = false;
			return {};
		} else {
			console.log($allPagesStore.count + ' pages fetched');
			// now we need to fetch the folder list from the server
			let folderResponse = await fetchFolderList($userStore.token);
			if (folderResponse instanceof Error) {
				notificationStore.set({
					message: folderResponse.message,
					shown: true,
					type: 'error'
				});
				$spinnerStore.shown = false;
				return {};
			}
			// and then combine the two into a single tree
			var rootFolder = new FolderNode('folder', 'sources/pages/', new Array<TreeNode>(), null);
			addFoldersToRoot(
				$pageTreeStore.rootFolder,
				$pageTreeStore.rootFolder.children,
				$allPagesStore.pages
			);
		}
		$notificationStore.message = 'Loaded all pages ' + $activeStore.activeFile;
		$notificationStore.shown = true;
		$spinnerStore.shown = false;
	}

	/**
	 * Loop through all the pages and add them to their parent folder, or the root folder if not found.
	 * Finally, update the root folder in the pageTreeStore.
	 * @param root
	 * @param folders
	 * @param pages
	 */
	function addFoldersToRoot(root: FolderNode, folders: Array<TreeNode>, pages: Array<TreeNode>) {
		if (folders === undefined || pages === undefined) {
			console.log('No folders or pages to add to root');
			return;
		}

		for (const node of pages) {
			const page = node as Page;
			const parentFolder = folders.find((folder) => folder.srcKey === page.parent) as FolderNode;
			if (parentFolder) {
				console.log('Adding page ' + page.srcKey + ' to folder ' + parentFolder.srcKey);
				parentFolder.children.push(page);
				parentFolder.count++;
			} else {
				console.log('Adding page ' + page.srcKey + ' to root folder');
				root.children.push(page);
				root.count++;
			}
		}
		$pageTreeStore.rootFolder = root;
	}
	/**
	 * TODO: Move this to the store
	 * Load the markdown for the specified page srcKey.
	 * @param srcKey
	 */
	function loadMarkdown(srcKey: string) {
		const pagesFolder: string = 'sources/pages/';
		let token = $userStore.token;
		console.log('Loading markdown file... ' + srcKey);
		spinnerStore.set({ shown: true, message: 'Loading markdown file... ' + srcKey });
		notificationStore.set({ shown: false, message: '', type: 'info' });
		tick();
		let page = fetchPage(token, srcKey);
		if (page instanceof Error) {
			notificationStore.set({
				message: page.message,
				shown: true,
				type: 'error'
			});
			$spinnerStore.shown = false;
			return {};
		}
		$activeStore.activeFile = decodeURIComponent($markdownStore.metadata!!.srcKey);
		$activeStore.isNewFile = false;
		$activeStore.hasChanged = false;
		$activeStore.isValid = true;
		$activeStore.fileType = FileType.Page;
		$activeStore.newSlug = $markdownStore.metadata!!.slug;
		let leafIndex = $activeStore.activeFile.lastIndexOf('/');
		let folderString = '';
		if (leafIndex >= pagesFolder.length) {
			folderString = $activeStore.activeFile.substring(pagesFolder.length, leafIndex);
		}
		let folder = new FolderNode('folder', pagesFolder + folderString, [], null);
		$activeStore.folder = folder;
		$notificationStore.message = 'Loaded file ' + $activeStore.activeFile;
		$notificationStore.shown = true;
		$spinnerStore.shown = false;
	}

	// @deprecated
	async function rebuild() {
		let token = $userStore.token;
		console.log('Regenerating project pages file...');
		fetch('https://api.cantilevers.org/project/pages/rebuild', {
			method: 'PUT',
			headers: {
				Accept: 'application/json',
				Authorization: 'Bearer ' + token,
				'X-Content-Length': '0'
			},
			mode: 'cors'
		})
			.then((response) => response.json())
			.then((data) => {
				console.log(data);
				notificationStore.set({
					message: data.data,
					shown: true,
					type: 'success'
				});
				loadAllPages();
			})
			.catch((error) => {
				console.log(error);
				notificationStore.set({
					message: error,
					shown: true,
					type: 'error'
				});
				$spinnerStore.shown = false;
			});
	}

	/**
	 * Start the new page flow
	 */
	function createNewPage() {
		if (selectedTemplate && selectedParentFolder) {
			fetchTemplateMetadata($userStore.token, selectedTemplate.key).then((response) => {
				console.dir(response);
				if (response instanceof Error) {
					notificationStore.set({
						message: response.message,
						shown: true,
						type: 'error'
					});
					$spinnerStore.shown = false;
				} else if (response instanceof TemplateMetadata) {
					setupNewPage(selectedTemplate!!, response, selectedParentFolder!!);
				} else {
					console.log('Could not create a new page; invalid response to fetchTemplateMetadata');
				}
			});
		}
	}

	/**
	 * Create a blank, empty page and update the activeStore to update the UI, putting user into edit mode.
	 * This needs to load the template file and process it to set up the custom attributes and sections.
	 * @param template
	 */
	function setupNewPage(template: Template, metadata: TemplateMetadata, folder: FolderNode) {
		var newMDPage: MarkdownContent = {
			body: '',
			metadata: new Page(
				'page',
				'',
				'',
				template.key,
				'',
				new Date(),
				new Map<string, string>(),
				new Map(metadata.sections.map((obj) => [obj, ''])),
				false,
				''
			)
		};
		$markdownStore.metadata = newMDPage.metadata;
		$activeStore.activeFile = '';
		$activeStore.isNewFile = true;
		$activeStore.isValid = false;
		$activeStore.newSlug = '';
		$activeStore.hasChanged = false;
		if (selectedParentFolder) {
			$activeStore.folder = folder;
		}

		console.log('Creating new page');
		markdownStore.set(newMDPage);
	}

	/**
	 * Return just the folders in the pageTreeStore
	 */
	function getFolders(): FolderNode[] {
		if ($pageTreeStore.rootFolder.children === undefined) {
			console.log('Need to load folders');
			let folders = fetchFolderList($userStore.token);
			console.log('after fetch: folders=' + folders);
			if (folders instanceof Error) {
				notificationStore.set({
					message: folders.message,
					shown: true,
					type: 'error'
				});
				$spinnerStore.shown = false;
				return [];
			} else if (folders instanceof Array) {
				return folders;
			} else {
				return [];
			}
		} else {
			console.log('Folders already loaded');
			console.dir($pageTreeStore.rootFolder.children);
			return $pageTreeStore.rootFolder.children.filter((node) => node instanceof FolderNode) as FolderNode[];
		}
	}

	function getTemplates() {
		if ($allTemplatesStore.count > 0) {
			return $allTemplatesStore.templates;
		} else {
			console.log('Need to load templates');
			fetchTemplates($userStore.token);
		}
		return [];
	}

	/**
	 * Fetch all the folders from the pageTreeStore, then open the new folder modal
	 */
	function openFolderModal() {
		selectedParentFolder = undefined;
		selectedTemplate = undefined;
		folders = $pageTreeStore.rootFolder.children as FolderNode[];
		newFolderModal = true;
	}

	/**
	 * Fetch all the templates and folders, then open the new page modal
	 */
	async function openNewPageModal() {
		selectedParentFolder = undefined;
		selectedTemplate = undefined;
		console.log('await getFolders()');
		folders = getFolders();
		templates = getTemplates();
		newPageModal = true;
	}

	/**
	 * Create a new folder in `selectedParentFolder` with name `newFolderName`.
	 */
	function createNewFolder() {
		const prefix = '/sources/pages/';
		let folderName = encodeURIComponent(
			(selectedParentFolder ? selectedParentFolder.srcKey : '') + newFolderName
		);
		console.log('Creating new folder ' + folderName);

		// TODO: move this to the pageTreeStore

		fetch('https://api.cantilevers.org/project/pages/folder/new/' + folderName, {
			method: 'PUT',
			headers: {
				Accept: 'text/plain',
				Authorization: 'Bearer ' + $userStore.token,
				'Content-Type': 'application/json',
				'X-Content-Length': '0'
			},
			mode: 'cors'
		})
			.then((response) => {
				if (!response.ok) {
					throw response;
				} else {
					response.text();
				}
			})
			.then((data) => {
				notificationStore.set({
					message: decodeURI(folderName) + ' saved. ' + data,
					shown: true,
					type: 'success'
				});
				loadAllPages();
			})
			.catch((error) => {
				notificationStore.set({
					message: 'Error creating folder: ' + error,
					shown: true,
					type: 'error'
				});
				console.log(error);
			});
		newFolderName = '';
		selectedParentFolder = undefined;
		newFolderModal = false;
		$spinnerStore.shown = false;
	}

	const userStoreUnsubscribe = userStore.subscribe((data) => {
		if (data) {
			loadAllPages();
		}
	});

	onDestroy(userStoreUnsubscribe);
</script>

<h3 class="px-4 py-4 text-center text-2xl font-bold text-slate-900">Pages</h3>

{#if $userStore === undefined}
	<div class="px-8"><p class="text-warning text-lg">Login to see pages</p></div>
{:else}
	<div class="flex items-center justify-center" role="group">
		<button
			class="inline-block rounded-l bg-purple-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
			on:click={(e) => {
				spinnerStore.set({ shown: true, message: 'Rebuilding project...' });
				tick().then(() => rebuild());
			}}>Rebuild</button>
		<button
			class="inline-block bg-purple-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
			on:click={(e) => {
				console.log('Show spinner');
				spinnerStore.set({ shown: true, message: 'Reloading project...' });
				tick().then(() => loadAllPages());
			}}>Reload</button>
		<button
			class="easy-in-out inline-block bg-purple-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
			on:click={openFolderModal}
			>Add folder
		</button>

		<button
			type="button"
			on:click={openNewPageModal}
			class="inline-block rounded-r bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
			>New Page</button>
	</div>
	<div class="px-8">
		{#if $pageTreeStore && $pageTreeStore.rootFolder}
			<h4 class="text-right text-sm text-slate-900">{$pageTreeStore.rootFolder.count} pages</h4>
			<div class="justify-left flex py-2">
				<PageTreeView {rootFolder} onClickFn={loadMarkdown} />
			</div>
		{:else}
			<div class="flex items-center justify-center py-4">
				<div class="flex min-h-screen w-full items-center justify-center bg-gray-200">
					<svg
						class="h-12 w-12 animate-spin text-indigo-400"
						viewBox="0 0 24 24"
						fill="none"
						xmlns="http://www.w3.org/2000/svg">
						<path
							d="M12 4.75V6.25"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M17.1266 6.87347L16.0659 7.93413"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M19.25 12L17.75 12"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M17.1266 17.1265L16.0659 16.0659"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M12 17.75V19.25"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M7.9342 16.0659L6.87354 17.1265"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M6.25 12L4.75 12"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M7.9342 7.93413L6.87354 6.87347"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
					</svg>
				</div>
			</div>
		{/if}
	</div>
{/if}

<Modal title="Add new folder" bind:open={newFolderModal} autoclose size="sm">
	<p>Select the parent folder for the new folder:</p>
	<label for="add-folder-select" class="block text-sm font-medium text-slate-600">Folder</label>
	<select
		bind:value={selectedParentFolder}
		id="add-folder-select"
		class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm">
		{#each folders as f}
			<option value={f}>
				{f.srcKey}
			</option>
		{/each}
	</select>

	<p>Selected: {selectedParentFolder ? selectedParentFolder.srcKey : ''}</p>

	<TextInput name="new-folder-name" bind:value={newFolderName} label="Name" required />

	<svelte:fragment slot="footer">
		<button
			type="button"
			class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
			>Cancel</button>
		<button
			type="button"
			disabled={!newFolderValid}
			on:click={createNewFolder}
			class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg disabled:bg-slate-400"
			>Create</button>
	</svelte:fragment>
</Modal>

<Modal title="Create new page" bind:open={newPageModal} autoclose size="sm">
	<p>Choose template and parent folder for page</p>

	<label for="template-select" class="block text-sm font-medium text-slate-600">Template</label>
	<select
		bind:value={selectedTemplate}
		id="template-select"
		class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm">
		{#each $allTemplatesStore.templates as template}
			<option value={template}>
				{template.key}
			</option>
		{/each}
	</select>

	<label for="new-page-folder-select" class="block text-sm font-medium text-slate-600"
		>Folder</label>
	<select
		bind:value={selectedParentFolder}
		id="new-page-folder-select"
		class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm">
		{#each folders as f}
			<option value={f}>
				{f.srcKey}
			</option>
		{/each}
	</select>

	<svelte:fragment slot="footer">
		<button
			type="button"
			class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
			>Cancel</button>
		<button
			type="button"
			disabled={!newPageValid}
			on:click={createNewPage}
			class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg disabled:bg-slate-400"
			>Create</button>
	</svelte:fragment>
</Modal>
