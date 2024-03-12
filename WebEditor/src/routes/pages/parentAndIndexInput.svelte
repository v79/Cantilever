<script lang="ts">
	import TextInput from '$lib/forms/textInput.svelte';
	import type { iconConfigType } from '$lib/forms/textInputIconType';
	import type { FolderNode, PageNode } from '$lib/models/pages.svelte';
	import { markdownStore } from '$lib/stores/contentStore.svelte';
	import { userStore } from '$lib/stores/userStore.svelte';
	import {
		getModalStore,
		getToastStore,
		type ModalSettings,
		type ToastSettings
	} from '@skeletonlabs/skeleton';
	import { Home } from 'svelte-google-materialdesign-icons';
	import { folders, pages, switchIndexPage } from '../../lib/stores/pageStore.svelte';
	import { project } from '$lib/stores/projectStore.svelte';

	const modalStore = getModalStore();
	const toastStore = getToastStore();

	export let value = '';
	export let isRoot = false;

	$: currentIndexPage = findIndexPageForFolder($markdownStore?.metadata?.parent);

	const toast: ToastSettings = {
		message: 'Updated index page',
		background: 'variant-filled-success',
		hideDismiss: true
	};
	const errorToast: ToastSettings = {
		message: 'Failed to update index page',
		background: 'variant-filled-error'
	};

	/**
	 * @type {ModalSettings}
	 */
	$: switchIndexPageModal = {
		type: 'component',
		component: 'switchIndexPageModal',
		meta: {
			currentPage: $markdownStore.metadata,
			currentIndexPage: currentIndexPage?.title ?? 'index.md',
			onFormSubmit: () => {
				initateIndexUpdate();
			}
		}
	};

	/**
	 * @type {iconConfigType}
	 */
	let inactiveHomeIcon = {
		icon: Home,
		variation: 'outlined',
		onClick: (e: Event) => {
			modalStore.trigger(switchIndexPageModal);
		}
	};

	/**
	 * @type {iconConfigType}
	 */
	let activeHomeIcon = {
		icon: Home,
		variation: 'filled',
		onClick: (e: Event) => {
			// do nothing
		}
	};

	async function initateIndexUpdate() {
		console.log(
			'initateIndexUpdate. Setting the index page for folder ' +
				$markdownStore.metadata?.parent +
				' to ' +
				$markdownStore.metadata?.srcKey +
				' (was ' +
				currentIndexPage?.srcKey +
				')'
		);
		if (currentIndexPage && $markdownStore.metadata && $userStore.token) {
			const response = switchIndexPage(
				currentIndexPage.srcKey,
				$markdownStore.metadata?.srcKey,
				$markdownStore.metadata?.parent,
				$userStore.token,
				$project.domain
			);
			response.then((result) => {
				if (result instanceof Error) {
					errorToast.message = 'Failed to update index page: ' + result.message;
					toastStore.trigger(errorToast);
				} else {
					$markdownStore.metadata.isRoot = true;
					toast.message = result;
					toastStore.trigger(toast);
				}
			});
		}
	}

	function findIndexPageForFolder(folder: string | undefined): PageNode | undefined {
		if ($pages) {
			let folderNode = $folders.folders.find((f: FolderNode) => f.srcKey === folder + '/');
			if (folderNode?.indexPage) {
				return $pages.pages.find((p: PageNode) => p.srcKey === folderNode?.indexPage);
			}
		}
	}

	function handleMessage(e: CustomEvent) {
		switch (e.detail) {
			case 'right':
				isRoot ? activeHomeIcon.onClick(e) : inactiveHomeIcon.onClick(e);
				break;
			default:
				console.log('Unhandled message from textInput', e);
		}
	}
</script>

<!-- annoying that I can't move the isRoot check into the iconRight prop -->
{#if isRoot}
	<TextInput
		bind:value
		name="parent"
		label="Parent"
		required
		readonly
		iconRight={activeHomeIcon}
		on:message={handleMessage} />
{:else}
	<TextInput
		bind:value
		name="parent"
		label="Parent"
		required
		readonly
		iconRight={inactiveHomeIcon}
		on:message={handleMessage} />
{/if}
