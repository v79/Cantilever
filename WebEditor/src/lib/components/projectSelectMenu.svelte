<script lang="ts">
	import { goto } from '$app/navigation';
	import { fetchProject } from '$lib/stores/projectStore.svelte';
	import { userStore } from '$lib/stores/userStore.svelte';
	import { getToastStore, type ToastSettings } from '@skeletonlabs/skeleton';
	import { Download, Icon } from 'svelte-google-materialdesign-icons';

	export let projectList: Map<string, string> = new Map<string, string>();

	let toastStore = getToastStore();
	const toast: ToastSettings = {
		message: 'Loaded project',
		background: 'variant-filled-success',
		hideDismiss: true
	};
	const errorToast: ToastSettings = {
		message: 'Failed to load project',
		background: 'variant-filled-error'
	};

	let selectedProjectKey: string = '';
	$: disabled = selectedProjectKey === '';

	async function initiateLoadProject() {
		if (selectedProjectKey !== '' && $userStore.isLoggedIn()) {
			let loadResponse = await fetchProject($userStore.token!!, selectedProjectKey);
			if (loadResponse instanceof Error) {
				console.error('Failed to load project', loadResponse);
				errorToast.message = 'Failed to load project: ' + loadResponse.message;
				toastStore.trigger(errorToast);
			} else {
				toast.message = 'Loaded project ' + loadResponse.projectName;
				toastStore.trigger(toast);
				goto('/project');
			}
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
