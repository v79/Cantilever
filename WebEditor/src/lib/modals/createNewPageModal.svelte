<script lang="ts">
	import { fetchTemplates, templates } from '$lib/stores/templateStore.svelte';
	import { userStore } from '$lib/stores/userStore.svelte';
	import { getModalStore, getToastStore, type ToastSettings } from '@skeletonlabs/skeleton';
	import { onMount, type SvelteComponent } from 'svelte';

	export let parent: SvelteComponent;
	const modalStore = getModalStore();
	const toastStore = getToastStore();

	const errorToast: ToastSettings = {
		message: 'Failed to load posts',
		background: 'variant-filled-error'
	};

	// Base Classes
	const cBase = 'card p-4 w-modal shadow-xl space-y-4';
	const cHeader = 'text-2xl font-bold';
	const cForm = 'p-4 space-y-4 rounded-container-token';

	let selectedTemplateKey: string | undefined;

	$: selectedTemplate = $templates?.templates.find((t) => t.srcKey === selectedTemplateKey);

	onMount(async () => {
		if (!$templates || $templates.count === undefined || $templates.count === 0) {
			console.log('No templates found');
			const result = await fetchTemplates($userStore.token!!);
			if (result instanceof Error) {
				errorToast.message = 'Failed to fetch templates. Message was: ' + result.message;
				toastStore.trigger(errorToast);
				console.error(result);
			} else {
				// templates loaded into store
			}
		} else {
			// templates already loaded
		}
	});

    function closeAndSubmit() {
        if (selectedTemplate) {
            $modalStore[0].meta.onFormSubmit(selectedTemplate);
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
			<label for="selectTemplate" class="label">Choose the template for the new page:</label>
			{#if $templates && $templates.templates}
				<select
					class="select"
					id="selectTemplate"
					name="selectTemplate"
					bind:value={selectedTemplateKey}
				>
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
						Selected template: {selectedTemplate.title} has {selectedTemplate.sections.length} section(s)
					</p>
				{/if}
			{/if}
		</article>
		<!-- prettier-ignore -->
		<footer class="modal-footer {parent.regionFooter}">
			<button class="btn {parent.buttonNeutral}" on:click={parent.onClose}>{parent.buttonTextCancel}</button>
			<button class="btn variant-filled-primary" on:click={closeAndSubmit}>Create</button>
		</footer>
	</div>
{/if}
