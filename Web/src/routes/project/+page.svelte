<script lang="ts">
	import { afterNavigate } from '$app/navigation';
	import { Modal } from 'flowbite-svelte';
	import { onDestroy, tick } from 'svelte';
	import { stringify } from 'yaml';
	import ImageResEdit from '../../components/forms/imageResEdit.svelte';
	import TextInput from '../../components/forms/textInput.svelte';
	import SpinnerWrapper, { spinnerStore } from '../../components/utilities/spinnerWrapper.svelte';
	import { CantileverProject, ImgRes, parseResString } from '../../models/structure';
	import { activeStore } from '../../stores/appStatusStore.svelte';
	import { notificationStore } from '../../stores/notificationStore.svelte';
	import { userStore } from '../../stores/userStore.svelte';
	import { projectStore } from './projectStore.svelte';

	let saveChangesModal = false;

	let addingNewImageRes: boolean = false;
	let newImageResKey: string = '';
	let newImageRes: ImgRes = new ImgRes(640, 480);

	$: resCount = $projectStore.imageResolutions.size;

	afterNavigate(() => {
		$activeStore.currentPage = 'Project';
		$activeStore.activeFile = '';
	});

	function loadProjectDefinition() {
		let token = $userStore.token;
		console.log('Loading project definition');
		spinnerStore.set({ shown: true, message: 'Loading cantilevers project definition file...' });
		notificationStore.set({ shown: false, message: '', type: 'info' });
		tick();
		fetch('https://api.cantilevers.org/project/', {
			method: 'GET',
			headers: {
				Accept: 'application/json',
				Authorization: 'Bearer ' + token
			},
			mode: 'cors'
		})
			.then((response) => response.json())
			.then((data) => {
				if (data.data == undefined) {
					throw new Error(data.message);
				}
				var tmpResolutions = Object.entries(data.data.imageResolutions); // Array[key, value]
				var imageRestMap: Map<string, ImgRes> = new Map<string, ImgRes>();
				for (const iR of tmpResolutions) {
					imageRestMap.set(iR[0], parseResString(iR[1] as string));
				}

				var tmpProject = new CantileverProject(
					data.data.projectName,
					data.data.author,
					data.data.dateFormat,
					data.data.dateTimeFormat,
					imageRestMap
				);
				projectStore.set(tmpProject);
				$notificationStore.message = 'Loaded project ' + tmpProject.projectName;
				$notificationStore.shown = true;
				$spinnerStore.shown = false;
			})
			.catch((error) => {
				console.log(error);
				notificationStore.set({
					message: error,
					shown: true,
					type: 'error'
				});
				$spinnerStore.shown = false;
				return {};
			});
	}

	function saveProject() {
		spinnerStore.set({
			shown: true,
			message: 'Saving updated cantilevers project definition file...'
		});
		let yaml = stringify($projectStore);
		console.log('Yaml output: ' + yaml);

		fetch('https://api.cantilevers.org/project/', {
			method: 'PUT',
			headers: {
				Accept: 'application/json',
				'Content-Type': 'application/yaml',
				Authorization: 'Bearer ' + $userStore.token
			},
			body: yaml,
			mode: 'cors'
		})
			.then((response) => response.json())
			.then((data) => {
				if (data.data == undefined) {
					throw new Error(data.message);
				}
				var tmpResolutions = Object.entries(data.data.imageResolutions); // Array[key, value]
				var imageRestMap: Map<string, ImgRes> = new Map<string, ImgRes>();
				for (const iR of tmpResolutions) {
					imageRestMap.set(iR[0], parseResString(iR[1] as string));
				}

				var tmpProject = new CantileverProject(
					data.data.projectName,
					data.data.author,
					data.data.dateFormat,
					data.data.dateTimeFormat,
					imageRestMap
				);
				projectStore.set(tmpProject);
				$notificationStore.message = 'Loaded project ' + tmpProject.projectName;
				$notificationStore.shown = true;
				$spinnerStore.shown = false;
			})
			.catch((error) => {
				console.log(error);
				notificationStore.set({
					message: error,
					shown: true,
					type: 'error'
				});
				$spinnerStore.shown = false;
				return {};
			});
	}

	function updateResolution(oldKey: string, newKey: string, newRes: ImgRes) {
		console.log(
			'Updating resolution from ' +
				oldKey +
				'>' +
				$projectStore.imageResolutions.get(oldKey) +
				' with new key ' +
				newKey
		);
		let res = $projectStore.imageResolutions.get(oldKey);
		if (res || addingNewImageRes) {
			$projectStore.imageResolutions.set(newKey, newRes);
			$projectStore.imageResolutions.delete(oldKey);
			console.log('Updated imageres from ' + oldKey + ' to ' + newKey);
		}
	}

	function deleteResolution(key: string) {
		console.log('Deleting resolution ' + key);
		if (addingNewImageRes) {
			addingNewImageRes = false;
			newImageRes = new ImgRes(640, 480);
			newImageResKey = '';
		}
		$projectStore.imageResolutions.delete(key);
		$projectStore.imageResolutions = $projectStore.imageResolutions;
	}

	function enableAddResolution() {
		addingNewImageRes = true;
	}

	const userStoreUnsubscribe = userStore.subscribe((data) => {
		if (data) {
			loadProjectDefinition();
		}
	});

	onDestroy(userStoreUnsubscribe);
