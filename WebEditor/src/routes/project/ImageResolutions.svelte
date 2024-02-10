<script lang="ts">
	import { ImgRes } from '$lib/models/project';
	import { project } from '$lib/stores/projectStore.svelte';
	import { Icon, Delete, Add } from 'svelte-google-materialdesign-icons';
	import ImageResEdit from './ImageResEdit.svelte';
	import { getModalStore } from '@skeletonlabs/skeleton';

	let modalStore = getModalStore();
	$: hoveredRes = '';

	/**
	 * @type: {ModalSettings}
	 */
	$: deleteResolutionModal = {
		type: 'confirm',
		title: 'Delete resolution',
		body: 'Are you sure you want to delete resolution ' + hoveredRes + '?',
		buttonTextConfirm: 'Delete',
		buttonTextCancel: 'Cancel',
		response: (r: boolean) => {
			if (r) {
				deleteResolutionRow();
			}
			modalStore.close();
		}
	};

	function hoverRes(key: string) {
		hoveredRes = key;
	}

	function addResolutionRow() {
		$project.imageResolutions.set('new', new ImgRes(0, 0));
	}

	function deleteResolutionRow() {
		$project.imageResolutions.delete(hoveredRes);
	}
</script>

{#if $project.imageResolutions}
	<div class="grid grid-cols-6 gap-6">
		<div class="col-span-6 sm:col-span-6 lg:col-span-6">
			<p><em>Changing image resolutions won't trigger regeneration of images at this time.</em></p>
			{#each [...$project.imageResolutions] as [key, res], index}
				<div
					class="flex items-center"
					role="row"
					tabindex={index}
					on:mouseover={(e) => {
						hoverRes(key);
					}}
					on:focus={(e) => {
						hoverRes(key);
					}}>
					<ImageResEdit {index} bind:key bind:res />
					<div class="items-start ml-4 mt-8">
						{#if hoveredRes === key}
							<button
								on:click={() => {
									modalStore.trigger(deleteResolutionModal);
								}}
								title="Delete resolution"
								class="rounded-full hover:bg-gray-200 transition-colors duration-300 ease-in-out"
								><Icon icon={Delete} color="red" /></button>
						{/if}
					</div>
				</div>
			{/each}
			<div class="mt-1 grid grid-cols-1 gap-6 w-6/12">
				<div class="col-span-2 sm:col-span-2 lg:col-span-2 flex justify-end">
					<div class="btn-group variant-filled mt-4">
						<button
							class="variant-filled-primary"
							on:click={() => {
								addResolutionRow();
							}}><Icon icon={Add} />Add resolution</button>
					</div>
				</div>
			</div>
		</div>
	</div>
{/if}
