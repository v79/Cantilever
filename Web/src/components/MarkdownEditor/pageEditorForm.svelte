<script lang="ts">
	import TextInput from '../forms/textInput.svelte';
	import { activeStore } from '../../stores/appStatusStore.svelte';
	import type { Page } from '../../models/structure';
	import { Accordion, AccordionItem } from 'flowbite-svelte';

	export let metadata: Page;
	export let previewModal = false;

	function bindMap(e: Event, key: string) {
		metadata.attributes.set(key, e.currentTarget.value);
		metadata.attributes = metadata.attributes;
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
						<Accordion
							flush
							activeClasses="bg-gray-100 dark:bg-gray-800 text-slate-200 dark:text-white focus:ring-4 focus:ring-gray-200 dark:focus:ring-gray-800"
							inactiveClasses="text-slate-200 dark:text-slate-300 hover:bg-gray-100 hover:dark:bg-gray-800">
							{#each [...metadata.sections] as [key, body]}
								<AccordionItem open>
									<span slot="header">{key}</span>
									<textarea
										bind:value={body}
										name="markdown-{key}"
										id="markdown-{key}"
										class="textarea-lg mt-1  block h-[500px] w-full rounded-md focus:border-indigo-500 focus:ring-indigo-500"
										placeholder="Markdown goes here" />
								</AccordionItem>
							{/each}
						</Accordion>
					</div>
				</div>
			</div>
		</form>
	</div>
</div>
