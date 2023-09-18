<script lang="ts">
	import TextInput from '../../components/forms/textInput.svelte';
	import { onMount } from 'svelte';
	import { Modal } from 'flowbite-svelte';

	export let index: number;
	export let key: string;
	export let value: string;
	export let readonly: boolean = true;
	export let onUpdate = (oldKey: string, newKey: string, newValue: string) => {};
	export let onDelete = (key: string) => {};

	let editing: boolean = false;
	let deleteAttribute: boolean = false;
	let newKey = key;
	let newValue = value;
	$: editSave = editing ? 'Update' : 'Edit';

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
		deleteAttribute = true;
	}

	function saveChanges() {
		onUpdate(key, newKey, newValue);
	}

	function deleteCustomAttribute() {
		onDelete(newKey);
	}

	function updateKey(event: Event) {
		const { target } = event;
		if (target) {
			newKey = (target as HTMLInputElement).value;
		}
	}
	function updateValue(event: Event) {
		const { target } = event;
		if (target) {
			newValue = (target as HTMLInputElement).value;
		}
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
			onInput={updateKey} />
	</div>
	<div class="col-span-3 sm:col-span-3 lg:col-span-3">
		<TextInput
			bind:value
			name="imageres-{index}-x"
			label="Value"
			readonly={!editing}
			onInput={updateValue} />
	</div>
	<div class="col-span-3 mt-7 sm:col-span-3 lg:col-span-3">
		<span on:click={clickEdit} on:keypress={clickEdit}>{editSave}</span>
		{#if editing}
			<span on:click={cancelEdit} on:keypress={cancelEdit}>Cancel</span>
		{/if}
		<span on:click={openDeleteModal} on:keypress={openDeleteModal}>🗑️</span>
	</div>
</div>

<Modal title="Delete custom attribute?" bind:open={deleteAttribute} autoclose size="sm">
	<p>
		Really delete custom attribute named '{newKey}'?
	</p>
	<svelte:fragment slot="footer">
		<button
			type="button"
			class="rounded bg-purple-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
			>Cancel</button>
		<button
			type="button"
			on:click={deleteCustomAttribute}
			class="rounded bg-red-600 px-6 py-2.5 text-xs font-medium uppercase leading-tight text-white shadow-md transition duration-150 ease-in-out hover:bg-purple-700 hover:shadow-lg focus:bg-purple-700 focus:shadow-lg focus:outline-none focus:ring-0 active:bg-purple-800 active:shadow-lg"
			>Delete</button>
	</svelte:fragment>
</Modal>
