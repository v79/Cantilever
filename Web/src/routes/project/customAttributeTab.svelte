<script lang="ts">
	import { readable, readonly } from 'svelte/store';
	import AttributeEdit from './attributeEdit.svelte';
	import { projectStore } from './projectStore.svelte';

	let addingNewAttribute: boolean = false;
	let newAttributeKey: string = '';
	let newAttributeValue: string = '';

	$: attributeCount = $projectStore.attributes.size;

	function enableAddAttribute() {
		addingNewAttribute = true;
	}

	function updateAttribute(oldKey: string, newKey: string, newValue: string) {
		console.log(
			'Updating attribute from ' +
				oldKey +
				'>' +
				$projectStore.attributes.get(oldKey) +
				' with new key ' +
				newKey +
				', to value ' +
				newValue
		);

		let existingValue = $projectStore.attributes.get(oldKey);
		if (existingValue || addingNewAttribute) {
			$projectStore.attributes.set(newKey, newValue);
			if (oldKey !== newKey) {
				$projectStore.attributes.delete(oldKey);
			}
		}
		addingNewAttribute = false;
		$projectStore.attributes = $projectStore.attributes;
	}

	function deleteAttribute(key: string) {
		console.log('Deleting attribute ' + key);
		if (addingNewAttribute) {
			addingNewAttribute = false;
			newAttributeValue = '';
			newAttributeKey = '';
		}
		$projectStore.attributes.delete(key);
		$projectStore.attributes = $projectStore.attributes;
	}
</script>

<div class="grid grid-cols-6 gap-6">
	<div class="col-span-6 sm:col-span-6 lg:col-span-6">
		{#each [...$projectStore.attributes] as [key, value], index}
			<AttributeEdit {index} {key} {value} onUpdate={updateAttribute} onDelete={deleteAttribute} />
		{/each}
		{#if addingNewAttribute}
			<AttributeEdit
				index={attributeCount + 1}
				key={newAttributeKey}
				value={newAttributeValue}
				readonly={false}
				onUpdate={updateAttribute}
				onDelete={deleteAttribute} />
		{/if}
	</div>
	<div class="col-span-6">
		<button
			disabled={addingNewAttribute}
			on:click={enableAddAttribute}
			class="float-right text-right text-sm font-medium text-slate-200"
			type="button">Add new...</button>
	</div>
</div>
