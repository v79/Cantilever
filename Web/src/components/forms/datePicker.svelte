<script lang="ts">
	export let name: string;
	export let required = false;
	export let value: Date;
	export let label: string;

	export let onChange = (e: Event) => {};

	const onInput = (e: Event) => {
		const { target } = e;
		if (target) {
			// value is a Date. The input element returns a string formatted "YYYY-MM-dd"
			// the backend Kotlin model uses LocalDate for this field but there's no equivalent in Javascript/Typescript
			// hence the error here. Works at runtime.
			// @ts-ignore
			value = (target as HTMLInputElement).value;
		}
	};
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
	{#if !value}
		<span class="text-sm text-yellow-200">{label} must not be blank</span>
	{/if}
{/if}
