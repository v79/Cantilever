<script lang="ts">
	import { page } from '$app/stores';
	import TextInput from '$lib/forms/textInput.svelte';
	import { createProject, project, saveProject } from '$lib/stores/projectStore.svelte';
	import { userStore } from '$lib/stores/userStore.svelte';
	import {
		getModalStore,
		getToastStore,
		Tab,
		TabGroup,
		type ToastSettings
	} from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { Icon, Save } from 'svelte-google-materialdesign-icons';
	import ImageResolutions from './ImageResolutions.svelte';

	const modalStore = getModalStore();
	const toastStore = getToastStore();
	let mode = 'edit';

	$: webPageTitle = $project && $project.projectName ? ' - ' + $project.projectName : '';

	const toast: ToastSettings = {
		message: 'Loaded posts',
		background: 'variant-filled-success',
		hideDismiss: true
	};
	const errorToast: ToastSettings = {
		message: 'Failed to load posts',
		background: 'variant-filled-error'
	};

	/**
	 * @type: {ModalSettings}
	 */
	$: createNewProjectModal = {
		type: 'component',
		component: 'createNewProjectModal',
		meta: {
			modalTitle: 'Create new project',
			projectTitle: $project ? $project.projectName : '',
			domain: $project ? $project.domain : '',
			onFormSubmit: (domain: string) => {
				initiateCreateProject(domain);
			}
		}
	};

	let tabSet = 0;
	$: saveDisabled =
		$project &&
		($project.projectName === undefined ||
			$project.projectName === '' ||
			$project.domain === undefined ||
			$project.domain === '');

	onMount(async () => {
		if ($userStore.isLoggedIn()) {
			// if the query string contains mode=new, clear the project store
			if ($page.url.searchParams.get('mode') === 'new') {
				project.clear();
				mode = 'new';
			}
		}
	});

	async function initiateSaveProject() {
		console.dir($project);
		if (!saveDisabled) {
			let saveResult = saveProject($project, $userStore.token!!);
			saveResult.then((r) => {
				if (r instanceof Error) {
					errorToast.message = 'Failed to save project';
					toastStore.trigger(errorToast);
				} else {
					toast.message = 'Saved project ' + $project.projectName;
					toastStore.trigger(toast);
				}
			});
		}
	}

	async function initiateCreateProject(domain: string) {
		console.log('initiateCreateProject: ', domain);
		let saveResult = createProject($project, $userStore.token!!);
		saveResult.then((r) => {
			if (r instanceof Error) {
				console.dir(r);
				errorToast.message = r.message;
				toastStore.trigger(errorToast);
			} else {
				toast.message = 'Saved project ' + $project.projectName;
				toastStore.trigger(toast);
				mode = 'edit';
			}
		});
	}

	const projectUnsub = project.subscribe((value) => {
		if (value) {
			// do nothing
		}
	});
</script>

<svelte:head>
	<title>Cantilever: Project Settings {webPageTitle}</title>
</svelte:head>

<div class="flex flex-col grow mt-2 container">
	{#if $userStore.isLoggedIn()}
		<div class="flex flex-col justify-center w-full">
			{#if $project}
				<h3 class="h3 mb-2 text-center">
					{#if mode === 'new'}Create new project{:else}{$project.projectName} settings{/if}
				</h3>
				<div class="flex flex-row justify-end">
					<div class="btn-group variant-filled" role="group">
						<button
							on:click={(e) => {
								if (mode === 'new') {
									modalStore.trigger(createNewProjectModal);
								} else {
									initiateSaveProject();
								}
							}}
							disabled={saveDisabled}
							title="Save project settings"
							class="variant-filled-primary">Save<Icon icon={Save} /></button>
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
					<div class="col-span-6 sm:col-span-6 lg:col-span-2">
						<TextInput
							label="Website domain"
							name="domain"
							readonly={mode === 'edit'}
							bind:value={$project.domain}
							required />
					</div>
				</div>

				<TabGroup justify="justify-center" class="mt-4">
					<Tab bind:group={tabSet} name="resolutions" value={0}
						>Resolutions ({$project.imageResolutions.size})</Tab>
					<Tab bind:group={tabSet} name="attributes" value={1}>Custom attributes</Tab>
					<!-- Tab Panels --->
					<svelte:fragment slot="panel">
						{#if tabSet === 0}
							<ImageResolutions />
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
