<script lang="ts">
	import BasicFileList from '$lib/components/BasicFileList.svelte';
	import PostListItem from '$lib/components/FileListItem.svelte';
	import ListPlaceholder from '$lib/components/ListPlaceholder.svelte';
	import DatePicker from '$lib/forms/datePicker.svelte';
	import MarkdownEditor from '$lib/forms/markdownEditor.svelte';
	import TextInput from '$lib/forms/textInput.svelte';
	import { MarkdownContent, PostItem } from '$lib/models/markdown';
	import { CLEAR_MARKDOWN, markdownStore } from '$lib/stores/contentStore.svelte';
	import { project } from '$lib/stores/projectStore.svelte';
	import { userStore } from '$lib/stores/userStore.svelte';
	import {
		getModalStore,
		getToastStore,
		type ToastSettings,
		type TreeViewNode
	} from '@skeletonlabs/skeleton';
	import { onMount, tick } from 'svelte';
	import { Add, Delete, Icon, Refresh, Save } from 'svelte-google-materialdesign-icons';
	import { deletePost, fetchPost, fetchPosts, posts, savePost } from '../../lib/stores/postStore.svelte';

	const modalStore = getModalStore();
	const toastStore = getToastStore();

	$: webPageTitle = $markdownStore.metadata?.title ? ' - ' + $markdownStore.metadata?.title : '';

	let postListNodes = [] as TreeViewNode[]; // for the treeview component
	let pgTitle: string;
	$: markdownTitle = $markdownStore.metadata?.title ?? 'Untitled';
	$: postIsValid = $markdownStore.metadata?.title != null && $markdownStore.metadata?.title != '';
	let isNewPost = false;

	// define modals
	/**
	 * @type: {ModalSettings}
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
			modalStore.close();
		}
	};

	/**
	 * @type: {ModalSettings}
	 */
	$: deletePostModal = {
		type: 'component',
		component: 'confirmDeleteModal',
		meta: {
			modalTitle: 'Confirm post deletion',
			itemKey: $markdownStore.metadata?.srcKey ?? 'unknown',
			onFormSubmit: () => {
				initiateDeletePost();
			}
		}
	};

	/**
	 * @type: {ModalSettings}
	 */
	$: saveNewPostModal = {
		type: 'component',
		component: 'saveNewPostModal',
		meta: {
			modalTitle: 'Save new post',
			postTitle: markdownTitle,
			templateKey: $markdownStore.metadata?.templateKey ?? 'unknown',
			onFormSubmit: () => {
				initiateSavePost();
			}
		}
	};

	/**
	 * @type: {ModalSettings}
	 */
	$: createNewPostModal = {
		type: 'component',
		component: 'createNewPostModal',
		meta: {
			modalTitle: 'Create new post',
			onFormSubmit: () => {
				initiateNewPost();
			}
		}
	};

	const toast: ToastSettings = {
		message: 'Loaded posts',
		background: 'variant-filled-success',
		hideDismiss: true
	};
	const errorToast: ToastSettings = {
		message: 'Failed to load posts',
		background: 'variant-filled-error'
	};

	onMount(async () => {
		if (posts.isEmpty()) {
			await loadPostList();
		}
	});

	async function loadPostList() {
		if (!$userStore.token || $project.domain === '') {
			console.log('no token or domain');
			return;
		} else {
			let fetchResult = await fetchPosts($userStore.token, $project.domain);
			if (fetchResult instanceof Error) {
				errorToast.message = 'Failed to load posts. Message was: ' + fetchResult.message;
				toastStore.trigger(errorToast);
				console.error(fetchResult);
			} else {
				toast.message = 'Loaded ' + fetchResult + ' posts';
				toastStore.trigger(toast);
			}
		}
	}

	async function initiateLoadPost(srcKey: string) {
		let loadResponse = fetchPost(srcKey, $userStore.token!!, $project.domain);
		loadResponse.then((r) => {
			if (r instanceof Error) {
				errorToast.message = 'Failed to load post';
				toastStore.trigger(errorToast);
			} else {
				toast.message = r;
				toastStore.trigger(toast);
				isNewPost = false;
			}
		});
	}

	function initiateNewPost() {
		let newPost = new MarkdownContent(
			new PostItem('', '', 'sources/templates/post.html.hbs', '', new Date(), new Date(), true),
			''
		);
		markdownStore.set(newPost);
		isNewPost = true;
	}

	async function initiateSavePost() {
		if ($markdownStore.metadata) {
			let metadata = $markdownStore.metadata;
			if (isNewPost || metadata.srcKey === '') {
				console.log('Is a new post / srcKey is blank, so setting it to the slug', metadata.slug);
				metadata.srcKey = $project.domain + '/sources/posts/' + metadata.slug + '.md';
				$markdownStore.metadata = metadata;
			}
			let saveResult = await savePost(metadata?.srcKey, $userStore.token!!, $project.domain);
			if (saveResult instanceof Error) {
				errorToast.message = 'Failed to save post';
				toastStore.trigger(errorToast);
			} else {
				console.log('postStore: Saved post', metadata.srcKey);
				isNewPost = false;
				toast.message = saveResult;
				toastStore.trigger(toast);
				reloadPostList();
			}
		}
	}

	async function initiateDeletePost() {
		console.log('Deleting post');
		if ($markdownStore.metadata) {
			let deleteResult = deletePost(
				$markdownStore.metadata.srcKey,
				$userStore.token!!,
				$project.domain
			);
			deleteResult.then((r) => {
				if (r instanceof Error) {
					errorToast.message = 'Failed to delete post';
					toastStore.trigger(errorToast);
				} else {
					toast.message = 'Deleted post ' + r;
					markdownStore.set(CLEAR_MARKDOWN);
					toastStore.trigger(toast);
					reloadPostList();
				}
			});
		}
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

<svelte:head>
	<title>Cantilever: Blog Posts {webPageTitle}</title>
</svelte:head>

<div class="flex flex-row grow mt-2 container justify-center">
	<div class="basis-1/4 flex flex-col items-center mr-4">
		{#if $userStore.isLoggedIn()}
			<h3 class="h3 mb-2">Posts</h3>

			<div class="btn-group variant-filled">
				<button class="variant-filled-secondary" on:click={reloadPostList} title="Reload post list"
					><Icon icon={Refresh} />Reload</button>
				<button
					class="variant-filled-primary"
					on:click={(e) => modalStore.trigger(createNewPostModal)}
					title="Create new post"><Icon icon={Add} />New Post</button>
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
					<BasicFileList nodes={postListNodes} onClickFn={initiateLoadPost} />
				</div>
			{/if}
		{:else}
			<p class="text-error-500">Not logged in</p>
		{/if}
	</div>

	<div class="basis-3/4 container flex flex-col w-full">
		<h3 class="h3 text-center mb-2">
			{#if pgTitle}{pgTitle}{/if}
		</h3>
		{#if $markdownStore.metadata}
			<div class="flex flex-row justify-end">
				<div class="btn-group variant-filled" role="group">
					<button
						class=" variant-filled-error"
						title="Delete post"
						disabled={isNewPost}
						on:click={(e) => {
							modalStore.trigger(deletePostModal);
						}}><Icon icon={Delete} />Delete</button>
					<button
						disabled={!postIsValid}
						title="Save and regenerate post"
						class=" variant-filled-primary"
						on:click={(e) => {
							if (isNewPost) {
								modalStore.trigger(saveNewPostModal);
							} else {
								modalStore.trigger(savePostModal);
							}
						}}>Save<Icon icon={Save} /></button>
				</div>
			</div>
			<div class="grid grid-cols-6 gap-6">
				<div class="col-span-6 sm:col-span-6 lg:col-span-2">
					{#if isNewPost}
						<p><em>Slug will be set on first save</em></p>
					{:else}
						<TextInput
							label="Slug"
							name="slug"
							bind:value={$markdownStore.metadata.slug}
							required
							readonly />
					{/if}
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
						readonly />
				</div>
				<div class="col-span-6">
					<TextInput
						bind:value={$markdownStore.metadata.title}
						required
						name="postTitle"
						label="Title" />
				</div>
				<div class="col-span-6">
					<label for="markdown" class="label"
						><span>Markdown</span> <code>{$markdownStore.metadata.srcKey}</code></label>
					<MarkdownEditor bind:body={$markdownStore.body} />
				</div>
			</div>
			<div class="flex flex-row justify-end mt-2">
				<div class="btn-group variant-filled" role="group">
					<button
						class="variant-filled-primary"
						disabled={!postIsValid}
						title="Save and regenerate post"
						on:click={(e) => {
							if (isNewPost) {
								modalStore.trigger(saveNewPostModal);
							} else {
								modalStore.trigger(savePostModal);
							}
						}}>Save<Icon icon={Save} /></button>
				</div>
			</div>
		{/if}
	</div>
</div>
