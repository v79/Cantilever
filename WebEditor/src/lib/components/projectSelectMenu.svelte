<script lang="ts">
	import { fetchProject } from '$lib/stores/projectStore.svelte';
	import { userStore } from '$lib/stores/userStore.svelte';
	import { Download, Icon } from 'svelte-google-materialdesign-icons';

	export let projectList: Map<string, string> = new Map<string, string>();

	let selectedProjectKey: string = '';
	$: disabled = selectedProjectKey === '';

	async function initiateLoadProject() {
		console.log('loadProject');
		if (selectedProjectKey !== '' && $userStore.isLoggedIn()) {
			await fetchProject($userStore.token!!, selectedProjectKey);
		}
	}
</script>

{#if projectList.size > 0}
	<label class="label" for="projectSelect">Select a project</label>
	<select class="select" bind:value={selectedProjectKey}>
		<option value="">Select project</option>
		{#each projectList as p}
			<option value={p[1]}>{p[0]}</option>
		{/each}
	</select>
	<button
		class="btn variant-filled-primary ml-4"
		{disabled}
		on:click={(e) => {
			initiateLoadProject();
		}}><Icon icon={Download} />Load</button>
{:else}
	<div class="placeholder m-2 p-2">Loading projects...</div>
{/if}
