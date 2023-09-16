<script lang="ts">
	import ImageResEdit from './imageResEdit.svelte';
	import { ImgRes } from '../../models/structure';
	import { projectStore } from './projectStore.svelte';

	let addingNewImageRes: boolean = false;
	let newImageResKey: string = '';
	let newImageRes: ImgRes = new ImgRes(640, 480);

	$: resCount = $projectStore.imageResolutions.size;

	function enableAddResolution() {
		addingNewImageRes = true;
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
			if (oldKey !== newKey) {
				$projectStore.imageResolutions.delete(oldKey);
			}
			console.log('Updated imageres from ' + oldKey + ' to ' + newKey);
		}
		addingNewImageRes = false;
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
</script>

<div class="grid grid-cols-6 gap-6">
	<div class="col-span-6 sm:col-span-6 lg:col-span-6">
		{#each [...$projectStore.imageResolutions] as [key, res], index}
			<ImageResEdit {index} {key} {res} onUpdate={updateResolution} onDelete={deleteResolution} />
		{/each}
		{#if addingNewImageRes}
			<ImageResEdit
				index={resCount + 1}
				key={newImageResKey}
				res={newImageRes}
				readonly={false}
				onUpdate={updateResolution}
				onDelete={deleteResolution} />
		{/if}
	</div>
	<div class="col-span-6">
		<button
			disabled={addingNewImageRes}
			on:click={enableAddResolution}
			class="float-right text-right text-sm font-medium text-slate-200"
			type="button">Add new...</button>
	</div>
</div>
