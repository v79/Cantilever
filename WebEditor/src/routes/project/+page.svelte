<script lang="ts">
	import { userStore } from '$lib/stores/userStore.svelte';
	import {
		getModalStore,
		getToastStore,
		Tab,
		TabGroup,
		type ToastSettings
	} from '@skeletonlabs/skeleton';
	import { fetchProject, project } from '$lib/stores/projectStore.svelte';
	import { onMount } from 'svelte';
	import { Icon, Save } from 'svelte-google-materialdesign-icons';
	import TextInput from '$lib/forms/textInput.svelte';
	import ImageResolutions from './ImageResolutions.svelte';
	const modalStore = getModalStore();
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

	let tabSet = 0;

	onMount(async () => {
		if ($userStore.isLoggedIn()) {
			fetchProject($userStore.token!!);
		}
	});

	function initiateSaveProject() {
		console.log('Saving project');
	}

	const projectUnsub = project.subscribe((value) => {
		if (value) {
			// do nothing
		}
	});
</script>

<div class="flex flex-col grow mt-2 container">
	{#if $userStore.isLoggedIn()}
		<div class="flex flex-col justify-center w-full">
			{#if $project}
				<h3 class="h3 mb-2 text-center">{$project.projectName} settings</h3>
				<div class="flex flex-row justify-end">
					<div class="btn-group variant-filled" role="group">
						<button on:click={() => {}} title="Save project settings" class="variant-filled-primary"
							>Save<Icon icon={Save} /></button>
					</div>
				</div>

				<div class="grid grid-cols-6 gap-6">
					<div class="col-span-6 sm:col-span-6 lg:col-span-4">
						<TextInput
							label="Project name"
							name="project-name"
							bind:value={$project.projectName}
							required />
					</div>
					<div class="col-span-6 sm:col-span-6 lg:col-span-2">
						<TextInput
							label="Project author"
							name="project-author"
							bind:value={$project.author}
							required />
					</div>
				</div>
				<div class="grid grid-cols-6 gap-6">
					<div class="col-span-6 sm:col-span-6 lg:col-span-2">
						<TextInput
							label="Default date format"
							name="date-format"
							bind:value={$project.dateFormat}
							required />
					</div>
					<div class="col-span-6 sm:col-span-6 lg:col-span-2">
						<TextInput
							label="Default date &amp; time format"
							name="date-time-format"
							bind:value={$project.dateTimeFormat}
							required />
					</div>
				</div>
				
					<TabGroup justify="justify-center" class="mt-4">
						<Tab bind:group={tabSet} name="resolutions" value={0}>Resolutions</Tab>
						<Tab bind:group={tabSet} name="attributes" value={1}>Custom attributes</Tab>
						<!-- Tab Panels --->
						<svelte:fragment slot="panel">
							{#if tabSet === 0}
                                {#if $project.imageResolutions.size === 0}
                                    <p class="placeholder">No image resolutions defined</p>
                                {:else}
                                    <ImageResolutions />
                                {/if}
							{:else if tabSet === 1}
								(custom attribute panel contents)
							{/if}
						</svelte:fragment>
					</TabGroup>
				
			{:else}
				<p class="placeholder">Loading project...</p>
			{/if}
		</div>
	{:else}
		<p class="text-error-500">Please log in to continue</p>
	{/if}
</div>
