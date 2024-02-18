<script lang="ts">
	import {
		AppBar,
		AppRail,
		AppRailAnchor,
		AppShell,
		ListBox,
		ListBoxItem,
		Modal,
		Toast,
		getToastStore,
		initializeStores,
		popup,
		type ModalComponent,
		type PopupSettings,
		type ToastSettings
	} from '@skeletonlabs/skeleton';
	import '../app.postcss';
	// Highlight JS
	import 'highlight.js/styles/github-dark.css';
	import { page } from '$app/stores';
	import LoginAvatar from '$lib/components/LoginAvatar.svelte';
	import { userStore } from '$lib/stores/userStore.svelte';
	import { arrow, autoUpdate, computePosition, flip, offset, shift } from '@floating-ui/dom';
	import { storePopup } from '@skeletonlabs/skeleton';
	import {
		Article,
		Dataset_linked,
		Document_scanner,
		Feed,
		Home,
		Icon,
		Perm_media,
		Settings_applications,
		Sync
	} from 'svelte-google-materialdesign-icons';
	storePopup.set({ computePosition, autoUpdate, flip, shift, offset, arrow });

	import { beforeNavigate } from '$app/navigation';
	import ConfirmDeleteModal from '$lib/modals/confirmDeleteModal.svelte';
	import CreateNewFolderModal from '$lib/modals/createNewFolderModal.svelte';
	import CreateNewPageModal from '$lib/modals/createNewPageModal.svelte';
	import SaveNewPageModal from '$lib/modals/saveNewPageModal.svelte';
	import SaveNewPostModal from '$lib/modals/saveNewPostModal.svelte';
	import SaveNewProjectModal from '$lib/modals/saveNewProjectModal.svelte';
	import SaveNewTemplateModal from '$lib/modals/saveNewTemplateModal.svelte';
	import SwitchIndexPageModal from '$lib/modals/switchIndexPageModal.svelte';
	import { handlebars, markdownStore } from '$lib/stores/contentStore.svelte';
	import { project } from '$lib/stores/projectStore.svelte';
	import {
		rebuildAllMetadata,
		rebuildAllPages,
		rebuildAllPosts
	} from '$lib/stores/regenStore.svelte';
	import SpinnerStore, { spinner } from '$lib/stores/spinnerStore.svelte';
	import { onMount } from 'svelte';
	import ExpandMore from 'svelte-google-materialdesign-icons/Expand_more.svelte';

	const modalRegistry: Record<string, ModalComponent> = {
		confirmDeleteModal: { ref: ConfirmDeleteModal },
		saveNewPostModal: { ref: SaveNewPostModal },
		saveNewTemplateModal: { ref: SaveNewTemplateModal },
		createNewPageModal: { ref: CreateNewPageModal },
		createNewFolderModal: { ref: CreateNewFolderModal },
		switchIndexPageModal: { ref: SwitchIndexPageModal },
		saveNewPageModal: { ref: SaveNewPageModal },
		createNewProjectModal: { ref: SaveNewProjectModal }
	};
	initializeStores();

	const toastStore = getToastStore();
	const toast: ToastSettings = {
		message: 'Loaded posts',
		background: 'variant-filled-success',
		hideDismiss: true
	};
	const errorToast: ToastSettings = {
		message: 'Failed to load posts',
		background: 'variant-filled-error'
	};

	$: loggedIn = $userStore.isLoggedIn();

	export const warmTimer = 60 * 1000;

	let regenComboValue = 'Regenerate...';
	const regenPopup: PopupSettings = {
		event: 'click',
		target: 'regenPopup',
		placement: 'bottom',
		closeQuery: '.listbox-item'
	};

	onMount(() => {
		async function warm() {
			// attempt to warm the lambda by calling /warm (/ping is reserved by API Gateway)
			console.log('Keeping lambda warm...');
			fetch('https://api.cantilevers.org/warm', {
				mode: 'no-cors',
				headers: {
					Accept: 'text/plain'
				}
			});
		}

		const interval = setInterval(warm, warmTimer);
		warm();
		return () => clearInterval(interval);
	});

	beforeNavigate(() => {
		// clear content stores on navigation
		markdownStore.clear();
		handlebars.clear();
	});

	async function initiateMetadataRebuild() {
		if ($project && $project.domain) {
			spinner.show('Rebuilding metadata...');
			let response = rebuildAllMetadata($userStore.token!!, $project.domain);
			response.then((data) => {
				toast.message = data;
				toastStore.trigger(toast);
				spinner.hide();
				regenComboValue = 'Regenerate...';
			});
		}
	}
	async function initiatePostsRebuild() {
		if ($project && $project.domain) {
			spinner.show('Rebuilding posts...');
			let response = rebuildAllPosts($userStore.token!!, $project.domain);
			response.then((data) => {
				toast.message = data;
				toastStore.trigger(toast);
				spinner.hide();
				regenComboValue = 'Regenerate...';
			});
		}
	}
	async function initiatePagesRebuild() {
		if ($project && $project.domain) {
			spinner.show('Rebuilding pages...');
			let response = rebuildAllPages($userStore.token!!, $project.domain);
			response.then((data) => {
				toast.message = data;
				toastStore.trigger(toast);
				spinner.hide();
				regenComboValue = 'Regenerate...';
			});
		}
	}

	async function initiateImageResRebuild() {
		console.log('Rebuilding image resolutions - not yet implemented');
	}
