<script lang="ts">
	import { Add, Icon, Refresh } from 'svelte-google-materialdesign-icons';
	import { RecursiveTreeView, type TreeViewNode } from '@skeletonlabs/skeleton';
	import PostListItem from './PostListItem.svelte';

	let postList: TreeViewNode[] = [
		{
			id: 'post-1',
			content: PostListItem,
			contentProps: { title: 'New Design', date: '2024-01-01', srcKey: 'new-design' }
		},
		{
			id: 'post-2',
			content: PostListItem,
			contentProps: { title: 'UI Concerns', date: '2023-01-01', srcKey: 'ui-concerns' }
		},
		{
			id: 'post-3',
			content: PostListItem,
			contentProps: { title: 'Success with images', date: '2022-01-01', srcKey: 'image-success' }
		}
	];
</script>

<section class="flex flex-row grow mt-2 container justify-center">
	<div class="basis-1/4 flex flex-col items-center mr-4">
		<h3 class="h3">Posts</h3>

		<div class="btn-group variant-filled">
			<button><Icon icon={Refresh} />Reload</button>
			<button><Icon icon={Add} />New Post</button>
		</div>
		<div class="flex flex-row m-4">
			<span class="text=sm text-secondary-500">{postList.length} posts</span>
		</div>
		<div class="card bg-primary-200 w-full">
			<RecursiveTreeView nodes={postList} spacing="space-y-0" padding="py-2 px-2" />
		</div>
	</div>

	<div class="basis-2/3 container flex flex-row justify-center">
		<h3 class="h3">Markdown Editor</h3>
	</div>

	<div class="basis-1/6 border-yellow-500 border flex flex-row justify-center">
		<h3 class="h3">Notes</h3>
	</div>
</section>
