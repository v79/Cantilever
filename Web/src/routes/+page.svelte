<script lang="ts">
	import PostList from '../components/postList.svelte';
	import TopMenu from '../components/topMenu.svelte';
	import Footer from '../components/footer.svelte';
	import MarkdownEditor from '../components/markdownEditor.svelte';
</script>

<TopMenu />

<div class="flex flex-row">
	<div class="basis-1/4 bg-slate-400">
		<PostList />
	</div>
	<div class="basis-1/2 bg-slate-600">
		<MarkdownEditor />
	</div>
	<div class="invisible basis-1/4 bg-slate-800 lg:visible">
		<h3 class="px-4 py-4 text-center text-2xl font-bold text-slate-200">Assets</h3>
	</div>
</div>

<Footer />

<style lang="postcss">
	:global(html) {
		background-color: theme(colors.gray.200);
	}
</style>
