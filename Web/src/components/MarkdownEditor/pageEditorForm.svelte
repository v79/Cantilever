<script lang="ts">
	import { Modal, TabItem, Tabs } from 'flowbite-svelte';
	import SvelteMarkdown from 'svelte-markdown';
	import type { Page } from '../../models/structure';
	import { activeStore } from '../../stores/appStatusStore.svelte';
	import Viditor from '../Viditor.svelte';
	import TextInput from '../forms/textInput.svelte';

	export let metadata: Page;
	export let previewModal = false;

	// function to combine all of the sections into a single string, for the preview modal
	function mergeSources(sources: Map<string, string>) {
		var source = '';
		for (let src of sources.values()) {
			source += '\n' + src;
		}
		return source;
	}

	$: markdownSource = mergeSources(metadata.sections);

	function bindMap(e: Event, key: string) {
		const { target } = e;
		if (target) {
			metadata.attributes.set(key, (target as HTMLInputElement).value);
			metadata.attributes = metadata.attributes;
		}
	}
</script>

<div class="relative mt-5 md:col-span-2 md:mt-0">
	<div class="px-4 py-5 sm:p-6">
		<form action="#" method="POST">
			<div class="overflow-hidden shadow sm:rounded-md">
				<div class="grid grid-cols-6 gap-6">
					<div class="col-span-6 sm:col-span-6 lg:col-span-3">
						<TextInput bind:value={$activeStore.newSlug} readonly name="slug" label="Slug/URL" />
					</div>

					<div class="col-span-6 sm:col-span-3 lg:col-span-3">
						<TextInput
							bind:value={metadata.templateKey}
							name="template"
							label="Template"
							required />
					</div>

					<div class="col-span-6">
						<TextInput bind:value={metadata.title} required name="Title" label="Title" />
					</div>
					{#if metadata.attributes}
						<div class="col-span-5">
							<h3 class="text-base font-bold text-slate-200">Attributes</h3>
						</div>
						<div class="col-span-1">
							<button
								class="float-right text-right text-sm font-medium text-slate-200"
								type="button">Edit...</button>
						</div>
						{#each [...metadata.attributes] as [key, value]}
							<div class="col-span-2 sm:col-span-2 lg:col-span-2">
								<TextInput
									bind:value
									name="attribute-{key}"
									label={key}
									onInput={(event) => bindMap(event, key)} />
							</div>
						{/each}
					{/if}
					<div class="col-span-6">
						<h3 class="text-base font-bold text-slate-200">Markdown</h3>
						<button
							type="button"
							class="float-right text-right text-sm font-medium text-slate-200"
							on:click={() => {
								previewModal = true;
							}}>Preview</button>
						<Tabs style="pill">
							{#each [...metadata.sections] as [key, body], index}
								<TabItem
									open={index == 0}
									title={key}
									inactiveClasses="inline-block text-sm font-medium text-center disabled:cursor-not-allowed p-4 hover:text-purple-500 hover:border-gray-300 dark:hover:text-gray-300 text-slate-200 dark:text-gray-400"
									activeClasses="inline-block text-sm font-medium text-center disabled:cursor-not-allowed p-4 border-b-2 border-purple-400 border-spacing-4 hover:text-purple-500 hover:border-gray-300 dark:hover:text-gray-300 text-slate-200 dark:text-gray-400">
									<Viditor
										bind:body
										id="markdown-{key}"
										onChange={(newBody) => {
											body = newBody;
											metadata.sections.set(key, newBody);
											metadata.sections = metadata.sections;
										}} />
								</TabItem>
							{/each}
						</Tabs>
					</div>
				</div>
			</div>
		</form>
	</div>
</div>

<!-- preview modal -->

<Modal title={metadata?.title} bind:open={previewModal} size="lg">
	<SvelteMarkdown source={markdownSource} />
</Modal>
