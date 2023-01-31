<script>
	import PostList from '../components/postList.svelte';
	import TopMenu from '../components/topMenu.svelte';
	import Footer from '../components/footer.svelte';
	import MarkdownEditor from '../components/markdownEditor.svelte';
</script>

<TopMenu/>

<div class="flex flex-row">
	<div class="basis-1/4 bg-slate-400">
		<h3 class="text-xl text-slate-900">Posts</h3>
		<PostList />
	</div>
	<div class="divider divider-horizontal"></div>
	<div class="basis-1/2 bg-slate-600">
		<h3 class="text-xl">Markdown Editor</h3>
		<MarkdownEditor/>
	</div>
	<div class="divider divider-horizontal"></div>
	<div class="basis-1/4 bg-slate-800"><h3 class="text-lg text-slate-200">Tools</h3></div>
</div>

<Footer/>

<style lang="postcss">
	:global(html) {
		background-color: theme(colors.gray.200);
	}
</style>
