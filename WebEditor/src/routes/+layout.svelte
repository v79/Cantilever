<script lang="ts">
	import {
		AppBar,
		AppRail,
		AppRailAnchor,
		AppShell,
		Avatar,
		Modal,
		initializeStores
	} from '@skeletonlabs/skeleton';
	import '../app.postcss';
	// Highlight JS
	import { storeHighlightJs } from '@skeletonlabs/skeleton';
	import hljs from 'highlight.js/lib/core';
	import xml from 'highlight.js/lib/languages/xml';
	import 'highlight.js/styles/github-dark.css';
	// for HTML
	import css from 'highlight.js/lib/languages/css';
	import javascript from 'highlight.js/lib/languages/javascript';
	import typescript from 'highlight.js/lib/languages/typescript';
	// Floating UI for Popups
	import { arrow, autoUpdate, computePosition, flip, offset, shift } from '@floating-ui/dom';
	import { storePopup } from '@skeletonlabs/skeleton';
	import {
		Article,
		Document_scanner,
		Feed,
		Icon,
		Perm_media,
		Settings_applications
	} from 'svelte-google-materialdesign-icons';
	import { page } from '$app/stores';
	import LoginAvatar from '../components/LoginAvatar.svelte';
	storePopup.set({ computePosition, autoUpdate, flip, shift, offset, arrow });
	initializeStores();
</script>

<!-- Single Modal Container -->
<Modal />
<!-- App Shell -->
<AppShell>
	<svelte:fragment slot="header">
		<!-- App Bar -->
		<AppBar gridColumns="grid-cols-3" slotDefault="place-self-center" slotTrail="place-content-end">
			<svelte:fragment slot="lead">
				<strong class="text-xl">Cantilever v0.0.9</strong>
				<button type="button" class="btn btn-sm variant-ghost-secondary"> Rebuild Metadata </button>
			</svelte:fragment>
			<h1 class="h1">{$page.route.id}</h1>
			<!-- TODO: replace with value from my custom navigation store? -->
			<svelte:fragment slot="trail">
				<LoginAvatar />
			</svelte:fragment>
		</AppBar>
	</svelte:fragment>

	<!-- Navigation Rail -->
	<svelte:fragment slot="sidebarLeft">
		<!-- Hidden below Tailwind's large breakpoint -->

		<AppRail>
			<div data-sveltekit-preload-data="false">
				<AppRailAnchor href="/" title="Project">
					<svelte:fragment slot="lead"
						><Icon icon={Settings_applications} size={32} variation="outlined" /></svelte:fragment
					>
					<span>Project</span>
				</AppRailAnchor>

				<AppRailAnchor href="/posts" title="Posts">
					<svelte:fragment slot="lead"
						><Icon icon={Feed} size={32} variation="outlined" /></svelte:fragment
					>
					<span>Posts</span>
				</AppRailAnchor>

				<AppRailAnchor href="/" title="Pages">
					<svelte:fragment slot="lead"
						><Icon icon={Article} size={32} variation="outlined" /></svelte:fragment
					>
					<span>Pages</span>
				</AppRailAnchor>

				<AppRailAnchor href="/" title="Media">
					<svelte:fragment slot="lead"
						><Icon icon={Perm_media} size={32} variation="outlined" /></svelte:fragment
					>
					<span>Media</span>
				</AppRailAnchor>

				<AppRailAnchor href="/" title="Templates">
					<svelte:fragment slot="lead"
						><Icon icon={Document_scanner} size={32} variation="outlined" /></svelte:fragment
					>
					<span>Templates</span>
				</AppRailAnchor>
			</div>
		</AppRail>
	</svelte:fragment>

	<!-- Page Route Content -->
	<div class="container h-full mx-auto flex">
		<slot />
	</div>
</AppShell>