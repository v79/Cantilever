<script lang="ts">
	import LoginButton from './loginButton.svelte';
	import { spinnerStore } from './utilities/spinnerWrapper.svelte';
	import { userStore } from '../stores/userStore.svelte';
	import { activeStore } from '../stores/appStatusStore.svelte';
	import { allPostsStore } from '../stores/postsStore.svelte';
	import { markdownStore } from '../stores/markdownContentStore.svelte';
	import { Dropdown, DropdownItem, Navbar, NavBrand, NavLi, NavUl } from 'flowbite-svelte';
	import { Modal } from 'flowbite-svelte';
	import CToast from './customized/cToast.svelte';
	import { notificationStore } from '../stores/notificationStore.svelte';
	import { afterUpdate, onMount } from 'svelte';
	import { afterNavigate } from '$app/navigation';

	let regenAllPostsModal = false;
	let regenAllPagesModal = false;

	var activePage = $activeStore.currentPage;
	var postsPage: boolean,
		templatesPage: boolean,
		projectPage: boolean,
		pagesPage: boolean = false;
	var title = 'Cantilever Editor';

	onMount(() => {});

	afterUpdate(() => {
		if ($activeStore.activeFile) {
			title = 'Cantilever Editor: ' + activePage + ' - ' + $activeStore.activeFile;
		} else {
			title = 'Cantilever Editor: ' + activePage;
		}
		switch ($activeStore.currentPage) {
			case 'Posts':
				postsPage = true;
				templatesPage = false;
				pagesPage = false;
				projectPage = false;
				break;
			case 'Pages':
				postsPage = false;
				templatesPage = false;
				pagesPage = true;
				projectPage = false;
				break;
			case 'Templates':
				postsPage = false;
				templatesPage = true;
				pagesPage = false;
				projectPage = false;
				break;
			case 'Project':
				postsPage = false;
				templatesPage = false;
				pagesPage = false;
				projectPage = true;
				break;
			default:
				break;
		}
	});

	afterNavigate(() => {
		markdownStore.clear();
	});

	function regenerateAllPosts() {
		console.log('Triggering regeneration of all posts');

		fetch('https://api.cantilevers.org/project/posts/rebuild', {
			method: 'PUT',
			headers: {
				Accept: 'application/json',
				Authorization: 'Bearer ' + $userStore.token,
				'X-Content-Length': '0'
			},
			mode: 'cors'
		})
			.then((response) => response.json())
			.then((data) => {
				notificationStore.set({ message: data.data, shown: true, type: 'success' });
				spinnerStore.set({ shown: false, message: '' });
			})
			.catch((error) => {
				console.log(error);
				notificationStore.set({ message: error, shown: true, type: 'error' });
				spinnerStore.set({ shown: false, message: '' });
			});
	}

	function regenerateAllPages() {
		console.log('Triggering regeneration of all pages');

		fetch('https://api.cantilevers.org/project/pages/rebuild', {
			method: 'PUT',
			headers: {
				Accept: 'application/json',
				Authorization: 'Bearer ' + $userStore.token,
				'X-Content-Length': '0'
			},
			mode: 'cors'
		})
			.then((response) => response.json())
			.then((data) => {
				notificationStore.set({ message: data.data, shown: true, type: 'success' });
				spinnerStore.set({ shown: false, message: '' });
			})
			.catch((error) => {
				console.log(error);
				notificationStore.set({ message: error, shown: true, type: 'error' });
				spinnerStore.set({ shown: false, message: '' });
			});
	}
</script>

<svelte:head>
	<title>{title}</title>
</svelte:head>

<Navbar
	color="none"
	navClass="bg-slate-600 text-gray-200 shadow-lg  py-4 px-4"
	fluid={false}
	navDivClass="mx-auto flex flex-wrap justify-between items-center shadow-md ">
	<NavBrand href="/">
		<img
			src="https://flowbite.com/docs/images/logo.svg"
			class="mr-3 h-6 sm:h-9"
			alt="Flowbite Logo" />
		<span class="self-center whitespace-nowrap text-xl font-semibold dark:text-white">
			Cantilever
		</span>
	</NavBrand>

	<NavUl nonActiveClass="text-gray-200">
		{#if $userStore !== undefined}
			<NavLi id="generate-menu" nonActiveClass="text-grey-200" class="cursor-pointer"
				>Generate</NavLi>
		{/if}
		<NavLi
			nonActiveClass="text-grey-200"
			activeClass="text-grey-200 font-bold"
			active={postsPage}
			href="/">Posts</NavLi>
		<NavLi
			nonActiveClass="text-grey-200"
			activeClass="text-grey-200 font-bold"
			active={pagesPage}
			href="/pages">Pages</NavLi>
		<NavLi
			nonActiveClass="text-grey-200"
			activeClass="text-grey-200 font-bold"
			active={templatesPage}
			href="/templates">Templates</NavLi>
		<NavLi
			nonActiveClass="text-grey-200"
			activeClass="text-grey-200 font-bold"
			active={projectPage}
			href="/project">Project</NavLi>

		{#if $userStore !== undefined}
			<Dropdown triggeredBy="#generate-menu">
				<DropdownItem on:click={(e) => (regenAllPostsModal = true)}>Rebuild all posts</DropdownItem>
				<DropdownItem on:click={(e) => (regenAllPagesModal = true)}>Rebuild all pages</DropdownItem>
			</Dropdown>
		{/if}
	</NavUl>
	<div class="flex-grow items-center justify-between" />
	<div class="flex flex-wrap items-center justify-end">
		<LoginButton />
	</div>
</Navbar>

<CToast />

<Modal title="Regenerate all posts?" bind:open={regenAllPostsModal} autoclose size="sm">
	<p>Regenerating all {$allPostsStore.count} posts may take some time. Continue?</p>

	<svelte:fragment slot="footer">
		<button
			type="button"
			class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
			>Cancel</button>
		<button
			type="button"
			on:click={(e) => {
				spinnerStore.set({ message: 'Regenerating...', shown: true });
				regenerateAllPosts();
			}}
			class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
			>Regenerate</button>
	</svelte:fragment>
</Modal>

<Modal title="Regenerate all pages?" bind:open={regenAllPagesModal} autoclose size="sm">
	<p>Regenerating all pages may take some time. Continue?</p>

	<svelte:fragment slot="footer">
		<button
			type="button"
			class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
			>Cancel</button>
		<button
			type="button"
			on:click={(e) => {
				spinnerStore.set({ message: 'Regenerating...', shown: true });
				regenerateAllPages();
			}}
			class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
			>Regenerate</button>
	</svelte:fragment>
</Modal>
