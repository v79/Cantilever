<script lang="ts">
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { onMount, type SvelteComponent } from 'svelte';
	import { createSlug } from '$lib/stores/contentStore.svelte';
	import { project } from '$lib/stores/projectStore.svelte';

	import TextInput from '$lib/forms/textInput.svelte';
	export let parent: SvelteComponent;
	const modalStore = getModalStore();

	$: newProjectSlug = '';
	$: saveDomain = '';
	// TODO: this needs to be a much more complex validation
	$: disabled = saveDomain === '' || saveDomain.indexOf('.') === -1;

	// Base Classes
	const cBase = 'card p-4 w-modal shadow-xl space-y-4';
	const cHeader = 'text-2xl font-bold';
	const cForm = 'p-4 space-y-4 rounded-container-token';

	onMount(() => {
		if ($project) {
			saveDomain = $project.domain;
			newProjectSlug = createSlug(saveDomain);
		}
	});

	function onDomainChange(e: Event) {
		saveDomain = (e.target as HTMLInputElement).value;
		// TODO: add validation
		if ($project) {
			$project.domain = saveDomain;
		}
	}

	function updateAndSubmit() {
		if ($project) {
			$project.domain = saveDomain;
			$modalStore[0].meta.onFormSubmit(saveDomain);
			modalStore.close();
		}
	}
</script>

{#if $modalStore[0]}
	<div class="modal-example-form {cBase}">
		<header class={cHeader}>
			{$modalStore[0].meta.modalTitle} -
			<code>{saveDomain}</code>
		</header>
		<article>
			<p>
				Creating new project named
				<strong>{$modalStore[0].meta.projectTitle}</strong> for website domain
				<strong>{saveDomain}</strong>.
			</p>
			<p>The domain should match that of your website, and cannot be changed later.</p>
			<p>The project definition file will be saved as <code>{newProjectSlug}.yaml</code>.</p>
		</article>
		<!-- Enable for debugging: -->
		<form class="modal-form {cForm}">
			<TextInput
				name="domain"
				label="Website domain"
				bind:value={saveDomain}
				required
				onInput={onDomainChange} />
		</form>
		<!-- prettier-ignore -->
		<footer class="modal-footer {parent.regionFooter}">
			<button class="btn {parent.buttonNeutral}" on:click={parent.onClose}>{parent.buttonTextCancel}</button>
			<button disabled={disabled} class="btn variant-filled-primary" on:click={updateAndSubmit}>Save</button>
		</footer>
	</div>
{/if}
