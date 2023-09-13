<script lang="ts">
	import { Modal } from 'flowbite-svelte';
	import { ImgRes } from '../../models/structure';
	import NumberInput from './numberInput.svelte';
	import TextInput from './textInput.svelte';
	import { onMount } from 'svelte';

	export let index: number;
	export let key: string;
	export let res: ImgRes;
	export let readonly: boolean = true;
	export let onUpdate = (oldKey: string, newKey: string, newRes: ImgRes) => {};
	export let onDelete = (key: string) => {};

	let deleteImageRes = false;
	let editing: boolean = false;
	let newKey = key;
	$: editSave = editing ? 'Update' : 'Edit';

	$: xS = res.x && isNaN(res.x) ? '' : (res.x as unknown as string);
	$: yS = res.y as unknown as string;

	onMount(() => {
		if (!readonly) {
			editing = true;
		}
	});

	function clickEdit() {
		if (editing) {
			saveChanges();
		}
		editing = !editing;
	}

	function cancelEdit() {
		editing = false;
	}

	function openDeleteModal() {
		deleteImageRes = true;
	}

	function saveChanges() {
		console.log('Save changes... ' + key + '>' + newKey + ': ' + xS + ',' + yS);
		let newX = parseInt(xS);
		let newY = parseInt(yS);
		let newRes = new ImgRes(newX, newY);
		onUpdate(key, newKey, newRes);
		key = newKey;
	}

	function updateDimension(dimension: string, e: Event) {
		switch (dimension) {
			case 'x':
				res.x = parseInt(e.target.value);
				break;
			case 'y':
				res.y = parseInt(e.target.value);
				break;
		}
	}

	function deleteResolution() {
		onDelete(key);
	}
</script>

<div class="mt-1 grid grid-cols-12 gap-6">
	<div class="col-span-3 sm:col-span-3 lg:col-span-3">
		<TextInput
			required
			value={key}
			name="imageres-{index}"
			label="Name"
			readonly={!editing}
			onInput={(event) => {
				newKey = event.target.value;
			}} />
	</div>
	<div class="col-span-2 sm:col-span-2 lg:col-span-2">
		<NumberInput
			bind:value={xS}
			name="imageres-{index}-x"
			label="X"
			readonly={!editing}
			onInput={(event) => {
				updateDimension('x', event);
			}} />
	</div>
	<div class="col-span-2 sm:col-span-2 lg:col-span-2">
		<NumberInput
			bind:value={yS}
			name="imageres-{index}-y"
			label="Y"
			readonly={!editing}
			onInput={(event) => {
				updateDimension('y', event);
			}} />
	</div>
	<div class="col-span-3 mt-7 sm:col-span-3 lg:col-span-3">
		<span on:click={clickEdit} on:keypress={clickEdit}>{editSave}</span>
		{#if editing}
			<span on:click={cancelEdit} on:keypress={cancelEdit}>Cancel</span>
		{/if}
		<span on:click={openDeleteModal} on:keypress={openDeleteModal}>🗑️</span>
	</div>
</div>

<Modal title="Delete image resolution?" bind:open={deleteImageRes} autoclose size="sm">
	<p>
		Really delete image resolution named '{key}'?
	</p>
	<svelte:fragment slot="footer">
		<button
			type="button"
			class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
			>Cancel</button>
		<button
			type="button"
			on:click={deleteResolution}
			class="rounded bg-red-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
			>Delete</button>
	</svelte:fragment>
</Modal>
