<script lang="ts">
	import { afterNavigate } from '$app/navigation';
	import ModalDeleteFile from '../../components/MarkdownEditor/modal-delete-file.svelte';
	import PageEditorForm from '../../components/MarkdownEditor/pageEditorForm.svelte';
	import { Modal } from 'flowbite-svelte';
	import PageList from './pageList.svelte';
	import SpinnerWrapper from '../../components/utilities/spinnerWrapper.svelte';
	import { createSlug } from '../../functions/createSlug';
	import { Page } from '../../models/structure';
	import { AS_CLEAR, activeStore } from '../../stores/appStatusStore.svelte';
	import { markdownStore } from '../../stores/markdownContentStore.svelte';
	import { notificationStore } from '../../stores/notificationStore.svelte';
	import { userStore } from '../../stores/userStore.svelte';
	import ActiveStoreView from '../../components/activeStoreView.svelte';

	let previewModal = false;
	let deleteFileModal = false;
	let saveExistingModal = false;
	let saveNewModal = false;
	let saveNewFileSlug = '';

	afterNavigate(() => {
		activeStore.set(AS_CLEAR);
		$activeStore.currentPage = 'Pages';
	});

	function mapReplacer(key: string, value: any): any {
		if (value instanceof Map) {
			return Object.fromEntries(value);
		} else {
			return value;
		}
	}

	function saveFile() {
		if ($markdownStore.metadata === null) {
			throw new Error('Cannot save a page with no metadata');
		} else {
			if ($markdownStore.metadata.srcKey) {
				$markdownStore.metadata.srcKey = $activeStore.folder?.srcKey + $activeStore.newSlug + '.md';
			}
			console.log('Saving page file ', $markdownStore.metadata?.srcKey);
		}
		let pageJson = JSON.stringify($markdownStore, mapReplacer);
		console.log(pageJson);
		fetch('https://api.cantilevers.org/project/pages/', {
			method: 'POST',
			headers: {
				Accept: 'text/plain',
				Authorization: 'Bearer ' + $userStore.token,
				'Content-Type': 'application/json'
			},
			body: pageJson,
			mode: 'cors'
		})
			.then((response) => response.text())
			.then((data) => {
				notificationStore.set({
					message: decodeURI($markdownStore.metadata?.srcKey ?? '') + ' saved. ' + data,
					shown: true,
					type: 'success'
				});
				$activeStore.isNewFile = false;
			})
			.catch((error) => {
				console.log(error);
				notificationStore.set({
					message: error,
					shown: true,
					type: 'error'
				});
				return {};
			});
	}
</script>

<div class="flex grow flex-row">
	<div class="basis-1/4 bg-slate-400">
		<PageList />
	</div>
	<div class="basis-1/2 bg-slate-600">
		<div class="relative mt-5 md:col-span-2 md:mt-0">
			<h3 class="px-4 py-4 text-center text-2xl font-bold">
				{#if $markdownStore?.metadata?.title}{$markdownStore?.metadata.title}{:else}Markdown Editor
				{/if}
			</h3>

			{#if $markdownStore?.metadata instanceof Page}
				<div class="flex items-center justify-end pr-8 focus:shadow-lg" role="group">
					<button
						class="inline-block rounded-l bg-purple-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
						disabled>Reset Changes</button>
					<button
						class="inline-block bg-red-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
						on:click={() => {
							deleteFileModal = true;
						}}>Delete</button>
					<button
						type="button"
						on:click={() => {
							if ($activeStore.isNewFile) {
								saveNewFileSlug = createSlug($markdownStore?.metadata?.title ?? '');
								$activeStore.newSlug = saveNewFileSlug;
								$markdownStore.metadata.srcKey = saveNewFileSlug;
								$activeStore.activeFile = saveNewFileSlug;
								saveNewModal = true;
							} else {
								saveExistingModal = true;
							}
						}}
						disabled={!$markdownStore?.metadata?.isValid() ?? true}
						class="inline-block rounded-r bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800 disabled:bg-slate-800 disabled:hover:bg-purple-600"
						>Save</button>
				</div>
				<PageEditorForm bind:metadata={$markdownStore.metadata} bind:previewModal />
			{:else}
				<h3 class="px-8 text-center text-lg text-slate-200">
					Load an existing file or create a new one to get started
				</h3>
			{/if}
		</div>
	</div>
	<div class="invisible basis-1/4 bg-slate-800 lg:visible">
		<ActiveStoreView />
		<SpinnerWrapper spinnerID="globalSpinner" />
	</div>
</div>

<Modal title="Save file?" bind:open={saveExistingModal} autoclose size="sm">
	<p>
		Save changes to file <strong>{$markdownStore?.metadata?.title}</strong>?
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

<Modal title="Save new page?" bind:open={saveNewModal} autoclose size="sm">
	<p>
		Creating new page <strong>{$markdownStore.metadata?.title}</strong> from template
		<strong>{$markdownStore.metadata?.templateKey}</strong> in folder
		<strong>{$activeStore.folder?.srcKey}</strong>.
	</p>
	<p>The slug (url) will be fixed after saving, so this is your last chance to change it.</p>
	<form>
		<label for="new-slug" class="block text-sm font-medium text-slate-600">Slug/url</label>
		<input
			type="text"
			name="new-slug"
			id="new-slug"
			bind:value={saveNewFileSlug}
			required
			class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm" />
		{#if saveNewFileSlug === ''}
			<span class="text-sm text-yellow-600"
				>Slug must not be blank and will be set to the default value on save</span>
		{/if}
	</form>

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

<ModalDeleteFile
	shown={deleteFileModal}
	on:closeModal={(e) => {
		deleteFileModal = false;
	}} />
