<script lang="ts">
	import ListPlaceholder from '$lib/components/ListPlaceholder.svelte';
	import NestedFileList from '$lib/components/NestedFileList.svelte';
	import TextInput from '$lib/forms/textInput.svelte';
	import { markdownStore } from '$lib/stores/contentStore.svelte';
	import { userStore } from '$lib/stores/userStore.svelte';
	import {
		getModalStore,
		getToastStore,
		type ToastSettings,
		type TreeViewNode
	} from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import {
		Add,
		Delete,
		Folder,
		Home,
		Icon,
		Refresh,
		Save
	} from 'svelte-google-materialdesign-icons';
	import PostListItem from '../posts/PostListItem.svelte';
	import FolderIconComponent from './FolderIconComponent.svelte';
	import FolderListItem from './FolderListItem.svelte';
	import IndexPageIconComponent from './IndexPageIconComponent.svelte';
	import PageIconComponent from './PageIconComponent.svelte';
	import {
		createFolder,
		fetchFolders,
		fetchPage,
		fetchPages,
		folders,
		pages,
		savePage
	} from './pageStore.svelte';
	import SectionTabs from './SectionTabs.svelte';
	import { TemplateNode } from '$lib/models/templates.svelte';
	import { FolderNode, PageNode } from '$lib/models/pages.svelte';
	import { PageItem } from '$lib/models/markdown';
	import { createSlug } from '$lib/functions/createSlug';
	import CreateNewFolder from 'svelte-google-materialdesign-icons/Create_new_folder.svelte';

	const modalStore = getModalStore();
	const toastStore = getToastStore();

	let pgFolderNodes = [] as TreeViewNode[]; // for the treeview component#
	let expandedNodes = [] as string[];
	let pgTitle = 'Markdown Editor';
	let isNewPage = false;

	let homeIcon = Home;

	$: pgAndFoldersLabel = $pages?.count + ' pages in ' + $folders?.count + ' folders';
	$: isValid =
		$markdownStore.metadata?.srcKey != null ||
		(isNewPage && $markdownStore.metadata?.title != null);

	const toast: ToastSettings = {
		message: 'Loaded posts',
		background: 'variant-filled-success',
		hideDismiss: true
	};
	const errorToast: ToastSettings = {
		message: 'Failed to load posts',
		background: 'variant-filled-error'
	};

	const createNewPostModal = {
		type: 'component',
		component: 'createNewPageModal',
		meta: {
			modalTitle: 'Create new page',
			showOnlyWithSections: true,
			onFormSubmit: (template: TemplateNode, parentFolder: FolderNode) => {
				initiateNewPage(template, parentFolder);
			}
		}
	};

	/**
	 * @type: {ModalSettings}
	 */
	const createNewFolderModal = {
		type: 'component',
		component: 'createNewFolderModal',
		meta: {
			modalTitle: 'Create new folder',
			onFormSubmit: (parentFolder: FolderNode, srcKey: string) => {
				initiateNewFolder(parentFolder, srcKey);
			}
		}
	};

	/**
	 * @type: {ModalSettings}
	 */
	$: saveNewPageModal = {
		type: 'component',
		component: 'saveNewPageModal',
		meta: {
			modalTitle: 'Save new page',
			parentFolder: $markdownStore.metadata ? $markdownStore.metadata.parent : 'sources/pages/',
			templateKey: $markdownStore.metadata ? $markdownStore.metadata.templateKey : 'page',
			pageTitle: $markdownStore.metadata ? $markdownStore.metadata.title : 'New Page',
			onFormSubmit: (slug: string) => {
				console.log('Saving page with slug: ' + slug);
				initiateSavePage();
			}
		}
	};

	/**
	 * @type: {ModalSettings}
	 */
	$: savePageModal = {
		type: 'confirm',
		title: 'Confirm save',
		body: "Save changes to page '<strong>" + pgTitle + "</strong>'?",
		buttonTextConfirm: 'Save',
		buttonTextCancel: 'Cancel',
		response: (r: boolean) => {
			if (r) {
				console.log('save clicked');
				initiateSavePage();
			} else {
				console.log('cancel clicked');
			}
			modalStore.close();
		}
	};

	onMount(async () => {
		if (!$pages) {
			await loadPagesAndFolders();
		}
	});

	async function loadPagesAndFolders() {
		if (!$userStore.token) {
			console.log('no token');
			return;
		} else {
			console.log('fetching pages...');
			const pgCount = await fetchPages($userStore.token);
			const folderCount = await fetchFolders($userStore.token);
			if (pgCount instanceof Error) {
				errorToast.message = 'Failed to load pages. Message was: ' + pgCount.message;
				toastStore.trigger(errorToast);
				console.error(pgCount);
			} else {
				toast.message = 'Loaded ' + pgCount + ' pages';
				toastStore.trigger(toast);
			}
			if (folderCount instanceof Error) {
				errorToast.message = 'Failed to load folders. Message was: ' + folderCount.message;
				toastStore.trigger(errorToast);
				console.error(folderCount);
			} else {
				toast.message = 'Loaded ' + folderCount + ' folders';
				toastStore.trigger(toast);
			}
		}
	}

	async function intiateLoadPage(srcKey: string) {
		// First check if the item is a page, and not a folder
		let folder = $folders?.folders.find((f) => f.srcKey === srcKey);
		if (folder) {
			return;
		}
		let loadResponse = fetchPage(srcKey, $userStore.token!!);
		loadResponse.then((r) => {
			if (r instanceof Error) {
				errorToast.message = 'Failed to load page';
				toastStore.trigger(errorToast);
			} else {
				toast.message = 'Loaded page ' + r;
				toastStore.trigger(toast);
				isNewPage = false;
			}
		});
	}

	async function initiateSavePage() {
		console.log('saving page');
		if ($markdownStore.metadata) {
			let saveResult = await savePage($markdownStore.metadata.srcKey, $userStore.token!!);
			if (saveResult instanceof Error) {
				errorToast.message = 'Failed to save page';
				toastStore.trigger(errorToast);
			} else {
				toast.message = 'Saved page ' + saveResult;
				isNewPage = false;
				toastStore.trigger(toast);
				loadPagesAndFolders();
			}
		}
	}

	function initiateNewPage(template: TemplateNode, folder: FolderNode) {
		let sectionsObject = template.sections.reduce((obj, item) => {
			// @ts-expect-error
			obj[item as string] = '';
			return obj;
		}, {});

		$markdownStore.metadata = new PageItem(
			'',
			//@ts-ignore
			null,
			template.srcKey,
			'',
			new Date(),
			new Map<string, string>(),
			sectionsObject,
			false,
			folder.srcKey,
			true
		);
		isNewPage = true;
	}

	function initiateNewFolder(parentFolder: FolderNode, srcKey: string) {
		let createResult = createFolder(parentFolder.srcKey + srcKey, $userStore.token!!);
		createResult.then((r) => {
			if (r instanceof Error) {
				errorToast.message = 'Failed to create folder';
				toastStore.trigger(errorToast);
			} else {
				toast.message = 'Created folder ' + createResult;
				toastStore.trigger(toast);
				loadPagesAndFolders();
			}
		});
	}

	const foldersUnsubscribe = folders.subscribe((value) => {
		const rootFolderKey = 'sources/pages/';

		if (value && value.count != -1) {
			// build TreeViewNodes from FolderNodes
			pgFolderNodes = [];
			expandedNodes = [rootFolderKey];
			value.folders[0].children = [];
			// TODO: the root folder '/sources/pages/' doesn't have a FolderNode, and so has no children. Put the root pages into this folder
			let rootPages = $pages?.pages.filter((p) => p.parent === rootFolderKey);
			if (rootPages) {
				for (const p of rootPages) {
					value.folders[0].children.push(p.srcKey);
				}
			}

			for (const folder of value.folders) {
				let childNodes = [] as TreeViewNode[];
				// start with the root folder
				// then the remainder of the folders
				if (folder.children.length > 0) {
					for (const child of folder.children) {
						// child is just the srcKey of the page
						// find it in the pages list
						let page = $pages?.pages.find((p) => p.srcKey === child);
						if (page) {
							if (page.isRoot) {
								childNodes.push({
									id: child,
									lead: IndexPageIconComponent,
									content: PostListItem,
									contentProps: { title: page.title, date: '', srcKey: page.srcKey }
								});
							} else {
								childNodes.push({
									id: child,
									lead: PageIconComponent,
									content: PostListItem,
									contentProps: { title: page.title, date: '', srcKey: page.srcKey }
								});
							}
						}
					}
				}
				pgFolderNodes.push({
					id: folder.srcKey,
					lead: FolderIconComponent,
					content: FolderListItem,
					children: childNodes,
					contentProps: { title: folder.url, count: folder.children.length, srcKey: folder.srcKey }
				});
			}
			pgFolderNodes = [...pgFolderNodes];
		}
	});

	const contentStoreUnsubscribe = markdownStore.subscribe((value) => {
		if (value) {
			if (value.metadata != null) {
				pgTitle = value.metadata.title;
			}
		}
	});
