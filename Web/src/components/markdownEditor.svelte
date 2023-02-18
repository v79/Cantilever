<script lang="ts">
	import { beforeUpdate, afterUpdate, onDestroy } from 'svelte';
	import { markdownStore } from '../stores/markdownPostStore.svelte';
	import SvelteMarkdown from 'svelte-markdown';
	import Modal from './modal.svelte';
	import Spinner from './utilities/spinner.svelte';
	import { userStore } from '../stores/userStore.svelte';
	import { NotificationDisplay, notifier } from '@beyonk/svelte-notifications';
	import { activeStore } from '../stores/appStatusStore.svelte';
	import SaveNewFile from './saveNewFile.svelte';

	// import * as te from 'tw-elements';

	// const myModal = new te.Modal(document.getElementById('save-dialog'), {});

	let spinnerActive = false;
	let saveNewFileSlug: '';
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
		return title.toLowerCase().replaceAll(invalid, '-');
	}

	onDestroy(markdownStoreUnsubscribe);
</script>

<Spinner spinnerId="save-spinner" shown={spinnerActive} message="Saving..." />

<div class="relative mt-5 md:col-span-2 md:mt-0">
	<NotificationDisplay />

	<h3 class="px-4 py-4 text-center text-2xl font-bold">Markdown Editor {formIsValid}</h3>
	{#if $markdownStore}
		<div class="flex items-center justify-end pr-8 focus:shadow-lg" role="group">
			<button
				class="inline-block rounded-l bg-purple-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
				disabled>Restore</button>
			<button
				class="inline-block bg-purple-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
				disabled>Delete</button>
			<!-- //data-bs-toggle="modal" //data-bs-target="#save-dialog"-->
			<button
				type="button"
				data-bs-toggle="modal"
				data-bs-target="#save-dialog"
				disabled={!formIsValid}
				class="inline-block rounded-r bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800 disabled:hover:bg-purple-600"
				>Save</button>
		</div>
		<form action="#" method="POST">
			<div class="overflow-hidden shadow sm:rounded-md">
				<div class="px-4 py-5 sm:p-6">
					<div class="grid grid-cols-6 gap-6">
						<div class="col-span-6 sm:col-span-6 lg:col-span-2">
							<label for="Slug" class="block text-sm font-medium text-slate-200">Slug</label>
							<input
								bind:value={$activeStore.newSlug}
								readonly
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
								required
								class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm" />
							{#if $markdownStore.post.date === ''}
								<span class="text-sm text-yellow-200">Date must not be blank</span>
							{/if}
						</div>

						<div class="col-span-6 sm:col-span-3 lg:col-span-2">
							<label for="Template" class="block text-sm font-medium text-slate-200"
								>Template</label>
							<input
								bind:value={$markdownStore.post.template.key}
								readonly
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
								required
								autocomplete="title"
								class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm" />
							{#if $markdownStore.post.title === ''}
								<span class="text-sm text-yellow-200">Title must not be blank</span>
							{/if}
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

<SaveNewFile modalId="save-dialog" />
