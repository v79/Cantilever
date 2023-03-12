<script lang="ts">
    import {onDestroy, onMount} from 'svelte';
    import type {MarkdownPost} from '../models/structure';
    import {activeStore} from '../stores/appStatusStore.svelte';
    import {markdownStore} from '../stores/markdownPostStore.svelte';
    import {notificationStore} from '../stores/notificationStore.svelte';
    import {allPostsStore, postStore} from '../stores/postsStore.svelte';
    import {userStore} from '../stores/userStore.svelte';
    import PostListItem from './postListItem.svelte';
    import {spinnerStore} from './utilities/spinnerWrapper.svelte';

    $: postsSorted = $postStore.sort(
		(a, b) => new Date(b.lastUpdated).valueOf() - new Date(a.lastUpdated).valueOf()
	);

	onMount(async () => {});

	function loadAllPosts() {
		// https://qs0pkrgo1f.execute-api.eu-west-2.amazonaws.com/prod/
		// https://api.cantilevers.org/structure
		// TODO: extract this sort of thing into a separate method, and add error handling, auth etc
		// spinnerStore.set({ message: 'Loading project structure', shown: true });
		$spinnerStore.message = 'Loading project structure';
		$spinnerStore.shown = true;
		console.log('Loading all posts json...');
		let token = $userStore.token;
		fetch('https://api.cantilevers.org/project/posts', {
			method: 'GET',
			headers: {
				Accept: 'application/json',
				Authorization: 'Bearer ' + token
			},
			mode: 'cors'
		})
			.then((response) => response.json())
			.then((data) => {
				if (data.data === undefined) {
					throw new Error(data.message);
				}
				allPostsStore.set(data.data);
				$notificationStore.message = 'Loaded all posts ' + $activeStore.activeFile;
				$notificationStore.shown = true;
			})
			.catch((error) => {
				console.log(error);
				notificationStore.set({
					message: error,
					shown: true,
					type: 'error'
				});
				return {};
			});
		$spinnerStore.shown = false;
	}

	function loadMarkdown(srcKey: string) {
		let token = $userStore.token;
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
				if (data.data === undefined) {
					throw new Error(data.message);
				}
				markdownStore.set(data.data);
				activeStore.set({
					activeFile: decodeURIComponent($markdownStore.post.srcKey),
					isNewFile: false,
					hasChanged: false,
					isValid: true,
					newSlug: $markdownStore.post.url
				});
				$notificationStore.message = 'Loaded file ' + $activeStore.activeFile;
				$notificationStore.shown = true;
			})
			.catch((error) => {
				console.log(error);
				notificationStore.set({
					message: error,
					shown: true,
					type: 'error'
				});
			});
		$spinnerStore.shown = false;
	}

	function rebuild() {
		let token = $userStore.token;
		console.log('Regenerating project posts file...');
		fetch('https://api.cantilevers.org/project/posts/rebuild', {
			method: 'PUT',
			headers: {
				Accept: 'application/json',
				Authorization: 'Bearer ' + token,
				'Content-Length': '0'
			},
			body: '1',
			mode: 'cors'
		})
			.then((response) => response.json())
			.then((data) => {
				console.log(data);
				notificationStore.set({
					message: data.data,
					shown: true,
					type: 'success'
				});
			})
			.catch((error) => {
				console.log(error);
				notificationStore.set({
					message: error,
					shown: true,
					type: 'error'
				});
			});
		$spinnerStore.shown = false;
		loadAllPosts();
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
				templateKey: 'post'
			}
		};
		activeStore.set({
			activeFile: '',
			isNewFile: true,
			hasChanged: false,
			isValid: false,
			newSlug: ''
		});
		console.log('Creating new post');
		markdownStore.set(newMDPost);
	}

	const userStoreUnsubscribe = userStore.subscribe((data) => {
		if (data) {
			loadAllPosts();
		}
	});

	const structStoreUnsubscribe = allPostsStore.subscribe((data) => {
		postStore.set(data.posts);
	});

	onDestroy(structStoreUnsubscribe);
	onDestroy(userStoreUnsubscribe);
</script>

<h3 class="px-4 py-4 text-center text-2xl font-bold text-slate-900">Posts</h3>

{#if $userStore === undefined}
	<div class="px-8"><p class="text-warning text-lg">Login to see posts</p></div>
{:else}
	<div class="flex items-center justify-center" role="group">
		<button type="button" on:click={(e) => ($spinnerStore.shown = !$spinnerStore.shown)}
			>Toggle Spinner</button>
		<button
			class="inline-block rounded-l bg-purple-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
			on:click={(e) => {
				console.log('Show spinner');
				spinnerStore.update((m) => {
					m.message = 'Rebuilding project...';
					m.shown = true;
					return m;
				});
				spinnerStore.set({ shown: true, message: 'Rebuilding project...' });
				// $spinnerStore.message = 'Rebuilding project...';
				// $spinnerStore.shown = true;

				rebuild();
			}}>Rebuild</button>
		<button
			class="inline-block bg-purple-800 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
			on:click={(e) => {
				$spinnerStore.message = 'Reloading project...';
				$spinnerStore.shown = true;
				loadAllPosts();
			}}>Reload</button>
		<button
			type="button"
			on:click={createNewPost}
			class="inline-block rounded-r bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800"
			>New Post</button>
	</div>
	<div class="px-8">
		{#if $allPostsStore}
			<h4 class="text-right text-sm text-slate-900">{$allPostsStore.count} posts</h4>
		{/if}
		{#if postsSorted.length > 0}
			<div class="justify-left flex py-2">
				<ul class="w-96 rounded-lg border border-gray-400 bg-white text-slate-900">
					{#each postsSorted as post}
						<PostListItem {post} onClickFn={loadMarkdown} />
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
