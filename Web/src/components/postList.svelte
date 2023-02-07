<script lang="ts">
	import { structureStore, postStore } from '../stores/postsStore.svelte';
	import { onDestroy, onMount } from 'svelte';
	import { userStore } from '../stores/userStore.svelte';

	$: postsSorted = $postStore.sort(
		(a, b) => new Date(b.lastUpdated).valueOf() - new Date(a.lastUpdated).valueOf()
	);

	onMount(async () => {});

	function loadStructure(token: String) {
		// https://qs0pkrgo1f.execute-api.eu-west-2.amazonaws.com/prod/
		// https://api.cantilevers.org/structure
		// TODO: extract this sort of thing into a separate method, and add error handling, auth etc
		fetch('https://api.cantilevers.org/structure', {
			method: 'GET',
			headers: {
				Accept: 'application/json',
				Authorization: 'Bearer ' + token
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
	}

	function loadMarkdown(srcKey: String) {
		console.log("Loading markdown file... " + srcKey);
	}

	const userStoreUnsubscribe = userStore.subscribe((data) => {
		if (data) {
			loadStructure(data.token);
		}
	});

	const structStoreUnsubscribe = structureStore.subscribe((data) => {
		postStore.set(data.posts);
	});

	onDestroy(structStoreUnsubscribe);
	onDestroy(userStoreUnsubscribe);
</script>

<h3 class="px-4 py-4 text-2xl font-bold text-slate-900">Posts</h3>

{#if $userStore === undefined}
	<div class="px-8"><p class="text-lg text-warning">Login to see posts</p></div>
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
						<li class="px-6 py-2 border-b border-grey-400 w-full hover:bg-slate-200 cursor-pointer" 
							on:click={ () => loadMarkdown(post.srcKey)}>
							{post.title}
						</li>
					{/each}
				</ul>
			</div>
		{:else}
			<div class="py-4 flex justify-center items-center">
				<div class="bg-gray-200 w-full min-h-screen flex justify-center items-center">
					<svg
						class="w-12 h-12 animate-spin text-indigo-400"
						viewBox="0 0 24 24"
						fill="none"
						xmlns="http://www.w3.org/2000/svg"
					>
						<path
							d="M12 4.75V6.25"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round"
						/>
						<path
							d="M17.1266 6.87347L16.0659 7.93413"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round"
						/>
						<path
							d="M19.25 12L17.75 12"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round"
						/>
						<path
							d="M17.1266 17.1265L16.0659 16.0659"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round"
						/>
						<path
							d="M12 17.75V19.25"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round"
						/>
						<path
							d="M7.9342 16.0659L6.87354 17.1265"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round"
						/>
						<path
							d="M6.25 12L4.75 12"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round"
						/>
						<path
							d="M7.9342 7.93413L6.87354 6.87347"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round"
						/>
					</svg>
				</div>
			</div>
		{/if}
	</div>
{/if}
