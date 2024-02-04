<script lang="ts">
	import { getModalStore } from '@skeletonlabs/skeleton';
	import type { SvelteComponent } from 'svelte';

	export let parent: SvelteComponent;
	const modalStore = getModalStore();

	let disabled = false;

	// Base Classes
	const cBase = 'card p-4 w-modal shadow-xl space-y-4';
	const cHeader = 'text-2xl font-bold';
	const cForm = 'p-4 space-y-4 rounded-container-token';

	function closeAndSubmit() {
		$modalStore[0].meta.onFormSubmit();
		modalStore.close();
	}
</script>

{#if $modalStore[0]}
	<div class="modal-example-form {cBase}">
		<header class={cHeader}>Replace current index page</header>
		<article>
			<p>
				Set page <strong>{$modalStore[0].meta.currentPage.title}</strong> as the new index page for
				folder <strong>{$modalStore[0].meta.currentPage.parent}</strong>?
			</p>
			<p class="mt-4">
				The current index page is <strong>{$modalStore[0].meta.currentIndexPage}</strong>.
			</p>
			<p class="mt-4">
				<em
					>This action will mean that '{$modalStore[0].meta.currentPage.srcKey}' will be generated
					as 'index.html' for this folder. A full regeneration of content is required for this to
					take effect.</em>
			</p>
		</article>
		<!-- prettier-ignore -->
		<footer class="modal-footer {parent.regionFooter}">
			<button class="btn {parent.buttonNeutral}" on:click={parent.onClose}>{parent.buttonTextCancel}</button>
			<button disabled={disabled} class="btn variant-filled-primary" on:click={closeAndSubmit}>Reassign</button>
		</footer>
	</div>
{/if}
