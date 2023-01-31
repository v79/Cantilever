<script lang="ts">
	import { structureStore, postStore } from '../stores/postsStore.svelte';
	import { onDestroy, onMount } from 'svelte';

	$: postsSorted = $postStore.sort(
		(a, b) => new Date(b.lastUpdated).valueOf() - new Date(a.lastUpdated).valueOf()
	);

	onMount(async () => {
		fetch('https://h2ezadb0cl.execute-api.eu-west-2.amazonaws.com/prod/structure', {
			method: 'GET',
			headers: {
				Accept: 'application/json'
			},
			mode: 'cors'
		})
			.then((response) => response.json())
			.then((data) => {
				console.log(data);
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

{#if $structureStore}
	<h4 class="text-sm text-slate-900">{$structureStore.postCount} posts</h4>
{/if}
<button class="btn">New Post</button>
<ol class="list-decimal pl-10">
	{#each postsSorted as post}
		{@const postDateString = new Date(post.date).toLocaleDateString('en-GB')}
		<li class="text-slate-900">
			{post.title}
		</li>
	{:else}
		<span class="animate-bounce">Loading...</span>
	{/each}
</ol>
