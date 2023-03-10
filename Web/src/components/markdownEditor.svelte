<script lang="ts">
	import {Modal} from 'flowbite-svelte';
	import {afterUpdate, beforeUpdate, onDestroy} from 'svelte';
	import SvelteMarkdown from 'svelte-markdown';
	import {spinnerStore} from '../components/utilities/spinnerWrapper.svelte';
	import {activeStore} from '../stores/appStatusStore.svelte';
	import {markdownStore} from '../stores/markdownPostStore.svelte';
	import {notificationStore} from '../stores/notificationStore.svelte';
	import {structureStore} from '../stores/postsStore.svelte';
	import {userStore} from '../stores/userStore.svelte';
	import CModal from './customized/cModal.svelte';
	import DatePicker from './forms/datePicker.svelte';
	import TextInput from './forms/textInput.svelte';
	import ModalDeleteFile from './MarkdownEditor/modal-delete-file.svelte';

	let saveExistingModal = false;
	let saveNewModal = false;
	let previewModal = false;
	let deleteFileModal = false;

	let saveNewFileSlug = '';
	$: formIsValid = $markdownStore?.post.title != '' && $markdownStore?.post.date != '';

	afterUpdate(() => {});

	const markdownStoreUnsubscribe = markdownStore.subscribe((data) => {
		// if (data) {
		// 	newSlug = createSlug(data.post.title);
		// }
	});

	beforeUpdate(() => {
		if ($activeStore.isNewFile) {
			$activeStore.newSlug = createSlug($markdownStore.post.title);
		}
	});

	function createSlug(title: string) {
		// const invalid: RegExp = new RegExp(';/?:@&=+$, ', 'g');
		const invalid = /[;\/?:@%&=+$, ]/g;
		return title.trim().toLowerCase().replaceAll(invalid, '-');
	}

	function saveFile() {
		console.log('Saving file ', $markdownStore.post.srcKey);
		let postJson = JSON.stringify($markdownStore);
		console.log(postJson);

		fetch('https://api.cantilevers.org/posts/save', {
			method: 'POST',
			headers: {
				Accept: 'text/plain',
				Authorization: 'Bearer ' + $userStore.token
			},
			body: postJson,
			mode: 'cors'
		})
			.then((response) => response.text())
			.then((data) => {
				notificationStore.set({
					message: decodeURI($markdownStore.post.srcKey) + ' saved. ' + data,
					shown: true,
					type: 'success'
				});
				let existing = $structureStore.posts.find(
					(post) => post.srcKey === $markdownStore.post.srcKey
				);
				if (!existing) {
					console.log('Added brand new file to structure');
					$structureStore.postCount = $structureStore.posts.push($markdownStore.post);
				}
				console.log(data);
			})
			.catch((error) => {
				notificationStore.set({ message: 'Error saving: ' + error, shown: true, type: 'error' });
				console.log(error);
			});
		$spinnerStore.shown = false;
	}

	onDestroy(markdownStoreUnsubscribe);
</script>

<div class="relative mt-5 md:col-span-2 md:mt-0">
	<h3 class="px-4 py-4 text-center text-2xl font-bold">
		{#if $markdownStore?.post.title}{$markdownStore.post.title}{:else}Markdown Editor {/if}
	</h3>
	{#if $markdownStore}
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
						saveNewFileSlug = createSlug($markdownStore.post.title);
						saveNewModal = true;
					} else {
						saveExistingModal = true;
					}
				}}
				disabled={!formIsValid}
				class="inline-block rounded-r bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800 disabled:hover:bg-purple-600"
				>Save</button>
		</div>
		<form action="#" method="POST">
			<div class="overflow-hidden shadow sm:rounded-md">
				<div class="px-4 py-5 sm:p-6">
					<div class="grid grid-cols-6 gap-6">
						<div class="col-span-6 sm:col-span-6 lg:col-span-2">
							<TextInput bind:value={$activeStore.newSlug} readonly name="slug" label="Slug/URL" />
						</div>

						<div class="col-span-6 sm:col-span-3 lg:col-span-2">
							<DatePicker label="Date" name="date" required bind:value={$markdownStore.post.date} />
						</div>

						<div class="col-span-6 sm:col-span-3 lg:col-span-2">
							<TextInput
								bind:value={$markdownStore.post.templateKey}
								name="template"
								label="Template"
								required
								readonly />
						</div>

						<div class="col-span-6">
							<TextInput
								bind:value={$markdownStore.post.title}
								required
								name="Title"
								label="Title" />
						</div>
						<div class="col-span-6">
							<label for="markdown" class="text-sm font-medium text-slate-200">Markdown</label>
							<button
								type="button"
								class="float-right text-right text-sm font-medium text-slate-200"
								on:click={() => {
									previewModal = true;
								}}>Preview</button>
							<textarea
								bind:value={$markdownStore.body}
								name="markdown"
								id="markdown"
								class="textarea-lg mt-1  block h-[500px] w-full rounded-md focus:border-indigo-500 focus:ring-indigo-500"
								placeholder="Markdown goes here" />
						</div>
					</div>
				</div>
			</div>
		</form>
	{:else}
		<h3 class="px-8 text-center text-lg text-slate-200">
			Load an existing file or create a new one to get started
		</h3>
	{/if}
</div>

<!-- preview modal -->
{#if $markdownStore}
	{@const mdSource = $markdownStore.body}
	<Modal title={$markdownStore.post.title} bind:open={previewModal} size="lg">
		<SvelteMarkdown source={mdSource} />
	</Modal>
{/if}

<CModal title="Save file?" bind:open={saveExistingModal} autoclose size="sm">
	<p>
		Save changes to file <strong>{$markdownStore.post.title}</strong>?
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
</CModal>

<CModal title="Save new file?" bind:open={saveNewModal} autoclose size="sm">
	<p>
		Creating new <strong>{$markdownStore.post.templateKey}</strong> named
		<strong>{$markdownStore.post.title}</strong>.
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
			autocomplete="new-slug"
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
			on:click={(e) => {
				let srcKey = 'sources/posts/' + saveNewFileSlug + '.md';
				spinnerStore.set({ message: 'Saving ' + srcKey, shown: true });
				$markdownStore.post.srcKey = srcKey;
				$markdownStore.post.url = saveNewFileSlug;
				$markdownStore.post.lastUpdated = new Date().toISOString();
				saveFile();
			}}
			class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
			>Save</button>
	</svelte:fragment>
</CModal>

<ModalDeleteFile
	shown={deleteFileModal}
	on:closeModal={(e) => {
		deleteFileModal = false;
	}} />
