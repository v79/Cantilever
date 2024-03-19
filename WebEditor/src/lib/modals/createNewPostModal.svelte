<script lang="ts">
	import { project } from '$lib/stores/projectStore.svelte';
	import { fetchTemplates, templates } from '$lib/stores/templateStore.svelte';
	import { userStore } from '$lib/stores/userStore.svelte';
	import { getModalStore, getToastStore, type ToastSettings } from '@skeletonlabs/skeleton';
	import { onMount, type SvelteComponent } from 'svelte';

	export let parent: SvelteComponent;
	const modalStore = getModalStore();
	const toastStore = getToastStore();

	const errorToast: ToastSettings = {
		message: 'Failed to templates',
		background: 'variant-filled-error'
	};

	$: templatesLoading = true;
	$: templatesReady = false;

	// Base Classes
	const cBase = 'card p-4 w-modal shadow-xl space-y-4';
	const cHeader = 'text-2xl font-bold';
	const cForm = 'p-4 space-y-4 rounded-container-token';

	onMount(async () => {
		if (templates.isEmpty()) {
			console.log('No templates found');
			const result = await fetchTemplates($userStore.token!!, $project.domain);
			if (result instanceof Error) {
				errorToast.message = 'Could not fetch templates. Create a "post" template first';
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
			templatesReady = true;
			templatesLoading = false;
		}
	});

	function closeAndSubmit() {
		$modalStore[0].meta.onFormSubmit();
		modalStore.close();
	}
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
				<p>Create a new post?</p>
			{:else}
				<p>No template called "post" found. Create a template called "post" first.</p>
			{/if}
		</article>
		<footer class="modal-footer {parent.regionFooter}">
			<button class="btn {parent.buttonNeutral}" on:click={parent.onClose}
				>{parent.buttonTextCancel}</button>
			<button
				disabled={!templatesReady}
				class="btn variant-filled-primary"
				on:click={closeAndSubmit}>Create</button>
		</footer>
	</div>
{/if}
