<script lang="ts">
	import { goto } from '$app/navigation';
	import ProjectSelectMenu from '$lib/components/projectSelectMenu.svelte';
	import { fetchProject, fetchProjectList, project } from '$lib/stores/projectStore.svelte';
	import { userStore } from '$lib/stores/userStore.svelte';
	import { getToastStore, type ToastSettings } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { Add, Icon } from 'svelte-google-materialdesign-icons';
	import PlusOne from 'svelte-google-materialdesign-icons/Plus_one.svelte';

	const toastStore = getToastStore();
	const toast: ToastSettings = {
		message: 'Success',
		background: 'variant-filled-success',
		hideDismiss: true
	};
	const errorToast: ToastSettings = {
		message: 'Failed',
		background: 'variant-filled-error'
	};

	$: isLoggedIn = $userStore.isLoggedIn();

	$: projectList = new Map<string,string>();

	onMount(async () => {
		if ($userStore.isLoggedIn()) {
			let list = await fetchProjectList($userStore.token!!);
			if(list instanceof Error) {
				console.error('Error fetching project list');
				errorToast.message = "Failed to load project list";
				toastStore.trigger(errorToast);
			} else {
				projectList = list;
			}
		}
	});

	async function navCreateNewProject() {
		console.log('navCreateNewProject');
		project.clear();
		let nav = await goto('/project?mode=new');
	}

	async function loadCantileverProject() {
		console.log('loadCantileverProject');
		if ($userStore.isLoggedIn()) {
			await fetchProject($userStore.token!!,"cantilever.yaml");
		}
	}

	
</script>

<svelte:head>
	<title>Cantilever</title>
</svelte:head>

<div class="flex flex-col grow mt-2 container">
	{#if isLoggedIn}
		<div class="flex flex-row justify-center w-full">
			<h3 class="h3 mb-2 text-center">Welcome</h3>
		</div>
		

		<div class="flex flex-row justify-center m-4">
			{#if projectList}
				<ProjectSelectMenu projectList={projectList} />
			{:else}
				<div class="placeholder">Loading projects...</div>
			{/if}
		</div>

		<div class="flex flex-row justify-center w-full btn-group m-4">
			<button class=" variant-filled-primary" on:click={() => navCreateNewProject()}
				><Icon icon={PlusOne} />Create a new project</button>
			<button class=" variant-filled-primary" on:click={() => {}}
				><Icon icon={Add} />New blog post</button>
			<button class=" variant-filled-primary" on:click={() => {}}
				><Icon icon={Add} />New page post</button>
				<button class="btn" on:click={() => loadCantileverProject()}>TEMP: Load Cantilever</button>
		</div>

		<div class="flex flex-col">
			<p class="p">This is where I'd put a list of projects, if I had any.</p>
			<p class="p">Plus provide some shortcut buttons to features like...</p>
			<ul class="list-inside list-disc">
				<li>Creating a new page</li>
				<li>Creating a new post</li>
				<li>Regenerating all content</li>
			</ul>
		</div>

	{:else}
		<h3 class="h3">Please log in to continue</h3>
	{/if}
</div>
