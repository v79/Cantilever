<script lang="ts">
	import TextInput from '../forms/textInput.svelte';
	import { activeStore } from '../../stores/appStatusStore.svelte';
	import type { Template } from '../../models/structure';

	export let template: Template;
	export let body: string = '';
</script>

<div class="relative mt-5 md:col-span-2 md:mt-0">
	<div class="px-4 py-5 sm:p-6">
		<form action="#" method="POST">
			<div class="overflow-hidden shadow sm:rounded-md">
				<div class="grid grid-cols-6 gap-6">
					<div class="col-span-6 sm:col-span-6 lg:col-span-3">
						<TextInput bind:value={$activeStore.activeFile} readonly name="slug" label="Filename" />
					</div>
				</div>

				<div class="col-span-6">
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
