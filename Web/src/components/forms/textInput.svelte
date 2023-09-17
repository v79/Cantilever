<script lang="ts">
	import { onMount } from 'svelte';

	export let disabled: boolean = false;
	export let name: string;
	export let value: string;
	export let label: string;
	export let required: boolean = false;
	export let readonly: boolean = false;
	export let onChange = (e: Event) => {};
	export let classes = 'mt-1 block w-full rounded-md border-gray-300 sm:text-sm';

	export let onInput: (e: Event) => void = (e: Event) => {
		const { target } = e;
		if (target) {
			value = (target as HTMLInputElement).value;
		}
	};

	let classesToApply: string = '';
	$: if (readonly) {
		classesToApply = classes + ' bg-slate-200 text-slate-500 focus:border-gray-300';
	} else {
		classesToApply = classes + ' focus:border-indigo-500 focus:ring-indigo-500';
	}
</script>

<label for={name} class="block text-sm font-medium text-slate-200">{label}</label>
<input
	{value}
	type="text"
	{name}
	{disabled}
	id={name}
	{required}
	readonly={readonly || null}
	autocomplete={name}
	on:change={onChange}
	on:input={onInput}
	class={classesToApply} />
{#if required}
	{#if value === ''}
		<span class="text-sm text-yellow-200">{label} must not be blank</span>
	{/if}
{/if}
