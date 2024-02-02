<script lang="ts">
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { onMount, type SvelteComponent } from 'svelte';
	import { createSlug } from '$lib/stores/contentStore.svelte';
	import { markdownStore } from '$lib/stores/contentStore.svelte';

	import TextInput from '$lib/forms/textInput.svelte';
	export let parent: SvelteComponent;
	const modalStore = getModalStore();
	const validChars: RegExp = /^[\a-zA-Z0-9]+(?:-[\w]+)*$/;

	$: newPageSlug = '';
	$: disabled = newPageSlug === '' || !validChars.test(newPageSlug);

	// Base Classes
	const cBase = 'card p-4 w-modal shadow-xl space-y-4';
	const cHeader = 'text-2xl font-bold';
	const cForm = 'p-4 space-y-4 rounded-container-token';

	onMount(() => {
		newPageSlug = createSlug($modalStore[0].meta.pageTitle);
	});

	function onSlugChange(e: Event) {
		newPageSlug = (e.target as HTMLInputElement).value;
		if ($markdownStore.metadata) {
			$markdownStore.metadata.slug = newPageSlug;
			$markdownStore.metadata.srcKey = newPageSlug;
		}
	}

	function updateAndSubmit() {
		if ($markdownStore.metadata) {
			console.log('updateAndSubmit: ', newPageSlug);
			$markdownStore.metadata.slug = newPageSlug;
			$markdownStore.metadata.srcKey = $markdownStore.metadata.parent + newPageSlug  + ".md";
			$modalStore[0].meta.onFormSubmit(newPageSlug);
			modalStore.close();
		}
	}
</script>

{#if $modalStore[0]}
	<div class="modal-example-form {cBase}">
		<header class={cHeader}>
			{$modalStore[0].meta.modalTitle} -
			<code>{newPageSlug}</code>
		</header>
		<article>
			<p>
				Creating new <strong>{$modalStore[0].meta.templateKey}</strong> named
				<strong>{$modalStore[0].meta.pageTitle}</strong> in folder <strong>{$modalStore[0].meta.parentFolder}</strong>.
			</p>
			<p>The slug (url) will be fixed after saving, so this is your last chance to change it.</p>
		</article>
		<!-- Enable for debugging: -->
		<form class="modal-form {cForm}">
			<TextInput
				name="slug"
				label="Slug/URL - must not contain spaces or special characters"
				bind:value={newPageSlug}
				required
				onInput={onSlugChange}
			/>
		</form>
		<!-- prettier-ignore -->
		<footer class="modal-footer {parent.regionFooter}">
			<button class="btn {parent.buttonNeutral}" on:click={parent.onClose}>{parent.buttonTextCancel}</button>
			<button disabled={disabled} class="btn variant-filled-primary" on:click={updateAndSubmit}>Save</button>
		</footer>
	</div>
{/if}