</script>

<div class="flex flex-row grow mt-2 container justify-center">
	<div class="basis-1/4 flex flex-col items-center mr-4">
		{#if $userStore.isLoggedIn()}
			<h3 class="h3 mb-2">Pages</h3>
			<div class="btn-group variant-filled">
				<button on:click={loadPagesAndFolders} title="Reload pages"><Icon icon={Refresh} /></button>
				<button on:click={(e) => modalStore.trigger(createNewFolderModal)} title="New Folder"
					><Icon icon={CreateNewFolder} /></button
				>
				<button on:click={(e) => modalStore.trigger(createNewPostModal)} title="New Page"
					><Icon icon={Add} />New Page</button
				>
			</div>
			<div class="flex flex-row m-4">
				{#if $pages?.count === undefined || $pages?.count === -1}
					<ListPlaceholder label="Loading pages and posts" rows={5} />
				{:else if $pages?.count === 0}
					<p class="text-error-500">No pages</p>
				{:else}
					<span class="text=sm text-secondary-500">{pgAndFoldersLabel}</span>
				{/if}
			</div>
			{#if $pages?.count > 0}
				<div class="card bg-primary-200 w-full">
					<NestedFileList nodes={pgFolderNodes} {expandedNodes} onClickFn={intiateLoadPage} />
				</div>
			{/if}
		{:else}
			<p class="text-error-500">Not logged in</p>
		{/if}
	</div>

	<div class="basis-3/4 container flex flex-col w-full">
		<h3 class="h3 text-center mb-2">{pgTitle} (isValid: {isValid})</h3>
		{#if $markdownStore.metadata}
			<div class="flex flex-row justify-end">
				<div class="btn-group variant-filled" role="group">
					<button
						class=" variant-filled-error"
						disabled={isNewPage}
						on:click={(e) => {
							// modalStore.trigger(deletePostModal);
						}}><Icon icon={Delete} />Delete</button
					>
					<button
						disabled={!isValid}
						class=" variant-filled-primary"
						on:click={(e) => {
							if (isNewPage) {
								modalStore.trigger(saveNewPageModal);
							} else {
								modalStore.trigger(savePageModal);
							}
						}}>Save<Icon icon={Save} /></button
					>
				</div>
			</div>
			<div class="grid grid-cols-6 gap-6">
				<div class="col-span-6 sm:col-span-6 lg:col-span-2">
					{#if isNewPage}
						<p><em>Slug will be set on first save</em></p>
					{:else}
						<TextInput
							label="Slug"
							name="slug"
							bind:value={$markdownStore.metadata.srcKey}
							required
							readonly
						/>
					{/if}
				</div>
				<div class="col-span-6 sm:col-span-3 lg:col-span-2">
					<TextInput
						bind:value={$markdownStore.metadata.templateKey}
						name="template"
						label="Template"
						required
						readonly
					/>
				</div>
				<div class="col-span-6 sm:col-span-3 lg:col-span-2">
					<TextInput
						bind:value={$markdownStore.metadata.parent}
						name="parent"
						label="Parent"
						required
						readonly
						iconRight={$markdownStore.metadata.isRoot ? homeIcon : undefined}
					/>
				</div>
				<div class="col-span-6">
					<TextInput
						bind:value={$markdownStore.metadata.title}
						required
						name="postTitle"
						label="Title"
					/>
				</div>

				<div class="col-span-6">
					{#if $markdownStore.metadata}
						<SectionTabs bind:sections={$markdownStore.metadata.sections} />
					{:else}
						<span class="text-error-500">No metadata found for page.</span>
					{/if}
				</div>
			</div>
			<div class="flex flex-row justify-end mt-2">
				<div class="btn-group variant-filled" role="group">
					<button
						disabled={!isValid}
						class=" variant-filled-primary"
						on:click={(e) => {
							if (isNewPage) {
								// modalStore.trigger(saveNewPostModal);
							} else {
								modalStore.trigger(savePageModal);
							}
						}}>Save<Icon icon={Save} /></button
					>
				</div>
			</div>
		{/if}
	</div>
</div>