</script>

<div class="flex grow flex-row">
	<div class="basis-1/4 bg-slate-400" />
	<div class="basis-1/2 bg-slate-600">
		<div class="relative mt-5 md:col-span-2 md:mt-0">
			<h3 class="px-4 py-4 text-center text-2xl font-bold">
				Project Settings
				{#if $projectStore?.projectName}
					: {$projectStore.projectName}
				{/if}
			</h3>
			<div class="flex items-center justify-end pr-8 focus:shadow-lg" role="group">
				<button
					type="button"
					on:click={() => {
						saveChangesModal = true;
					}}
					class="inline-block rounded-r bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white transition duration-150 ease-in-out hover:bg-blue-700 focus:bg-blue-700 focus:outline-none focus:ring-0 active:bg-blue-800 disabled:hover:bg-purple-600"
					>Save</button>
			</div>
			{#if $projectStore}
				<div class="relative mt-5 md:col-span-2 md:mt-0">
					<div class="px-4 py-5 sm:p-6">
						<!-- TODO: move this into a separate component? -->
						<form action="#" method="POST">
							<div class="overflow-hidden shadow sm:rounded-md">
								<div class="grid grid-cols-6 gap-6">
									<div class="col-span-6 sm:col-span-6 lg:col-span-3">
										<TextInput
											bind:value={$projectStore.projectName}
											name="projectName"
											label="Project name"
											required />
									</div>

									<div class="col-span-6 sm:col-span-6 lg:col-span-3">
										<TextInput bind:value={$projectStore.author} name="author" label="Author" />
									</div>

									<div class="col-span-6 sm:col-span-6 lg:col-span-3">
										<TextInput
											bind:value={$projectStore.dateFormat}
											name="dateFormat"
											label="Date format"
											required />
									</div>
									<div class="col-span-6 sm:col-span-6 lg:col-span-3">
										<TextInput
											bind:value={$projectStore.dateTimeFormat}
											name="dateTimeFormat"
											label="Date/Time Format"
											required />
									</div>

									<div class="col-span-5">
										<h3 class="text-base font-bold text-slate-200">Image resolutions</h3>
									</div>
									<div class="col-span-1">
										<button
											disabled={addingNewImageRes}
											on:click={enableAddResolution}
											class="float-right text-right text-sm font-medium text-slate-200"
											type="button">Add new...</button>
									</div>
									<div class="col-span-6 sm:col-span-6 lg:col-span-6">
										{#if addingNewImageRes}
											<ImageResEdit
												index={resCount + 1}
												key={newImageResKey}
												res={newImageRes}
												readonly={false}
												onUpdate={updateResolution}
												onDelete={deleteResolution} />
										{/if}
										{#each [...$projectStore.imageResolutions] as [key, res], index}
											<ImageResEdit
												{index}
												{key}
												{res}
												onUpdate={updateResolution}
												onDelete={deleteResolution} />
										{/each}
									</div>
								</div>
							</div>
						</form>
					</div>
				</div>
			{/if}
		</div>
	</div>
	<div class="invisible basis-1/4 bg-slate-800 lg:visible">
		<h3 class="px-4 py-4 text-center text-2xl font-bold text-slate-200">Messages</h3>
		<SpinnerWrapper spinnerID="globalSpinner" />
	</div>
</div>

<Modal title="Save project?" bind:open={saveChangesModal} autoclose size="sm">
	<p>
		Save changes to project definition <strong>{$projectStore?.projectName}</strong>?
	</p>
	<svelte:fragment slot="footer">
		<button
			type="button"
			class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
			>Cancel</button>
		<button
			type="button"
			on:click={saveProject}
			class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
			>Save</button>
	</svelte:fragment>
</Modal>
