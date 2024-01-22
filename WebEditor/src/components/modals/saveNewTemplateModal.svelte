<script lang="ts">
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { onMount, type SvelteComponent } from 'svelte';
	import { createSlug } from '../../stores/contentStore.svelte';
import { handlebars } from '../../stores/contentStore.svelte';
	import TextInput from '../forms/textInput.svelte';
	export let parent: SvelteComponent;
	const modalStore = getModalStore();
	const validChars: RegExp = /^[\a-zA-Z0-9]+(?:-[\w]+)*$/;

	$: newTemplateKey = '';
	$: disabled = newTemplateKey === '' || !validChars.test(newTemplateKey);

	// Base Classes
	const cBase = 'card p-4 w-modal shadow-xl space-y-4';
	const cHeader = 'text-2xl font-bold';
	const cForm = 'p-4 space-y-4 rounded-container-token';

	onMount(() => {
		newTemplateKey = createSlug($modalStore[0].meta.templateTitle);
	});

	function onKeyChange(e: Event) {
		newTemplateKey = (e.target as HTMLInputElement).value;
		if ($handlebars.srcKey) {
			$handlebars.srcKey = newTemplateKey;
		}
	}

	function updateAndSubmit() {
		if ($handlebars) {
			console.log('updateAndSubmit: ', newTemplateKey);
			$handlebars.srcKey = newTemplateKey;
			$modalStore[0].meta.onFormSubmit();
			modalStore.close();
		}
	}
</script>

{#if $modalStore[0]}
	<div class="modal-example-form {cBase}">
		<header class={cHeader}>
			{$modalStore[0].meta.modalTitle} -
			<code>{newTemplateKey}</code>
		</header>
		<article>
			<p>
				Creating new template named
				<strong>{$modalStore[0].meta.templateTitle}</strong>.
			</p>
			<p>The filename will be fixed after saving, so this is your last chance to change it.</p>
		</article>
		<!-- Enable for debugging: -->
		<form class="modal-form {cForm}">
			<TextInput
				name="filename"
				label="Filename - must not contain spaces or special characters"
				bind:value={newTemplateKey}
				required
				onInput={onKeyChange}
			/>
		</form>
		<!-- prettier-ignore -->
		<footer class="modal-footer {parent.regionFooter}">
			<button class="btn {parent.buttonNeutral}" on:click={parent.onClose}>{parent.buttonTextCancel}</button>
			<button disabled={disabled} class="btn variant-filled-primary" on:click={updateAndSubmit}>Save</button>
		</footer>
	</div>
{/if}
