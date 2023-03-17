<script>
    import LoginButton from './loginButton.svelte';
    import {activeStore} from '../stores/appStatusStore.svelte';
    import {spinnerStore} from './utilities/spinnerWrapper.svelte';
    import {userStore} from '../stores/userStore.svelte';
    import {allPostsStore} from '../stores/postsStore.svelte';
    import {Button, Chevron, Dropdown, DropdownItem, Navbar, NavBrand, NavUl} from 'flowbite-svelte';
    import CModal from './customized/cModal.svelte';
    import CToast from './customized/cToast.svelte';
    import {notificationStore} from '../stores/notificationStore.svelte';

    let regenAllPostsModal = false;
	let regenAllPagesModal = false;

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

<Navbar
	color="none"
	navClass="bg-slate-600 text-gray-200 shadow-lg py-4 px-4"
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
			<Button size="sm"><Chevron>Generate</Chevron></Button>
			<Dropdown>
				<DropdownItem on:click={(e) => (regenAllPostsModal = true)}>Rebuild all posts</DropdownItem>
				<DropdownItem on:click={(e) => (regenAllPagesModal = true)}>Rebuild all pages</DropdownItem>
			</Dropdown>
		{/if}
		<button type="button" class="uppercase">Pages</button>
		<button type="button" class="uppercase">Posts</button>
		<button type="button" class="uppercase">Templates</button>
	</NavUl>
	<div class="flex-grow items-center justify-between">
		<p>
			<strong>Current file:</strong>
			{$activeStore.activeFile} <strong>Is new:</strong>
			{$activeStore.isNewFile} <strong>Has changed: </strong>{$activeStore.hasChanged}
			<strong>Is valid: </strong>{$activeStore.isValid}
		</p>
	</div>
	<div class="flex flex-wrap items-center justify-end">
		<LoginButton />
	</div>
</Navbar>

<CToast />

<CModal title="Regenerate all posts?" bind:open={regenAllPostsModal} autoclose size="sm">
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
</CModal>

<CModal title="Regenerate all pages?" bind:open={regenAllPagesModal} autoclose size="sm">
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
</CModal>
