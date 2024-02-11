<script lang="ts">
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { onMount, type SvelteComponent } from 'svelte';
	import { createSlug } from '$lib/stores/contentStore.svelte';
	import { markdownStore } from '$lib/stores/contentStore.svelte';
	import TextInput from '$lib/forms/textInput.svelte';
	import { validateSlug } from '$lib/functions/validateSlug';
	
	export let parent: SvelteComponent;
	const modalStore = getModalStore();

	$: newPostSlug = '';
	$: disabled = newPostSlug === '' || !validateSlug(newPostSlug);

	// Base Classes
	const cBase = 'card p-4 w-modal shadow-xl space-y-4';
	const cHeader = 'text-2xl font-bold';
	const cForm = 'p-4 space-y-4 rounded-container-token';

	onMount(() => {
		newPostSlug = createSlug($modalStore[0].meta.postTitle);
	});

	function onSlugChange(e: Event) {
		newPostSlug = (e.target as HTMLInputElement).value;
		if ($markdownStore.metadata) {
			$markdownStore.metadata.slug = newPostSlug;
		}
	}

	function updateAndSubmit() {
		if ($markdownStore.metadata) {
			console.log('updateAndSubmit: ', newPostSlug);
			$markdownStore.metadata.slug = newPostSlug;
			$modalStore[0].meta.onFormSubmit();
			modalStore.close();
		}
	}
</script>

{#if $modalStore[0]}
	<div class="modal-example-form {cBase}">
		<header class={cHeader}>
			{$modalStore[0].meta.modalTitle} -
			<code>{newPostSlug}</code>
		</header>
		<article>
			<p>
				Creating new <strong>{$modalStore[0].meta.templateKey}</strong> named
				<strong>{$modalStore[0].meta.postTitle}</strong>.
			</p>
			<p>The slug (url) will be fixed after saving, so this is your last chance to change it.</p>
		</article>
		<!-- Enable for debugging: -->
		<form class="modal-form {cForm}">
			<TextInput
				name="slug"
				label="Slug/URL - must not contain spaces or special characters"
				bind:value={newPostSlug}
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
