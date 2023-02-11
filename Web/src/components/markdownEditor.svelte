<script lang="ts">
	import { onDestroy } from 'svelte';
	import { markdownStore } from '../stores/postsStore.svelte';
	import SvelteMarkdown from 'svelte-markdown';

	const markdownStoreUnsubscribe = markdownStore.subscribe((data) => {
		if (data) {
			console.log('Got some markdown data');
		}
	});

	onDestroy(markdownStoreUnsubscribe);
</script>

<div class="mt-5 md:col-span-2 md:mt-0">
	<h3 class="px-4 py-4 text-center text-2xl font-bold">Markdown Editor</h3>
	{#if $markdownStore}
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
	<div
		class="modal fade fixed top-0 left-0 hidden h-full w-full overflow-y-auto overflow-x-hidden outline-none"
		id="previewModal"
		tabindex="-1"
		aria-labelledby="exampleModalLabel"
		aria-hidden="true">
		<div class="modal-dialog modal-dialog-scrollable modal-xl pointer-events-none relative w-auto">
			<div
				class="modal-content pointer-events-auto relative flex w-full flex-col rounded-md border-none bg-white bg-clip-padding text-current shadow-lg outline-none">
				<div
					class="modal-header flex flex-shrink-0 items-center justify-between rounded-t-md border-b border-gray-200 p-4">
					<h5 class="text-xl font-medium leading-normal text-gray-800" id="exampleModalLabel">
						{$markdownStore.post.title}
					</h5>
					<button
						type="button"
						class="btn-close box-content h-4 w-4 rounded-none border-none p-1 text-black opacity-50 hover:text-black hover:no-underline hover:opacity-75 focus:opacity-100 focus:shadow-none focus:outline-none"
						data-bs-dismiss="modal"
						aria-label="Close" />
				</div>
				<div class="preview modal-body relative space-y-2 p-4 text-sm">
					<SvelteMarkdown source={mdSource} />
				</div>
				<div
					class="modal-footer flex flex-shrink-0 flex-wrap items-center justify-end rounded-b-md border-t border-gray-200 p-4">
					<button
						type="button"
						class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
						data-bs-dismiss="modal">Close</button>
				</div>
			</div>
		</div>
	</div>
{/if}
