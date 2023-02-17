<script lang="ts">
	import { onDestroy } from 'svelte';
	import { markdownStore } from '../stores/postsStore.svelte';
	import SvelteMarkdown from 'svelte-markdown';
	import Modal from './modal.svelte';
	import Spinner from './utilities/spinner.svelte';
	import { userStore } from '../stores/userStore.svelte';
	import Alert from './utilities/alert.svelte';
	import { AlertStatus } from '../models/alertStatus';
	import { NotificationDisplay, notifier } from '@beyonk/svelte-notifications';

	let newSlug = '';
	let spinnerActive = false;
	let alertMessage = '';
	let alertStatus = 0;
	let alertHidden = true;

	const markdownStoreUnsubscribe = markdownStore.subscribe((data) => {
		if (data) {
			newSlug = createSlug(data.post.title);
		}
	});

	function createSlug(title: string) {
		// const invalid: RegExp = new RegExp(';/?:@&=+$, ', 'g');
		const invalid = /[;\/?:@%&=+$, ]/g;
		return title.replaceAll(invalid, '-');
	}

	function saveFile() {
		spinnerActive = true;
		console.log('Saving file ', $markdownStore.post.srcKey);
		let postJson = JSON.stringify($markdownStore);

		fetch('https://api.cantilevers.org/posts/save2', {
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
				notifier.success($markdownStore.post.srcKey + ' saved', 3000);
				console.log(data);
			})
			.catch((error) => {
				notifier.danger('Error saving: ' + error, { persist: true });
				// alertStatus = AlertStatus.Error;
				// alertMessage = 'Error saving: ' + error;
				// alertHidden = false;
				// TODO: present errors...
				console.log(error);
			});
		spinnerActive = false;
	}

	var hasChanged: Boolean = false;

	onDestroy(markdownStoreUnsubscribe);
</script>

<Spinner spinnerId="save-spinner" shown={spinnerActive} message="Saving..." />

<div class="relative mt-5 md:col-span-2 md:mt-0">
	<NotificationDisplay />

	<Alert message={alertMessage} hidden={alertHidden} />

	<h3 class="px-4 py-4 text-center text-2xl font-bold">Markdown Editor</h3>
	{#if $markdownStore}
		<div class="flex items-center justify-end pr-8 focus:shadow-lg" role="group">
			<button
				class="inline-block rounded-l bg-purple-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
				disabled>Restore</button>
			<button
				class="inline-block bg-purple-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
				disabled>Delete</button>
			<button
				type="button"
				data-bs-toggle="modal"
				data-bs-target="#save-dialog"
				class="inline-block rounded-r bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
				>Save</button>
		</div>
		<form action="#" method="POST">
			<div class="overflow-hidden shadow sm:rounded-md">
				<div class="px-4 py-5 sm:p-6">
					<div class="grid grid-cols-6 gap-6">
						<div class="col-span-6 sm:col-span-6 lg:col-span-2">
							<label for="Slug" class="block text-sm font-medium text-slate-200">Slug</label>
							<input
								bind:value={$markdownStore.post.url}
								disabled
								type="text"
								name="Slug"
								id="Slug"
								class="mt-1 block w-full rounded-md border-gray-300 text-slate-500 shadow-sm sm:text-sm" />
						</div>

						<div class="col-span-6 sm:col-span-3 lg:col-span-2">
							<label for="date" class="block text-sm font-medium text-slate-200">Date</label>
							<input
								bind:value={$markdownStore.post.date}
								type="date"
								name="date"
								id="date"
								class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm" />
						</div>

						<div class="col-span-6 sm:col-span-3 lg:col-span-2">
							<label for="Template" class="block text-sm font-medium text-slate-200"
								>Template</label>
							<input
								bind:value={$markdownStore.post.template.key}
								disabled
								type="text"
								name="Template"
								id="Template"
								class="mt-1 block w-full rounded-md border-gray-300 text-slate-500 shadow-sm sm:text-sm" />
						</div>

						<!-- <div class="col-span-6 sm:col-span-3 lg:col-span-2">
				  <label for="postal_code" class="block text-sm font-medium text-gray-700">ZIP / Postal</label>
				  <input type="text" name="postal_code" id="postal_code" autocomplete="postal-code" class="mt-1 focus:ring-indigo-500 focus:border-indigo-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md">
				</div> -->

						<div class="col-span-6">
							<label for="title" class="block text-sm font-medium text-slate-200">Title</label>
							<input
								bind:value={$markdownStore.post.title}
								type="text"
								name="title"
								id="title"
								autocomplete="title"
								class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm" />
						</div>
						<div class="col-span-6">
							<label for="markdown" class="text-sm font-medium text-slate-200">Markdown</label>
							<button
								type="button"
								class="float-right text-right text-sm font-medium text-slate-200"
								data-bs-toggle="modal"
								data-bs-target="#previewModal">Preview</button>
							<textarea
								bind:value={$markdownStore.body}
								name="markdown"
								id="markdown"
								class="textarea-lg mt-1  block h-[500px] w-full rounded-md focus:border-indigo-500 focus:ring-indigo-500"
								placeholder="Markdown goes here" />
						</div>
					</div>
				</div>
				<!-- <div class="px-4 py-3 text-right sm:px-6">
				<button
					type="submit"
					class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
				>
					Save
				</button>
			</div> -->
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
	<Modal modalId="previewModal">
		<h5 slot="title" class="text-xl font-medium leading-normal text-gray-800" id="markdown-preview">
			{$markdownStore.post.title}
		</h5>
		<SvelteMarkdown slot="body" source={mdSource} />
	</Modal>
{/if}

<!-- save dialog -->
{#if $markdownStore}
	<Modal modalId="save-dialog" modalSize="sm">
		<h5 slot="title" class="text-xl font-medium leading-normal text-gray-800">Save file?</h5>
		<svelte:fragment slot="body">
			Save changes to file <strong>{$markdownStore.post.title}</strong> (<em
				>{$markdownStore.post.url}?</em
			>)
		</svelte:fragment>
		<svelte:fragment slot="buttons">
			<button
				type="button"
				class="rounded bg-purple-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
				data-bs-dismiss="modal">Cancel</button>
			<button
				type="button"
				on:click={saveFile}
				class="inline-block rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
				data-bs-dismiss="modal">Save</button>
		</svelte:fragment>
	</Modal>
{/if}
