<script lang="ts">
	import PostList from '../components/postList.svelte';
	import MarkdownEditor from '../components/MarkdownEditor/markdownEditor.svelte';
	import SpinnerWrapper from '../components/utilities/spinnerWrapper.svelte';
	import { AS_CLEAR, activeStore } from '../stores/appStatusStore.svelte';
	import { afterNavigate } from '$app/navigation';
	import ActiveStoreView from '../components/activeStoreView.svelte';

	afterNavigate(() => {
		activeStore.set(AS_CLEAR);
		$activeStore.currentPage = 'Posts';
	});
</script>

<div class="flex grow flex-row">
	<div class="basis-1/4 bg-slate-400">
		<PostList />
	</div>
	<div class="basis-1/2 bg-slate-600">
		<MarkdownEditor />
	</div>
	<div class="invisible basis-1/4 bg-slate-800 lg:visible">
		<ActiveStoreView />
		<SpinnerWrapper spinnerID="globalSpinner" />
	</div>
</div>

<style lang="postcss">
	:global(html) {
		background-color: theme(colors.gray.200);
	}
</style>