</script>

<!-- Single Modal Container -->
<Modal components={modalRegistry} />

<!-- Single Toast Container -->
<Toast position="t" padding="p-2" />

<!-- App Shell -->
<AppShell>
	<svelte:fragment slot="header">
		<!-- App Bar -->
		<AppBar gridColumns="grid-cols-3" slotDefault="place-self-center" slotTrail="place-content-end">
			<svelte:fragment slot="lead">
				<strong class="text-xl">Cantilever v0.0.11</strong>

				{#if loggedIn && $project && $project.domain}
					<button
						class="btn btn-sm variant-ghost-primary w-48 justify-between"
						use:popup={regenPopup}>
						<Icon icon={Sync} size={24} />
						<span class="capitalize">{regenComboValue ?? 'Trigger'}</span>
						<Icon icon={ExpandMore} size={24} />
					</button>
				{/if}
			</svelte:fragment>
			<div class="">
				{#if $project}
					<h2 class="h2">{$project.projectName}</h2>
				{:else}
					<h3 class="h3"><em>No project</em></h3>
				{/if}
			</div>
			<svelte:fragment slot="headline">
				<SpinnerStore />
			</svelte:fragment>
			<svelte:fragment slot="trail">
				<LoginAvatar />
			</svelte:fragment>
		</AppBar>
	</svelte:fragment>

	<!-- Navigation Rail -->
	<svelte:fragment slot="sidebarLeft">
		<!-- Hidden below Tailwind's large breakpoint -->

		{#if loggedIn}
			<AppRail>
				<div data-sveltekit-preload-data="false">
					<AppRailAnchor href="/" selected={$page.url.pathname === '/'} title="Home">
						<svelte:fragment slot="lead"
							><Icon icon={Home} size={32} variation="outlined" /></svelte:fragment>
						<span>Home</span>
					</AppRailAnchor>
					{#if $project && $project.projectName !== ''}
						<AppRailAnchor
							href="/project"
							selected={$page.url.pathname === '/project'}
							title="Project">
							<svelte:fragment slot="lead"
								><Icon
									icon={Settings_applications}
									size={32}
									variation="outlined" /></svelte:fragment>
							<span>Project</span>
						</AppRailAnchor>

						<AppRailAnchor href="/posts" selected={$page.url.pathname === '/posts'} title="Posts">
							<svelte:fragment slot="lead">
								<Icon icon={Feed} size={32} variation="outlined" />
							</svelte:fragment>

							<span>Posts</span>
						</AppRailAnchor>

						<AppRailAnchor href="/pages" selected={$page.url.pathname === '/pages'} title="Pages">
							<svelte:fragment slot="lead"
								><Icon icon={Article} size={32} variation="outlined" /></svelte:fragment>
							<span>Pages</span>
						</AppRailAnchor>

						<AppRailAnchor href="/media" selected={$page.url.pathname === '/media'} title="Media">
							<svelte:fragment slot="lead"
								><Icon icon={Perm_media} size={32} variation="outlined" /></svelte:fragment>
							<span>Media</span>
						</AppRailAnchor>

						<AppRailAnchor
							href="/templates"
							title="Templates"
							selected={$page.url.pathname === '/templates'}>
							<svelte:fragment slot="lead">
								<!-- TODO: this badge might be a nice way of indicating that there are ungenerated changes? -->
								<div class="relative inline-block">
									<span class="badge-icon variant-filled-error absolute -bottom-0 -right-0 z-10"
										>2</span>
									<Icon icon={Document_scanner} size={32} variation="outlined" />
								</div></svelte:fragment>
							<span>Templates</span>
						</AppRailAnchor>
					{/if}
				</div>
			</AppRail>
		{/if}
	</svelte:fragment>

	<!-- Page Route Content -->
	<div class="container h-full mx-auto flex">
		<slot />
	</div>
</AppShell>

<div class="card w-56 shadow-xl py-2 variant-glass-secondary z-20" data-popup="regenPopup">
	<ListBox rounded="rounded-none">
		<ListBoxItem
			bind:group={regenComboValue}
			name="metadata"
			value="metadata"
			on:click={(e) => {
				initiateMetadataRebuild();
			}}
			><svelte:fragment slot="lead"><Icon icon={Dataset_linked} /></svelte:fragment>Project metadata</ListBoxItem>
		<ListBoxItem
			bind:group={regenComboValue}
			name="posts"
			value="posts"
			on:click={(e) => {
				initiatePostsRebuild();
			}}>
			<svelte:fragment slot="lead"><Icon icon={Feed} /></svelte:fragment>Posts</ListBoxItem>
		<ListBoxItem
			bind:group={regenComboValue}
			name="pages"
			value="pages"
			on:click={(e) => {
				initiatePagesRebuild();
			}}>
			<svelte:fragment slot="lead"><Icon icon={Article} /></svelte:fragment>Pages</ListBoxItem>
		<ListBoxItem
			bind:group={regenComboValue}
			name="images"
			value="images"
			on:click={(e) => {
				initiateImageResRebuild();
			}}>
			<svelte:fragment slot="lead"
				><Icon icon={Perm_media} color="text-secondary-200" /></svelte:fragment
			><span class="text-secondary-200"><em>Images</em></span></ListBoxItem>
	</ListBox>
	<div class="arrow bg-surface-100-800-token" />
</div>
