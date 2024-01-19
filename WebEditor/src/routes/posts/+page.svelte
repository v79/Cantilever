<script lang="ts">
	import { type TreeViewNode } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { Add, Icon, Refresh } from 'svelte-google-materialdesign-icons';
	import { userStore } from '../../stores/userStore.svelte';
	import PostList from './PostList.svelte';
	import PostListItem from './PostListItem.svelte';
	import { fetchPost, fetchPosts, posts } from './postStore.svelte';
	import ListPlaceholder from '../../components/ListPlaceholder.svelte';
	import { markdownStore } from '../../stores/contentStore.svelte';
	import TextInput from '../../components/forms/textInput.svelte';
	import DatePicker from '../../components/forms/datePicker.svelte';
	import MarkdownEditor from '../../components/forms/markdownEditor.svelte';

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
							<TextInput
								label="Slug"
								name="slug"
								bind:value={$markdownStore.metadata.slug}
								required
								readonly
							/>
						</div>
						<div class="col-span-6 sm:col-span-3 lg:col-span-2">
							<DatePicker
								label="Date"
								name="date"
								required
								bind:value={$markdownStore.metadata.date}
							/>
						</div>
						<div class="col-span-6 sm:col-span-3 lg:col-span-2">
							<TextInput
								bind:value={$markdownStore.metadata.templateKey}
								name="template"
								label="Template"
								required
								readonly
							/>
						</div>
						<div class="col-span-6">
							<TextInput
								bind:value={$markdownStore.metadata.title}
								required
								name="Title"
								label="Title"
							/>
						</div>
						<div class="col-span-6">
							<label for="markdown" class="label"><span>Markdown</span></label>
							<MarkdownEditor bind:body={$markdownStore.body} />
						</div>
					</div>
				</form>
			{/if}
		</div>
	</div>
</div>
