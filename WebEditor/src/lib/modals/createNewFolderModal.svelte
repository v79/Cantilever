<script lang="ts">
	import { getModalStore, getToastStore, type ToastSettings } from '@skeletonlabs/skeleton';
	import type { SvelteComponent } from 'svelte';
	import { folders } from '../../routes/pages/pageStore.svelte';
	import { userStore } from '$lib/stores/userStore.svelte';
	import TextInput from '$lib/forms/textInput.svelte';
	import { validateSlug } from '$lib/functions/validateSlug';

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

	let selectedFolderKey: string | undefined;
	let newFolderKey: string = '';
	$: selectedFolder = $folders?.folders.find((f) => f.srcKey === selectedFolderKey);
	$: foldersLoaded = $folders && $folders.count > 0;
	$: disabled = newFolderKey === '' || !validateSlug(newFolderKey);

	function closeAndSubmit() {
		if (selectedFolder) {
			$modalStore[0].meta.onFormSubmit(selectedFolder, newFolderKey);
			modalStore.close();
		}
	}
</script>

{#if $modalStore[0]}
	<div class="modal-example-form {cBase}">
		<header class={cHeader}>
			{$modalStore[0].meta.modalTitle}
		</header>
		<article>
			{#if !foldersLoaded}
				<div class="placeholder h-4">Loading folders...</div>
			{:else}
				<label for="selectParent" class="label">Choose the parent folder for the new folder:</label>
				{#if $folders && $folders.folders}
					<select
						class="select m-4 w-9/12"
						id="selectParent"
						name="selectParent"
						bind:value={selectedFolderKey}
					>
						{#each $folders.folders as folder}
							<option value={folder.srcKey}>{folder.srcKey}</option>
						{/each}
					</select>

					<TextInput
						classes="m-4 w-9/12"
						name="newFolderKey"
						required
						bind:value={newFolderKey}
						label="New folder name:"
					/>
				{/if}
			{/if}
		</article>
		<!-- prettier-ignore -->
		<footer class="modal-footer {parent.regionFooter}">
			<button class="btn {parent.buttonNeutral}" on:click={parent.onClose}>{parent.buttonTextCancel}</button>
			<button disabled={disabled} class="btn variant-filled-primary" on:click={closeAndSubmit}>Create</button>
		</footer>
	</div>
{/if}
