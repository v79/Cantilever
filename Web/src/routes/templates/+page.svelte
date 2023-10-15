<script lang="ts">
	import { afterNavigate } from '$app/navigation';
	import SpinnerWrapper from '../../components/utilities/spinnerWrapper.svelte';
	import { activeStore } from '../../stores/appStatusStore.svelte';
	import TemplateList from './templateList.svelte';
	import { handlebarStore } from '../../stores/handlebarContentStore.svelte';
	import { Template } from '../../models/structure';
	import { userStore } from '../../stores/userStore.svelte';
	import { notificationStore } from '../../stores/notificationStore.svelte';
	import TemplateEditorForm from '../../components/HandlebarsEditor/templateEditorForm.svelte';
	import { Modal } from 'flowbite-svelte';
	import ActiveStoreView from '../../components/activeStoreView.svelte';

	let deleteFileModal = false;
	let saveExistingModal = false;
	let saveNewModal = false;
	let saveNewFileSlug = '';

	function saveFile() {
		console.log('Saving template file ', $handlebarStore.template?.key);
		let templateJson = JSON.stringify($handlebarStore);

		fetch('https://api.cantilevers.org/templates/', {
			method: 'POST',
			headers: {
				Accept: 'text/plain',
				Authorization: 'Bearer ' + $userStore.token,
				'Content-Type': 'application/json'
			},
			body: templateJson,
			mode: 'cors'
		})
			.then((response) => response.text())
			.then((data) => {
				notificationStore.set({
					message: decodeURI($handlebarStore.template?.key ?? '') + ' saved. ' + data,
					shown: true,
					type: 'success'
				});
			});
	}

	function generatePages() {
		console.log(
			'Trigger page regeneration for all pages with template ',
			$handlebarStore.template?.key
		);
		if ($handlebarStore.template?.key) {
			fetch(
				'https://api.cantilevers.org/generate/template/' +
					encodeURIComponent($handlebarStore.template?.key),
				{
					method: 'PUT',
					headers: {
						Accept: 'text/plain',
						Authorization: 'Bearer ' + $userStore.token,
						'X-Content-Length': '0'
					},
					mode: 'cors'
				}
			)
				.then((response) => response.text())
				.then((data) => {
					notificationStore.set({ message: data, shown: true, type: 'success' });
				});
			//TODO error handling
		}
	}

	afterNavigate(() => {
		$activeStore.currentPage = 'Templates';
		$activeStore.activeFile = '';
	});
</script>

<div class="flex grow flex-row">
	<div class="basis-1/4 bg-slate-400">
		<TemplateList />
	</div>

	<div class="basis-1/2 bg-slate-600">
		<div class="relative mt-5 md:col-span-2 md:mt-0">
			<h3 class="px-4 py-4 text-center text-2xl font-bold">
				{#if $handlebarStore?.template?.key}{$activeStore.activeFile}{:else}Handlebars Editor
				{/if}
			</h3>

			{#if $handlebarStore?.template instanceof Template}
				<div class="flex items-center justify-end pr-8 focus:shadow-lg" role="group">
					<button
						class="inline-block rounded-l bg-purple-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
						type="button"
						on:click={generatePages}>Rebuild pages</button>
					<button
						class="inline-block rounded-l bg-purple-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
						disabled>Reset Changes</button>
					<button
						disabled
						class="inline-block bg-red-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
						on:click={() => {
							deleteFileModal = true;
						}}>Delete</button>
					<button
						type="button"
						on:click={() => {
							if ($activeStore.isNewFile) {
								saveNewModal = true;
							} else {
								saveExistingModal = true;
							}
						}}
						disabled={false}
						class="inline-block rounded-r bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800 disabled:hover:bg-purple-600"
						>Save</button>
				</div>
				<TemplateEditorForm
					bind:template={$handlebarStore.template}
					bind:body={$handlebarStore.body} />
			{:else}
				<h3 class="px-8 text-center text-lg text-slate-200">
					Load an existing file or create a new one to get started
				</h3>
			{/if}
		</div>
	</div>

	<div class="invisible basis-1/4 bg-slate-800 lg:visible">
		<h3 class="px-4 py-4 text-center text-2xl font-bold text-slate-200">Messages</h3>
		<ActiveStoreView />
		<SpinnerWrapper spinnerID="globalSpinner" />
	</div>
</div>

<Modal title="Save file?" bind:open={saveExistingModal} autoclose size="sm">
	<p>
		Save changes to file <strong>{$activeStore.activeFile}</strong>?
	</p>
	<svelte:fragment slot="footer">
		<button
			type="button"
			class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
			>Cancel</button>
		<button
			type="button"
			on:click={saveFile}
			class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
			>Save</button>
	</svelte:fragment>
</Modal>
