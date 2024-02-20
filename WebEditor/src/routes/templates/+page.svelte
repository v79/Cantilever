<script lang="ts">
	import {
		getModalStore,
		getToastStore,
		type ModalSettings,
		type ToastSettings,
		type TreeViewNode
	} from '@skeletonlabs/skeleton';

	import { userStore } from '$lib/stores/userStore.svelte';
	import { onMount, tick } from 'svelte';
	import {
		fetchTemplate,
		fetchTemplates,
		templates,
		saveTemplate,
		fetchTemplateUsage,
		deleteTemplate,
		regenerate
	} from '$lib/stores/templateStore.svelte';
	import { Refresh, Icon, Save, Delete, Sync, Add } from 'svelte-google-materialdesign-icons';
	import TemplateListItem from './TemplateListItem.svelte';
	import ListPlaceholder from '$lib/components/ListPlaceholder.svelte';
	import PostList from '$lib/components/BasicFileList.svelte';
	import { handlebars } from '$lib/stores/contentStore.svelte';
	import TextInput from '$lib/forms/textInput.svelte';
	import { TemplateNode, TemplateUsageDTO } from '$lib/models/templates.svelte';
	import { spinner } from '$lib/stores/spinnerStore.svelte';
	import { project } from '$lib/stores/projectStore.svelte';

	const modalStore = getModalStore();
	const toastStore = getToastStore();

	$: webPageTitle = $handlebars.title ? '- ' + $handlebars.title : 'Untitled';

	let templateListNodes = [] as TreeViewNode[]; // for the treeview component
	let pgTitle = 'Template Editor';
	let isNewTemplate = false;
	$: templateIsValid = $handlebars.title != null && $handlebars.body != null;
	let usageCount = 0;

	const errorToast: ToastSettings = {
		message: 'Failed to fetch templates',
		background: 'variant-filled-error'
	};

	const toast: ToastSettings = {
		message: 'Loaded templates',
		background: 'variant-filled-success',
		hideDismiss: true
	};

	/**
	 * @type: {ModalSettings}
	 */
	$: saveTemplateModal = {
		type: 'confirm',
		title: 'Confirm save',
		body: "Save changes to template '<strong>" + $handlebars.title + "</strong>'?",
		buttonTextConfirm: 'Save',
		buttonTextCancel: 'Cancel',
		// TRUE if confirm pressed, FALSE if cancel pressed
		response: (r: boolean) => {
			if (r) {
				initiateSaveTemplate();
			}
			modalStore.close();
		}
	};

	/**
	 * @type: {ModalSettings}
	 */
	$: saveNewTemplateModal = {
		type: 'component',
		component: 'saveNewTemplateModal',
		meta: {
			modalTitle: 'Save new template',
			templateTitle: $handlebars.title,
			onFormSubmit: () => {
				initiateSaveTemplate();
			}
		}
	};

	/**
	 * @type: {ModalSettings}
	 */
	$: deleteTemplateModal = {
		type: 'component',
		component: 'confirmDeleteModal',
		meta: {
			modalTitle: 'Confirm template deletion',
			itemKey: $handlebars.title,
			furtherInfo: 'This template is used by ' + usageCount + ' pages and posts',
			onFormSubmit: () => {
				initiateDeletePost();
			}
		}
	};

	onMount(async () => {
		if (templates.isEmpty()) {
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
			const result = await fetchTemplates(token, $project.domain);
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
		let loadResponse = fetchTemplate(srcKey, $userStore.token!!, $project.domain);
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

	async function initiateSaveTemplate() {
		if ($handlebars) {
			console.log('initiateSaveTemplate: ', $handlebars.srcKey);
			if (isNewTemplate) {
				$handlebars.srcKey = 'sources/templates/' + $handlebars.srcKey + '.html.hbs';
			}
			const result = await saveTemplate($userStore.token!!, $project.domain);
			if (result instanceof Error) {
				errorToast.message = 'Failed to save template. Message was: ' + result.message;
				toastStore.trigger(errorToast);
				console.error(result);
			} else {
				toast.message = result;
				toastStore.trigger(toast);
				isNewTemplate = false;
			}
		}
	}

	async function initiateDeletePost() {
		const result = await deleteTemplate($handlebars.srcKey, $userStore.token!!, $project.domain);
		if (result instanceof Error) {
			errorToast.message = 'Failed to delete template. Message was: ' + result.message;
			toastStore.trigger(errorToast);
			console.error(result);
		} else {
			toast.message = result;
			toastStore.trigger(toast);
			$handlebars = new TemplateNode('', new Date(), '', new Array<string>(), '');
		}
	}

	function createNewTemplate() {
		console.log('Creating new template');
		const rawHTML = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="author" content="{{author}}">
    <meta name="generator" content="cantilevers">
    <title>{{ project.projectName }} - {{ title }}</title>
    <link rel="stylesheet" href="/css/pico.min.css">
</head>
<body>
</body>
</html>
`;
		let newTemplate: TemplateNode = new TemplateNode(
			'',
			new Date(),
			'(new template)',
			new Array<string>(),
			rawHTML
		);
		handlebars.set(newTemplate);
		isNewTemplate = true;
	}

	async function getTemplateUsage(srcKey: string): Promise<TemplateUsageDTO | Error> {
		const usageResponse = await fetchTemplateUsage(srcKey, $userStore.token!!, $project.domain);
		if (usageResponse instanceof Error) {
			errorToast.message = 'Failed to fetch template usage';
			toastStore.trigger(errorToast);
			console.error(usageResponse);
		} else {
			usageCount = usageResponse.count;
			return usageResponse;
		}
		return new Error('Unknown error when fetching template usage');
	}

	function triggerDeleteModal() {
		if (usageCount === 0) {
			modalStore.trigger(deleteTemplateModal);
		} else {
			errorToast.message =
				'This template is used by ' + usageCount + ' pages and posts and cannot be deleted';
			toastStore.trigger(errorToast);
		}
	}

	async function initiateGeneration() {
		if ($handlebars) {
			spinner.show('Regenerating from template ' + $handlebars.srcKey);
			$spinner = { value: true, label: 'Regenerating from template ' + $handlebars.srcKey };
			const result = await regenerate($handlebars.srcKey, $userStore.token!!, $project.domain);
			if (result instanceof Error) {
				errorToast.message = 'Failed to generate content. Message was: ' + result.message;
				toastStore.trigger(errorToast);
				console.error(result);
			} else {
				toast.message = result;
				toastStore.trigger(toast);
			}
			spinner.hide();
		}
	}

	const templatesUnsubscribe = templates.subscribe((value) => {
		if (value && value.count != -1) {
			// build TreeViewNodes from TemplateNodes
			templateListNodes = [];
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

<svelte:head>
	<title>Cantilever: Templates {webPageTitle}</title>
</svelte:head>

<div class="flex flex-row grow mt-2 container justify-center">
	<div class="basis-1/4 flex flex-col items-center mr-4">
		{#if $userStore.isLoggedIn()}
			<h3 class="h3 mb-2">Templates</h3>
			<div class="btn-group variant-filled">
				<button class="variant-filled-secondary" on:click={reloadPostList} title="Reload templates"
					><Icon icon={Refresh} />Reload</button>
				<button
					class="variant-filled-primary"
					on:click={(e) => createNewTemplate()}
					title="Create new template"><Icon icon={Add} />New Template</button>
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
						disabled={isNewTemplate}
						title="Regenerate content using this template"
						on:click={(e) => {
							initiateGeneration();
						}}><Icon icon={Sync} />Regenerate</button>
					<button
						class=" variant-filled-error"
						disabled={isNewTemplate}
						title="Delete template"
						on:click={(e) => {
							triggerDeleteModal();
						}}><Icon icon={Delete} />Delete</button>
					<button
						disabled={!templateIsValid}
						class=" variant-filled-primary"
						title="Save template"
						on:click={(e) => {
							if (isNewTemplate) {
								modalStore.trigger(saveNewTemplateModal);
							} else {
								modalStore.trigger(saveTemplateModal);
							}
						}}>Save<Icon icon={Save} /></button>
				</div>
			</div>
			<div class="grid grid-cols-6 gap-6">
				<div class="col-span-6 sm:col-span-6 lg:col-span-2">
					<TextInput label="Title" name="TemplateTitle" bind:value={$handlebars.title} required />
				</div>
				<div class="col-span-1 sm:col-span-1 lg:col-span-1">
					{#await getTemplateUsage($handlebars.srcKey) then value}
						<TextInput label="Used by" name="TemplateUsage" value={value.count} readonly />
					{:catch error}??{/await}
				</div>
				<div class="col-span-6">
					<textarea bind:value={$handlebars.body} class="textarea" rows="20"></textarea>
				</div>
			</div>
			<div class="flex flex-row justify-end mt-2">
				<div class="btn-group variant-filled" role="group">
					<button
						disabled={!templateIsValid}
						class="variant-filled-primary"
						title="Save template"
						on:click={(e) => {
							if (isNewTemplate) {
								modalStore.trigger(saveNewTemplateModal);
							} else {
								modalStore.trigger(saveTemplateModal);
							}
						}}>Save<Icon icon={Save} /></button>
				</div>
			</div>
		{/if}
	</div>
</div>
