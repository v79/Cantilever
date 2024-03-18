<script lang="ts">
	import PostListItem from '$lib/components/FileListItem.svelte';
	import ListPlaceholder from '$lib/components/ListPlaceholder.svelte';
	import NestedFileList from '$lib/components/NestedFileList.svelte';
	import TextInput from '$lib/forms/textInput.svelte';
	import { PageItem } from '$lib/models/markdown';
	import { FolderNode } from '$lib/models/pages.svelte';
	import { TemplateNode } from '$lib/models/templates.svelte';
	import { CLEAR_MARKDOWN, markdownStore } from '$lib/stores/contentStore.svelte';
	import { project } from '$lib/stores/projectStore.svelte';
	import { userStore } from '$lib/stores/userStore.svelte';
	import {
		getModalStore,
		getToastStore,
		type ToastSettings,
		type TreeViewNode
	} from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { Add, Delete, Icon, Refresh, Save } from 'svelte-google-materialdesign-icons';
	import CreateNewFolder from 'svelte-google-materialdesign-icons/Create_new_folder.svelte';
	import {
		createFolder,
		deleteFolder,
		deletePage,
		fetchFolders,
		fetchPage,
		fetchPages,
		folders,
		pages,
		savePage
	} from '$lib/stores/pageStore.svelte';
	import FolderIconComponent from './FolderIconComponent.svelte';
	import FolderListItem from './FolderListItem.svelte';
	import IndexPageIconComponent from './IndexPageIconComponent.svelte';
	import PageIconComponent from './PageIconComponent.svelte';
	import SectionTabs from './SectionTabs.svelte';
	import ParentAndIndexInput from './parentAndIndexInput.svelte';

	const modalStore = getModalStore();
	const toastStore = getToastStore();

	$: webPageTitle = $markdownStore.metadata?.title ? ' - ' + $markdownStore.metadata?.title : '';

	let pgFolderNodes = [] as TreeViewNode[]; // for the treeview component#
	let expandedNodes = [] as string[];
	let pgTitle: string;
	let isNewPage = false;

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

	/**
	 * @type: {ModalSettings}
	 */
	const createNewPageModal = {
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
	$: deletePageModal = {
		type: 'component',
		component: 'confirmDeleteModal',
		meta: {
			modalTitle: 'Delete page',
			itemKey: $markdownStore.metadata?.srcKey ?? 'unknown',
			onFormSubmit: () => {
				initiateDeletePage();
			}
		}
	};

	/**
	 * @type: {ModalSettings}
	 */
	$: deleteFolderModal = {
		type: 'component',
		component: 'confirmDeleteModal',
		meta: {
			modalTitle: 'Delete folder',
			itemKey: $markdownStore.metadata?.srcKey ?? 'unknown',
			onFormSubmit: (srcKey: string) => {
				initiateDeleteFolder(srcKey);
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
			}
			modalStore.close();
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
			pageTitle: $markdownStore.metadata?.title ?? 'unknown',
			templateKey: $markdownStore.metadata?.templateKey ?? 'unknown',
			parentFolder: $markdownStore.metadata?.parent ?? 'unknown',
			onFormSubmit: (newPageSlug: string) => {
				$markdownStore.metadata!!.srcKey = $markdownStore.metadata?.parent + newPageSlug + '.md';
				initiateSavePage();
			}
		}
	};

	onMount(async () => {
		if (pages.isEmpty()) {
			await loadPagesAndFolders();
		}
	});

	async function loadPagesAndFolders() {
		if (!$userStore.token) {
			console.log('no token');
			return;
		} else {
			console.log('fetching pages...');
			const pgCount = await fetchPages($userStore.token, $project.domain);
			const folderCount = await fetchFolders($userStore.token, $project.domain);
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
		let loadResponse = fetchPage(srcKey, $userStore.token!!, $project.domain);
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
			let saveResult = savePage(
				$markdownStore.metadata.srcKey,
				$userStore.token!!,
				$project.domain
			);
			saveResult.then((r) => {
				if (r instanceof Error) {
					errorToast.message = 'Failed to save page';
					toastStore.trigger(errorToast);
				} else {
					toast.message = 'Saved page ' + r;
					toastStore.trigger(toast);
					loadPagesAndFolders();
					isNewPage = false;
				}
			});
		}
	}

	async function initiateDeletePage() {
		console.log('Deleting page');
		if ($markdownStore.metadata) {
			let deleteResult = deletePage(
				$markdownStore.metadata.srcKey,
				$userStore.token!!,
				$project.domain
			);
			deleteResult.then((r) => {
				if (r instanceof Error) {
					errorToast.message = 'Failed to delete page';
					toastStore.trigger(errorToast);
				} else {
					toast.message = 'Deleted page ' + r;
					toastStore.trigger(toast);
					markdownStore.set(CLEAR_MARKDOWN);
					loadPagesAndFolders();
				}
			});
		}
	}

	async function initiateDeleteFolder(srcKey: string) {
		console.log('Deleting folder: ' + srcKey);
		let deleteResult = deleteFolder(srcKey, $userStore.token!!, $project.domain);
		deleteResult.then((r) => {
			if (r instanceof Error) {
				errorToast.message = 'Failed to delete folder';
				toastStore.trigger(errorToast);
			} else {
				toast.message = 'Deleted folder ' + r;
				toastStore.trigger(toast);
				loadPagesAndFolders();
			}
		});
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
		let createResult = createFolder(
			parentFolder.srcKey + srcKey,
			$userStore.token!!,
			$project.domain
		);
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

	function showFolderDelete(srcKey: string) {
		console.log('showFolderDelete: ' + srcKey);
		console.log($folders.folders.find((f) => f.srcKey === srcKey)?.children.length);
		if (folders && $folders.folders.find((f) => f.srcKey === srcKey)?.children.length != 0) {
			toastStore.trigger({
				message: 'Folder is not empty. Delete the pages in the folder first.',
				background: 'variant-filled-error'
			});
			return;
		}
		deleteFolderModal.meta.itemKey = srcKey;
		modalStore.trigger(deleteFolderModal);
	}

	const foldersUnsubscribe = folders.subscribe((value) => {
		var rootFolderKey = '';
		if($project.domain) {
			rootFolderKey = $project.domain + '/sources/pages/';
		}
		// console.dir(rootFolderKey);

		if (value && value.count != -1) {
			// build TreeViewNodes from FolderNodes
			pgFolderNodes = [];
			expandedNodes = [rootFolderKey];
			for (const folder of value.folders) {
				let childNodes = [] as TreeViewNode[];
				// console.log('Iterating through folder ' + folder.srcKey);
				if (folder.children.length > 0) {
					for (const child of folder.children) {
						// child is just the srcKey of the page
						// find it in the pages list
						let page = $pages?.pages.find((p) => p.srcKey === child);
						if (page) {
							let displaySrcKey = page.srcKey.slice($project.domain ? $project.domain.length + 1 : 0);
							if (page.isRoot) {
								childNodes.push({
									id: child,
									lead: IndexPageIconComponent,
									content: PostListItem,
									contentProps: { title: page.title, date: '', srcKey: displaySrcKey }
								});
							} else {
								childNodes.push({
									id: child,
									lead: PageIconComponent,
									content: PostListItem,
									contentProps: { title: page.title, date: '', srcKey: displaySrcKey }
								});
							}
						}
					}
				}
				let displayTitle = folder.url?.slice($project.domain ? $project.domain.length + 1 : 0) ?? 'unknown';
				pgFolderNodes.push({
					id: folder.srcKey,
					lead: FolderIconComponent,
					content: FolderListItem,
					children: childNodes,
					contentProps: {
						title: displayTitle,
						count: folder.children.length,
						srcKey: folder.srcKey,
						onDelete: showFolderDelete
					}
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

<svelte:head>
	<title>Cantilever: Pages {webPageTitle}</title>
</svelte:head>

<div class="flex flex-row grow mt-2 container justify-center">
	<div class="basis-1/4 flex flex-col items-center mr-4">
		{#if $userStore.isLoggedIn()}
			<h3 class="h3 mb-2">Pages</h3>
			<div class="btn-group variant-filled">
				<button class="variant-filled-secondary" on:click={loadPagesAndFolders} title="Reload pages"
					><Icon icon={Refresh} /></button>
				<button
					class="variant-filled-secondary"
					on:click={(e) => modalStore.trigger(createNewFolderModal)}
					title="New Folder"><Icon icon={CreateNewFolder} /></button>
				<button
					class="variant-filled-primary"
					on:click={(e) => modalStore.trigger(createNewPageModal)}
					title="New Page"><Icon icon={Add} />New Page</button>
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
		<h3 class="h3 text-center mb-2">
			{#if pgTitle}{pgTitle}{/if}
		</h3>
		{#if $markdownStore.metadata}
			<div class="flex flex-row justify-end">
				<div class="btn-group variant-filled" role="group">
					<button
						class=" variant-filled-error"
						disabled={isNewPage}
						title="Delete page"
						on:click={(e) => {
							modalStore.trigger(deletePageModal);
						}}><Icon icon={Delete} />Delete</button>
					<button
						disabled={!isValid}
						class=" variant-filled-primary"
						title="Save and regenerate page"
						on:click={(e) => {
							if (isNewPage) {
								modalStore.trigger(saveNewPageModal);
							} else {
								modalStore.trigger(savePageModal);
							}
						}}>Save<Icon icon={Save} /></button>
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
							readonly />
					{/if}
				</div>
				<div class="col-span-6 sm:col-span-3 lg:col-span-2">
					<TextInput
						bind:value={$markdownStore.metadata.templateKey}
						name="template"
						label="Template"
						required
						readonly />
				</div>
				<div class="col-span-6 sm:col-span-3 lg:col-span-2">
					<ParentAndIndexInput
						value={$markdownStore.metadata.parent}
						bind:isRoot={$markdownStore.metadata.isRoot} />
				</div>
				<div class="col-span-6">
					<TextInput
						bind:value={$markdownStore.metadata.title}
						required
						name="postTitle"
						label="Title" />
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
						title="Save and regenerate page"
						on:click={(e) => {
							if (isNewPage) {
								modalStore.trigger(saveNewPageModal);
							} else {
								modalStore.trigger(savePageModal);
							}
						}}>Save<Icon icon={Save} /></button>
				</div>
			</div>
		{/if}
	</div>
</div>
