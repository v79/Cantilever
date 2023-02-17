<script lang="ts">
	import { markdownStore, postStore, structureStore } from '../stores/postsStore.svelte';
	import { onDestroy, onMount } from 'svelte';
	import { userStore } from '../stores/userStore.svelte';
	import type { MarkdownPost } from '../models/structure';
	import Spinner from './utilities/spinner.svelte';
	import { notifier } from '@beyonk/svelte-notifications';
	let spinnerActive = false;
	let loadedFile = '';

	$: postsSorted = $postStore.sort(
		(a, b) => new Date(b.lastUpdated).valueOf() - new Date(a.lastUpdated).valueOf()
	);

	onMount(async () => {});

	function loadStructure(token: String) {
		// https://qs0pkrgo1f.execute-api.eu-west-2.amazonaws.com/prod/
		// https://api.cantilevers.org/structure
		// TODO: extract this sort of thing into a separate method, and add error handling, auth etc
		spinnerActive = true;
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
		spinnerActive = false;
	}

	function loadMarkdown(srcKey: string) {
		let token = $userStore.token;
		spinnerActive = true;
		console.log('Loading markdown file... ' + srcKey);

		fetch('https://api.cantilevers.org/posts/load/' + encodeURIComponent(srcKey), {
			method: 'GET',
			headers: {
				Accept: 'application/json',
				Authorization: 'Bearer ' + token
			},
			mode: 'cors'
		})
			.then((response) => response.json())
			.then((data) => {
				markdownStore.set(data.data);
				console.log(data);
				loadedFile = decodeURIComponent($markdownStore.post.srcKey);
				notifier.success('Loaded file ' + loadedFile, 0, { showProgress: false });
			})
			.catch((error) => {
				console.log(error);
			});
		spinnerActive = false;
	}

	function createNewPost() {
		var newMDPost: MarkdownPost = {
			body: '',
			post: {
				title: '',
				srcKey: '',
				url: '',
				date: '',
				lastUpdated: '',
				template: {
					key: 'post',
					lastUpdated: ''
				}
			}
		};
		loadedFile = '';
		console.log('Creating new post');
		markdownStore.set(newMDPost);
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

<Spinner spinnerId="load-spinner" message="Loading..." shown={spinnerActive} />

<h3 class="px-4 py-4 text-center text-2xl font-bold text-slate-900">Posts</h3>

{#if $userStore === undefined}
	<div class="px-8"><p class="text-warning text-lg">Login to see posts</p></div>
{:else}
	<div class="flex items-center justify-center" role="group">
		<button
			class="inline-block rounded-l bg-purple-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
			disabled>Another</button>
		<button
			class="inline-block bg-purple-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
			disabled>Something</button>
		<button
			type="button"
			on:click={createNewPost}
			class="inline-block rounded-r bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
			>New Post</button>
	</div>
	<div class="px-8">
		{#if $structureStore}
			<h4 class="text-right text-sm text-slate-900">{$structureStore.postCount} posts</h4>
		{/if}
		{#if postsSorted.length > 0}
			<div class="justify-left flex py-2">
				<ul class="w-96 rounded-lg border border-gray-400 bg-white text-slate-900">
					{#each postsSorted as post}
						{@const postDateString = new Date(post.date).toLocaleDateString('en-GB')}

						<li
							id={post.srcKey}
							class="border-grey-400 w-full cursor-pointer border-b px-6 py-2 hover:bg-slate-200 {loadedFile ===
							post.srcKey
								? 'bg-slate-100'
								: ''} "
							on:keyup={() => loadMarkdown(post.srcKey)}
							on:click={() => loadMarkdown(post.srcKey)}>
							{post.title}
						</li>
					{/each}
				</ul>
			</div>
		{:else}
			<div class="flex items-center justify-center py-4">
				<div class="flex min-h-screen w-full items-center justify-center bg-gray-200">
					<svg
						class="h-12 w-12 animate-spin text-indigo-400"
						viewBox="0 0 24 24"
						fill="none"
						xmlns="http://www.w3.org/2000/svg">
						<path
							d="M12 4.75V6.25"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M17.1266 6.87347L16.0659 7.93413"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M19.25 12L17.75 12"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M17.1266 17.1265L16.0659 16.0659"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M12 17.75V19.25"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M7.9342 16.0659L6.87354 17.1265"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M6.25 12L4.75 12"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
						<path
							d="M7.9342 7.93413L6.87354 6.87347"
							stroke="currentColor"
							stroke-width="1.5"
							stroke-linecap="round"
							stroke-linejoin="round" />
					</svg>
				</div>
			</div>
		{/if}
	</div>
{/if}
