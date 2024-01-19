<script lang="ts">
	import type { SvelteComponent } from 'svelte';

	// Stores
	import { getModalStore } from '@skeletonlabs/skeleton';

	// Props
	/** Exposes parent props to this component. */
	export let parent: SvelteComponent;

    let inputString = '';
    $: disabled = inputString !== 'delete';

	const modalStore = getModalStore();

	// Base Classes
	const cBase = 'card p-4 w-modal shadow-xl space-y-4';
	const cHeader = 'text-2xl font-bold';
	const cForm = 'p-4 space-y-4 rounded-container-token';
</script>

<!-- @component This example creates a simple form modal. -->

{#if $modalStore[0]}
	<div class="modal-example-form {cBase}">
		<header class={cHeader}>{$modalStore[0].meta.modalTitle}</header>
		<article>Delete post '<strong>{$modalStore[0].meta.itemTitle ?? '(title missing)'}</strong>'?<br/> This cannot be undone.</article>
		<!-- Enable for debugging: -->
		<form class="modal-form {cForm}">
			<label class="label">
				<span>Type 'delete' to confirm.</span>
				<input class="input text-warning-500" type="text" bind:value={inputString} placeholder="" />
			</label>
		
		</form>
		<!-- prettier-ignore -->
		<footer class="modal-footer {parent.regionFooter}">
			<button class="btn {parent.buttonNeutral}" on:click={parent.onClose}>{parent.buttonTextCancel}</button>
			<button  {disabled} class="btn variant-filled-error" on:click={$modalStore[0].meta.onFormSubmit}>Delete</button>
		</footer>
	</div>
{/if}