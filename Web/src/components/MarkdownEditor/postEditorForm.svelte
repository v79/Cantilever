<script lang="ts">
	import TextInput from '../forms/textInput.svelte';
	import DatePicker from '../forms/datePicker.svelte';
	import type { Post } from '../../models/structure';
	import { activeStore } from '../../stores/appStatusStore.svelte';

	export let metadata: Post;
	export let body: string = '';
	export let previewModal = false;
</script>

<form action="#" method="POST">
	<div class="overflow-hidden shadow sm:rounded-md">
		<div class="px-4 py-5 sm:p-6">
			<div class="grid grid-cols-6 gap-6">
				<div class="col-span-6 sm:col-span-6 lg:col-span-2">
					<TextInput bind:value={$activeStore.newSlug} readonly name="slug" label="Slug/URL" />
				</div>

				<div class="col-span-6 sm:col-span-3 lg:col-span-2">
					<DatePicker label="Date" name="date" required bind:value={metadata.date} />
				</div>

				<div class="col-span-6 sm:col-span-3 lg:col-span-2">
					<TextInput
						bind:value={metadata.templateKey}
						name="template"
						label="Template"
						required
						readonly />
				</div>

				<div class="col-span-6">
					<TextInput bind:value={metadata.title} required name="Title" label="Title" />
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
						bind:value={body}
						name="markdown"
						id="markdown"
						class="textarea-lg mt-1  block h-[500px] w-full rounded-md focus:border-indigo-500 focus:ring-indigo-500"
						placeholder="Markdown goes here" />
				</div>
			</div>
		</div>
	</div>
</form>
