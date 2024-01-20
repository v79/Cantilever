<script lang="ts">
	import { getModalStore, type ModalSettings, type TreeViewNode } from '@skeletonlabs/skeleton';
	import { onMount, tick } from 'svelte';
	import { Add, Delete, Icon, Refresh, Save } from 'svelte-google-materialdesign-icons';
	import ListPlaceholder from '../../components/ListPlaceholder.svelte';
	import DatePicker from '../../components/forms/datePicker.svelte';
	import MarkdownEditor from '../../components/forms/markdownEditor.svelte';
	import TextInput from '../../components/forms/textInput.svelte';
	import { markdownStore } from '../../stores/contentStore.svelte';
	import { userStore } from '../../stores/userStore.svelte';
	import PostList from './PostList.svelte';
	import PostListItem from './PostListItem.svelte';
	import { fetchPost, fetchPosts, posts } from './postStore.svelte';
	import { MarkdownContent, PostItem } from '../../models/markdown';

	const modalStore = getModalStore();
	let postListNodes = [] as TreeViewNode[];
	let pgTitle = 'Markdown Editor';
	$: markdownTitle = $markdownStore.metadata?.title ?? 'Untitled';
	$: postIsValid = $markdownStore.metadata?.title != null && $markdownStore.metadata?.title != '';

	// define modals
	/**
	 * @type: {ModalComponent}
	 */
	$: savePostModal = {
		type: 'confirm',
		title: 'Confirm save',
		body: "Save changes to post '<strong>" + markdownTitle + "</strong>'?",
		buttonTextConfirm: 'Save',
		buttonTextCancel: 'Cancel',
		// TRUE if confirm pressed, FALSE if cancel pressed
		response: (r: boolean) => {
			if (r) {
				initiateSavePost();
			}
		}
	};

	/**
	 * @type: {ModalComponent}
	 */
	$: deletePostModal = {
		type: 'component',
		component: 'confirmPostDeleteModal',
		meta: {
			modalTitle: 'Confirm post deletion',
			itemTitle: markdownTitle,
			onFormSubmit: () => {
				initiateDeletePost();
			}
		}
	};

	const newPostModal: ModalSettings = {
		type: 'confirm',
		title: 'Create new post',
		body: 'Create a new post?',
		response: (r: boolean) => {
			if (r) {
				initiateNewPost();
			}
		}
	};

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
				console.log('postStore: Fetched', fetchResult, 'posts');
			}
		}
	}

	async function initiateLoadPost(srcKey: string) {
		fetchPost(srcKey, $userStore.token!!);
	}

	function initiateNewPost() {
		let newPost = new MarkdownContent(
			new PostItem('', '', 'sources/templates/post.html.hbs', '', new Date(), new Date(), true),
			''
		);
		markdownStore.set(newPost);
		console.dir($markdownStore.metadata);
	}

	async function initiateSavePost() {
		console.log('save post (not yet)');
	}

	async function initiateDeletePost() {
		console.log('delete post (not yet)');
	}

	async function reloadPostList() {
		postListNodes = [];
		posts.set({ count: -1, posts: [] });
		tick();
		await loadPostList();
	}

	const userStoreUnsubscribe = userStore.subscribe((value) => {
		if (value) {
			// do nothing
		}
	});

	const postsUnsubscribe = posts.subscribe((value) => {
		if (value && value.count != -1) {
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
				<button on:click={reloadPostList}><Icon icon={Refresh} />Reload</button>
				<button on:click={(e) => modalStore.trigger(newPostModal)}
					><Icon icon={Add} />New Post</button
				>
			</div>
			<div class="flex flex-row m-4">
				{#if $posts?.count === undefined || $posts?.count === -1}
					<ListPlaceholder label="Loading posts" rows={5} />
				{:else if $posts?.count === 0}
					<p>No posts</p>
				{:else}
					<span class="text=sm text-secondary-500">{$posts?.count} posts</span>
				{/if}
			</div>
			{#if $posts?.count > 0}
				<div class="card bg-primary-200 w-full">
					<PostList nodes={postListNodes} onClickFn={initiateLoadPost} />
				</div>
			{/if}
		{:else}
			<p class="text-error-500">Not logged in</p>
		{/if}
	</div>

	<div class="basis-3/4 container flex flex-col w-full">
		<h3 class="h3 text-center mb-2">{pgTitle}</h3>
		{#if $markdownStore.metadata}
			<div class="flex flex-row justify-end">
				<div class="btn-group variant-filled" role="group">
					<button
						class=" variant-filled-error"
						on:click={(e) => {
							modalStore.trigger(deletePostModal);
						}}><Icon icon={Delete} />Delete</button
					>
					<button
						disabled={!postIsValid}
						class=" variant-filled-primary"
						on:click={(e) => {
							modalStore.trigger(savePostModal);
						}}>Save<Icon icon={Save} /></button
					>
				</div>
			</div>
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
					<DatePicker label="Date" name="date" required bind:value={$markdownStore.metadata.date} />
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
						name="postTitle"
						label="Title"
					/>
				</div>
				<div class="col-span-6">
					<label for="markdown" class="label"><span>Markdown</span></label>
					<MarkdownEditor bind:body={$markdownStore.body} />
				</div>
			</div>
			<div class="flex flex-row justify-end mt-2">
				<div class="btn-group variant-filled" role="group">
					<button
						class=" variant-filled-primary"
						disabled={!postIsValid}
						on:click={(e) => {
							modalStore.trigger(savePostModal);
						}}>Save<Icon icon={Save} /></button
					>
				</div>
			</div>
		{/if}
	</div>
</div>
