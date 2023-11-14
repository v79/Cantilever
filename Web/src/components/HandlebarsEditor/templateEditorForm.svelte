<script lang="ts">
	import { Badge } from 'flowbite-svelte';
	import type { HandlebarsTemplate } from '../../models/structure';
	import TextInput from '../forms/textInput.svelte';

	export let hbTemplate: HandlebarsTemplate;
	export let body = '';
</script>

<div class="relative mt-5 md:col-span-2 md:mt-0">
	<div class="px-4 py-5 sm:p-6">
		<form action="#" method="POST">
			<div class="overflow-hidden shadow sm:rounded-md">
				<div class="grid grid-cols-6 gap-6">
					<div class="col-span-6 sm:col-span-6 lg:col-span-3">
						<TextInput bind:value={hbTemplate.template.key} readonly name="slug" label="Filename" />
					</div>
					<div class="col-span-6 sm:col-span-6 lg:col-span-3">
						<TextInput
							bind:value={hbTemplate.template.metadata.name}
							name="template-name"
							label="Template name" />
					</div>

					{#if hbTemplate.template.metadata.sections}
						<div class="col-span-5">
							<h3 class="text-base font-bold text-slate-200">Sections</h3>
						</div>
						<div class="col-span-1">
							<button
								class="float-right text-right text-sm font-medium text-slate-200"
								type="button">Edit...</button>
						</div>
						<div class="col-span-6 flex items-center justify-center space-x-4">
							{#each hbTemplate.template.metadata.sections as section}
								<Badge
									class="bg-primary-100 dark:bg-primary-900 inline-flex items-center justify-center divide-gray-200 rounded-md border-2 border-gray-200  px-2.5 py-0.5 text-lg font-medium text-slate-200 dark:divide-gray-700 dark:border-gray-700 dark:text-slate-800"
									>{section}</Badge>
							{/each}
						</div>
					{/if}
				</div>

				<div class="col-span-6 gap-6">
					<label for="markdown" class="text-sm font-medium text-slate-200">Handlebars HTML</label>
					<textarea
						bind:value={body}
						name="markdown"
						id="markdown"
						class="textarea-lg mt-1 block h-[500px] w-full rounded-md focus:border-indigo-500 focus:ring-indigo-500"
						placeholder="Handlebars HTML goes here" />
				</div>
			</div>
		</form>
	</div>
</div>
