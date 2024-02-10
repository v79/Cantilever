<script lang="ts">
	import type { ImgRes } from '$lib/models/project';
	import { project } from '$lib/stores/projectStore.svelte';
	import { Icon, Delete } from 'svelte-google-materialdesign-icons';
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
        body: 'Are you sure you want to delete resolution ' +  hoveredRes + '?',
        buttonTextConfirm: 'Delete',
        buttonTextCancel: 'Cancel',
        response: (r: boolean) => {
            if (r) {
                console.log('Deleting resolution');
            }
            modalStore.close();
        }
    };



	function hoverRes(key: string) {
		hoveredRes = key;
	}
</script>

{#if $project.imageResolutions}
	<div class="grid grid-cols-6 gap-6">
		<div class="col-span-6 sm:col-span-6 lg:col-span-6">
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
								on:click={(e) => {modalStore.trigger(deleteResolutionModal)}}
								title="Delete resolution"
								class="rounded-full hover:bg-gray-200 transition-colors duration-300 ease-in-out"
								><Icon icon={Delete} color="red" /></button>
						{/if}
					</div>
				</div>
			{/each}
		</div>
	</div>
{/if}
