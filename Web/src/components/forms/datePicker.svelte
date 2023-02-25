<script lang="ts">
	export let name: string;
	export let required: boolean = false;
	export let value: string;
	export let label: string;

	export let onChange = (e: Event) => {};

	const onInput = (e: Event) => (value = e.target.value);
</script>

<label for={name} class="block text-sm font-medium text-slate-200">{label}</label>
<input
	{value}
	on:change={onChange}
	on:input={onInput}
	type="date"
	{name}
	id={name}
	{required}
	class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm" />
{#if required}
	{#if value === ''}
		<span class="text-sm text-yellow-200">{label} must not be blank</span>
	{/if}
{/if}
