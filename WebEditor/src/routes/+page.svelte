<script lang="ts">
	import { goto } from '$app/navigation';
	import { fetchProject, project } from '$lib/stores/projectStore.svelte';
	import { userStore } from '$lib/stores/userStore.svelte';
	import { Add, Icon } from 'svelte-google-materialdesign-icons';
	import PlusOne from 'svelte-google-materialdesign-icons/Plus_one.svelte';

	$: isLoggedIn = $userStore.isLoggedIn();

	async function navCreateNewProject() {
		console.log('navCreateNewProject');
		project.clear();
		let nav = await goto('/project?mode=new');
	}

	async function loadCantileverProject() {
		console.log('loadCantileverProject');
		if ($userStore.isLoggedIn()) {
			await fetchProject($userStore.token!!);
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
		<div class="flex flex-col">
			<p class="p">This is where I'd put a list of projects, if I had any.</p>
			<p class="p">Plus provide some shortcut buttons to features like...</p>
			<ul class="list-inside list-disc">
				<li>Creating a new page</li>
				<li>Creating a new post</li>
				<li>Regenerating all content</li>
			</ul>
		</div>

		<div class="flex flex-row justify-center w-full btn-group">
			<button class=" variant-filled-primary" on:click={() => navCreateNewProject()}
				><Icon icon={PlusOne} />Create a new project</button>
			<button class=" variant-filled-primary" on:click={() => {}}
				><Icon icon={Add} />New blog post</button>
			<button class=" variant-filled-primary" on:click={() => {}}
				><Icon icon={Add} />New page post</button>
				<button class="btn" on:click={() => loadCantileverProject()}>TEMP: Load Cantilever</button>
		</div>

	{:else}
		<h3 class="h3">Please log in to continue</h3>
	{/if}
</div>
