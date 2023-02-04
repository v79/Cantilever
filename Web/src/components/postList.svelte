<script lang="ts">
	import { structureStore, postStore } from '../stores/postsStore.svelte';
	import { onDestroy, onMount } from 'svelte';
	import { userStore } from '../stores/userStore.svelte';

	$: postsSorted = $postStore.sort(
		(a, b) => new Date(b.lastUpdated).valueOf() - new Date(a.lastUpdated).valueOf()
	);

	onMount(async () => {
		fetch('https://qs0pkrgo1f.execute-api.eu-west-2.amazonaws.com/prod/structure', {
			method: 'GET',
			headers: {
				Accept: 'application/json'
			},
			mode: 'cors'
		})
			.then((response) => response.json())
			.then((data) => {
				structureStore.set(data.data);
			})
			.catch((error) => {
				console.log(error);
				return {};
			});
	});

	const structStoreUnsubscribe = structureStore.subscribe((data) => {
		postStore.set(data.posts);
	});

	onDestroy(structStoreUnsubscribe);
</script>

<h3 class="px-4 py-4 text-2xl font-bold text-slate-900">Posts</h3>

{#if $userStore === undefined}
<div class="px-8">	<p class="text-lg text-warning">Login to see posts</p>
	</div>
{:else}
	<div class="px-8 btn-group lg:btn-group-horizontal">
		<button class="btn" disabled>Another</button>
		<button class="btn" disabled>Something</button>
		<button class="btn btn-active">New Post</button>
	</div>
	<div class="px-8">
		{#if $structureStore}
			<h4 class="text-sm text-slate-900 text-right">{$structureStore.postCount} posts</h4>
		{/if}
		{#if postsSorted.length > 0}
			<div class="py-2 flex justify-left">
				<ul class="bg-white rounded-lg border border-gray-400 w-96 text-slate-900">
					{#each postsSorted as post}
						{@const postDateString = new Date(post.date).toLocaleDateString('en-GB')}
						<li class="px-6 py-2 border-b border-grey-400 w-full hover:bg-slate-400 cursor-pointer">
							{post.title}
						</li>
					{/each}
				</ul>
			</div>
		{:else}
			<div class="py-4 flex justify-center items-center">
				<div
					class="spinner-border animate-spin inline-block w-8 h-8 border-4 rounded-full text-slate-900"
					role="status"
				/>
				<span class="px-4 text-slate-900">Loading...</span>
			</div>
		{/if}
	</div>
{/if}
