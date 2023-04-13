<script lang="ts">
	import { afterNavigate } from '$app/navigation';
	import PageEditorForm from '../../components/MarkdownEditor/pageEditorForm.svelte';
	import PageList from '../../components/pages/pageList.svelte';
	import SpinnerWrapper from '../../components/utilities/spinnerWrapper.svelte';
	import { Page } from '../../models/structure';
	import { activeStore } from '../../stores/appStatusStore.svelte';
	import { markdownStore } from '../../stores/markdownContentStore.svelte';

	let previewModal = false;

	afterNavigate(() => {
		$activeStore.currentPage = 'Pages';
		$activeStore.activeFile = '';
	});
</script>

<div class="flex grow flex-row">
	<div class="basis-1/4 bg-slate-400">
		<PageList />
	</div>
	<div class="basis-1/2 bg-slate-600">
		<div class="relative mt-5 md:col-span-2 md:mt-0">
			<h3 class="px-4 py-4 text-center text-2xl font-bold">
				{#if $markdownStore?.metadata?.title}{$markdownStore.metadata.title}{:else}Markdown Editor
				{/if}
			</h3>

			{#if $markdownStore.metadata instanceof Page}
				<PageEditorForm
					metadata={$markdownStore.metadata}
					bind:previewModal
					body="Body goes here" />
			{:else}
				<h3 class="px-8 text-center text-lg text-slate-200">
					Load an existing file or create a new one to get started
				</h3>
			{/if}
		</div>
	</div>
	<div class="invisible basis-1/4 bg-slate-800 lg:visible">
		<h3 class="px-4 py-4 text-center text-2xl font-bold text-slate-200">Messages</h3>
		<SpinnerWrapper spinnerID="globalSpinner" />
	</div>
</div>
