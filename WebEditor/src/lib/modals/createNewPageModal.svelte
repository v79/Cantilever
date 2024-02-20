<script lang="ts">
	import { fetchTemplates, templates } from '$lib/stores/templateStore.svelte';
	import { userStore } from '$lib/stores/userStore.svelte';
	import { getModalStore, getToastStore, type ToastSettings } from '@skeletonlabs/skeleton';
	import { onMount, type SvelteComponent } from 'svelte';
	import { folders } from '$lib/stores/pageStore.svelte';
	import { project } from '$lib/stores/projectStore.svelte';

	export let parent: SvelteComponent;
	const modalStore = getModalStore();
	const toastStore = getToastStore();

	const errorToast: ToastSettings = {
		message: 'Failed to load pages',
		background: 'variant-filled-error'
	};

	// Base Classes
	const cBase = 'card p-4 w-modal shadow-xl space-y-4';
	const cHeader = 'text-2xl font-bold';
	const cForm = 'p-4 space-y-4 rounded-container-token';

	$: templatesLoading = true;
	$: templatesReady = false;

	let selectedTemplateKey: string | undefined;
	let selectedFolderKey: string | undefined;

	$: selectedTemplate = $templates?.templates.find((t) => t.srcKey === selectedTemplateKey);
	$: selectedFolder = $folders?.folders.find((f) => f.srcKey === selectedFolderKey);
	$: foldersLoaded = $folders && $folders.count > 0;

	onMount(async () => {
		if (templates.isEmpty()) {
			const result = await fetchTemplates($userStore.token!!, $project.domain);
			if (result instanceof Error) {
				errorToast.message = 'Failed to fetch templates. Message was: ' + result.message;
				toastStore.trigger(errorToast);
				templatesReady = false;
				templatesLoading = false;
			} else {
				// templates loaded into store
				templatesReady = true;
				templatesLoading = false;
			}
		} else {
			// templates already loaded
		}
	});

	function closeAndSubmit() {
		if (selectedTemplate) {
			$modalStore[0].meta.onFormSubmit(selectedTemplate, selectedFolder);
			modalStore.close();
		}
	}

	const templateStoreUnsubscribe = templates.subscribe((value) => {
		if (value) {
			// do nothing
		}
	});
</script>

{#if $modalStore[0]}
	<div class="modal-example-form {cBase}">
		<header class={cHeader}>
			{$modalStore[0].meta.modalTitle}
		</header>
		<article>
			{#if templatesLoading}
				<div class="placeholder h-4">Loading templates...</div>
			{:else if templatesReady}
				<label for="selectTemplate" class="label">Choose the template for the new page:</label>
				{#if $templates && $templates.templates}
					<select
						class="select m-4 w-9/12"
						id="selectTemplate"
						name="selectTemplate"
						bind:value={selectedTemplateKey}>
						{#each $templates.templates as template}
							{#if $modalStore[0].meta.showOnlyWithSections}
								{#if template.sections.length > 0}
									<option value={template.srcKey}>{template.title}</option>
								{/if}
							{:else}
								<option value={template.srcKey}>{template.title}</option>
							{/if}
						{/each}
					</select>
					{#if selectedTemplate}
						<p>
							<em>
								Selected template: {selectedTemplate.title} has {selectedTemplate.sections.length} section(s)</em>
						</p>
					{/if}
				{/if}
			{:else}
				<p>
					No template found. Create a template first - it is recommended that you have an "index"
					template at the very least, for non-post items.
				</p>
			{/if}
			{#if !foldersLoaded || !templatesReady}
				<div class="placeholder h-4">Loading folders...</div>
			{:else}
				<label for="selectParent" class="label">Choose the parent folder for the new page:</label>
				{#if $folders && $folders.folders}
					<select
						class="select m-4 w-9/12"
						id="selectParent"
						name="selectParent"
						bind:value={selectedFolderKey}>
						{#each $folders.folders as folder}
							<option value={folder.srcKey}>{folder.srcKey}</option>
						{/each}
					</select>
				{/if}
			{/if}
		</article>
		<!-- prettier-ignore -->
		<footer class="modal-footer {parent.regionFooter}">
			<button class="btn {parent.buttonNeutral}" on:click={parent.onClose}>{parent.buttonTextCancel}</button>
			<button class="btn variant-filled-primary" disabled={!templatesReady} on:click={closeAndSubmit}>Create</button>
		</footer>
	</div>
{/if}
