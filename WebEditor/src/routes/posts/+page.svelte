<script lang="ts">
	import { type TreeViewNode } from '@skeletonlabs/skeleton';
	import { onMount, tick } from 'svelte';
	import { Add, Icon, Refresh } from 'svelte-google-materialdesign-icons';
	import { userStore } from '../../stores/userStore.svelte';
	import PostList from './PostList.svelte';
	import PostListItem from './PostListItem.svelte';
	import { fetchPost, fetchPosts, posts } from './postStore.svelte';
	import ListPlaceholder from '../../components/ListPlaceholder.svelte';
	import { markdownStore } from '../../stores/contentStore.svelte';

	let postListNodes = [] as TreeViewNode[];
	let pgTitle = 'Markdown Editor';

	onMount(async () => {
		if (!$posts) {
			await loadPostList();
		}
	});

	async function loadPostList() {
		if (!$userStore.token) {
			console.log('no token');
			return;
		} else {
			let fetchResult = await fetchPosts($userStore.token);
			if (fetchResult instanceof Error) {
				console.error(fetchResult);
			} else {
			}
		}
	}

	async function initiateLoadPost(srcKey: string) {
		fetchPost(srcKey, $userStore.token!!);
	}

	const userStoreUnsubscribe = userStore.subscribe((value) => {
		if (value) {
			// do nothing
		}
	});

	const postsUnsubscribe = posts.subscribe((value) => {
		if (value) {
			// build TreeViewNodes from PostNodes
			for (const post of value.posts) {
				postListNodes.push({
					id: post.srcKey,
					content: PostListItem,
					contentProps: { title: post.title, date: post.date, srcKey: post.slug }
				});
			}
			postListNodes = [...postListNodes];
		}
	});

	const contentStoreUnsubscribe = markdownStore.subscribe((value) => {
		if (value) {
			if (value.metadata != null) {
				pgTitle = value.metadata.title;
			}
		}
	});
</script>

<div class="flex flex-row grow mt-2 container justify-center">
	<div class="basis-1/4 flex flex-col items-center mr-4">
		{#if $userStore.name}
			<h3 class="h3">Posts</h3>

			<div class="btn-group variant-filled">
				<button><Icon icon={Refresh} />Reload</button>
				<button><Icon icon={Add} />New Post</button>
			</div>
			<div class="flex flex-row m-4">
				{#if $posts?.count === undefined}
					<ListPlaceholder label="Loading posts" rows={5} />
				{:else if $posts?.count === 0}
					<p>No posts</p>
				{:else}
					<span class="text=sm text-secondary-500">{$posts?.count} posts</span>
				{/if}
			</div>
			<div class="card bg-primary-200 w-full">
				<PostList nodes={postListNodes} onClickFn={initiateLoadPost} />
			</div>
		{:else}
			<p class="text-error-500">Not logged in</p>
		{/if}
	</div>

	<div class="basis-3/4 container flex flex-col w-full">
		<h3 class="h3 text-center mb-2">{pgTitle}</h3>
		<!-- form goes here in a grid -->
		<div class="">
			{#if $markdownStore.metadata}
				<form action="#" method="POST">
					<div class="grid grid-cols-6 gap-6">
						<div class="col-span-6 sm:col-span-6 lg:col-span-2">
							<input
								type="text"
								name="slug"
								id="slug"
								disabled
								value={$markdownStore.metadata.slug}
								class="mt-1 focus:ring-primary-500 focus:border-primary-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md text-primary-900"
								placeholder="Slug"
							/>
						</div>
					</div>
				</form>
				{$markdownStore.body}
			{/if}
		</div>
	</div>
</div>
