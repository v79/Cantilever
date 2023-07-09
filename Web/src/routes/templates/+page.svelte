<script lang="ts">
	import { afterNavigate } from '$app/navigation';
	import SpinnerWrapper from '../../components/utilities/spinnerWrapper.svelte';
	import { activeStore } from '../../stores/appStatusStore.svelte';
	import TemplateList from '../../components/templates/templateList.svelte';
	import { handlebarStore } from '../../stores/handlebarContentStore.svelte';

	afterNavigate(() => {
		$activeStore.currentPage = 'Templates';
		$activeStore.activeFile = '';
	});
</script>

<div class="flex grow flex-row">
	<div class="basis-1/4 bg-slate-400">
		<TemplateList />
	</div>

	<div class="basis-1/2 bg-slate-600">
		<div class="relative mt-5 md:col-span-2 md:mt-0">
			<h3 class="px-4 py-4 text-center text-2xl font-bold">
				{#if $handlebarStore?.template?.key}{$handlebarStore?.template?.key}{:else}Handlebars Editor
				{/if}
			</h3>
		</div>
	</div>

	<div class="invisible basis-1/4 bg-slate-800 lg:visible">
		<h3 class="px-4 py-4 text-center text-2xl font-bold text-slate-200">Messages</h3>
		<SpinnerWrapper spinnerID="globalSpinner" />
	</div>
</div>
