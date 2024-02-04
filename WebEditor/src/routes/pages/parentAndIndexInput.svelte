<script lang="ts">
	import TextInput from '$lib/forms/textInput.svelte';
	import type { iconConfigType } from '$lib/forms/textInputIconType';
	import { markdownStore } from '$lib/stores/contentStore.svelte';
	import { Home } from 'svelte-google-materialdesign-icons';
	import { folders, pages } from './pageStore.svelte';
	import { getModalStore, type ModalSettings } from '@skeletonlabs/skeleton';
	import type { FolderNode, PageNode } from '$lib/models/pages.svelte';

	const modalStore = getModalStore();

	export let value = '';
	export let isRoot = false;

	$: currentIndexPage = findIndexPageForFolder($markdownStore?.metadata?.parent);

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
				console.log(
					'initiate index updating from ' +
						currentIndexPage?.srcKey +
						' to ' +
						$markdownStore.metadata?.srcKey
				);
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
