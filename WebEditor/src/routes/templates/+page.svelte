<script lang="ts">
	import {
		getModalStore,
		getToastStore,
		type ModalSettings,
		type ToastSettings,
		type TreeViewNode
	} from '@skeletonlabs/skeleton';

	import { userStore } from '../../stores/userStore.svelte';
	import { onMount, tick } from 'svelte';
	import { fetchTemplate, fetchTemplates, templates } from './templateStore.svelte';
	import { Refresh, Icon, Save, Delete, Sync } from 'svelte-google-materialdesign-icons';
	import TemplateListItem from './TemplateListItem.svelte';
	import ListPlaceholder from '../../components/ListPlaceholder.svelte';
	import PostList from '../../components/BasicFileList.svelte';
	import { handlebars } from '../../stores/contentStore.svelte';
	import TextInput from '../../components/forms/textInput.svelte';

	const modalStore = getModalStore();
	const toastStore = getToastStore();

	let templateListNodes = [] as TreeViewNode[]; // for the treeview component
	let pgTitle = 'Template Editor';
	let isNewTemplate = false;
	let templateIsValid = false;

	const errorToast: ToastSettings = {
		message: 'Failed to fetch templates',
		background: 'variant-filled-error'
	};

	const toast: ToastSettings = {
		message: 'Loaded templates',
		background: 'variant-filled-success',
		hideDismiss: true
	};

	onMount(async () => {
		if (!$templates) {
			await loadTemplateList();
		}
	});

	async function reloadPostList() {
		templateListNodes = [];
		templates.set({ count: -1, templates: [] });
		tick();
		await loadTemplateList();
	}

	async function loadTemplateList() {
		if (!$userStore.token) {
			console.log('no token');
			return;
		}
		const token = $userStore.token;
		if (token) {
			const result = await fetchTemplates(token);
			if (result instanceof Error) {
				errorToast.message = 'Failed to fetch templates. Message was: ' + result.message;
				toastStore.trigger(errorToast);
				console.error(result);
			} else {
				toast.message = 'Loaded ' + result + ' templates';
				toastStore.trigger(toast);
			}
		}
	}

	async function initiateLoadTemplate(srcKey: string) {
		let loadResponse = fetchTemplate(srcKey, $userStore.token!!);
		loadResponse.then((r) => {
			if (r instanceof Error) {
				errorToast.message = 'Failed to load template';
				toastStore.trigger(errorToast);
			} else {
				toast.message = r;
				toastStore.trigger(toast);
				isNewTemplate = false;
			}
		});
	}

	const templatesUnsubscribe = templates.subscribe((value) => {
		if (value && value.count != -1) {
			// build TreeViewNodes from TemplateNodes
			for (const template of value.templates) {
				templateListNodes.push({
					id: template.srcKey,
					content: TemplateListItem,
					contentProps: { name: template.title, srcKey: template.srcKey }
				});
			}
		}
		templateListNodes = [...templateListNodes];
	});

	const hanldebarsUnsubscribe = handlebars.subscribe((value) => {
		if (value) {
			if (value.title != null) {
				pgTitle = value.title;
			}
		}
	});
</script>

<div class="flex flex-row grow mt-2 container justify-center">
	<div class="basis-1/4 flex flex-col items-center mr-4">
		{#if $userStore.name}
			<h3 class="h3">Templates</h3>
			<div class="btn-group variant-filled">
				<button on:click={reloadPostList}><Icon icon={Refresh} />Reload</button>
			</div>
			<div class="flex flex-row m-4">
				{#if $templates?.count === undefined || $templates?.count === -1}
					<ListPlaceholder label="Loading templates" rows={5} />
				{:else if $templates?.count === 0}
					<p>No templates</p>
				{:else}
					<span class="text=sm text-secondary-500">{$templates?.count} templates</span>
				{/if}
			</div>
			{#if $templates?.count > 0}
				<div class="card bg-primary-200 w-full">
					<PostList nodes={templateListNodes} onClickFn={initiateLoadTemplate} />
				</div>
			{/if}
		{:else}
			<p class="text-error-500">Not logged in</p>
		{/if}
	</div>

	<div class="basis-3/4 container flex flex-col w-full">
		<h3 class="h3 text-center mb-2">{pgTitle}</h3>
		{#if $handlebars.title}
			<div class="flex flex-row justify-end">
				<div class="btn-group variant-filled" role="group">
					<button
						class="variant-filled-secondary"
						on:click={(e) => {
							// regenerateFromTemplate()
						}}><Icon icon={Sync} />Regenerate</button
					>
					<button
						class=" variant-filled-error"
						disabled={isNewTemplate}
						on:click={(e) => {
							// modalStore.trigger(deleteTemplateModal);
						}}><Icon icon={Delete} />Delete</button
					>
					<button
						disabled={!templateIsValid}
						class=" variant-filled-primary"
						on:click={(e) => {
							if (isNewTemplate) {
								// modalStore.trigger(saveNewTemplateModal);
							} else {
								// modalStore.trigger(savePostModal);
							}
						}}>Save<Icon icon={Save} /></button
					>
				</div>
			</div>
			<div class="grid grid-cols-6 gap-6">
				<div class="col-span-6 sm:col-span-6 lg:col-span-2">
					<TextInput label="Title" name="TemplateTitle" bind:value={$handlebars.title} required />
				</div>
				<div class="col-span-6">
					<textarea bind:value={$handlebars.body} class="textarea" rows="20"></textarea>
				</div>
			</div>
			<div class="flex flex-row justify-end mt-2">
				<div class="btn-group variant-filled" role="group">
					<button
						disabled={!templateIsValid}
						class=" variant-filled-primary"
						on:click={(e) => {
							if (isNewTemplate) {
								// modalStore.trigger(saveNewTemplateModal);
							} else {
								// modalStore.trigger(savePostModal);
							}
						}}>Save<Icon icon={Save} /></button
					>
				</div>
			</div>
		{/if}
	</div>
</div>
