<script lang="ts">
	export let disabled = false;
	export let name: string;
	export let value: string;
	export let label: string;
	export let required = false;
	export let readonly = false;
	export let placeholder = '';
	export let onChange = (e: Event) => {};
	export let classes = '';

	$: classesToApply = `input ${classes}`;
	export let onInput: (e: Event) => void = (e: Event) => {
		const { target } = e;
		if (target) {
			value = (target as HTMLInputElement).value;
		}
	};
</script>

<label class="label" for={name}>
    <span>{label}</span>
	<input
		class={classesToApply}
		type="text"
		{value}
		{name}
		{disabled}
		id={name}
		{required}
		{placeholder}
		readonly={readonly || null}
		on:change={onChange}
		on:input={onInput}
	/>
</label>
{#if required}
	{#if value === ''}
		<span class="text-sm text-yellow-200">{label} must not be blank</span>
	{/if}
{/if}
