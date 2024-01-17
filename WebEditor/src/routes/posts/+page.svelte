<script lang="ts">
	import { type TreeViewNode } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { Add, Icon, Refresh } from 'svelte-google-materialdesign-icons';
	import { userStore } from '../../stores/userStore.svelte';
	import PostList from './PostList.svelte';
	import PostListItem from './PostListItem.svelte';
	import { fetchPosts, posts } from './postStore.svelte';

	let postListNodes = [] as TreeViewNode[];

	onMount(async () => {
		if (!$posts) {
			console.log('no posts');
			await loadPosts();
		}
		console.log('posts', $posts);
	});

	async function loadPosts() {
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
		console.log('initiateLoadPost', srcKey);
	}

	const userStoreUnsubscribe = userStore.subscribe((value) => {
		if (value) {
			// do nothing
		}
	});

	const postsUnsubscribe = posts.subscribe((value) => {
		if (value) {
			console.dir(value);
			// build TreeViewNodes from PostNodes
			for (const post of value.posts) {
				postListNodes.push({
					id: post.srcKey,
					content: PostListItem,
					contentProps: { title: post.title, date: post.date, srcKey: post.slug }
				});
			}
		}
	});
</script>

<section class="flex flex-row grow mt-2 container justify-center">
	<div class="basis-1/4 flex flex-col items-center mr-4">
		{#if $userStore.name}
		<h3 class="h3">Posts</h3>

		<div class="btn-group variant-filled">
			<button><Icon icon={Refresh} />Reload</button>
			<button><Icon icon={Add} />New Post</button>
		</div>
		<div class="flex flex-row m-4">
			{#if $posts}
			<span class="text=sm text-secondary-500">{$posts?.count} posts</span>
			{/if}
		</div>
		<div class="card bg-primary-200 w-full">
			<PostList nodes={postListNodes} onClickFn={initiateLoadPost}/>
		</div>
		{:else}
		<div class="placeholder"></div>
		{/if}
	</div>

	<div class="basis-2/3 container flex flex-row justify-center">
		<h3 class="h3">Markdown Editor</h3>
	</div>

	<div class="basis-1/6 border-yellow-500 border flex flex-row justify-center">
		<h3 class="h3">Notes</h3>
	</div>
</section>
