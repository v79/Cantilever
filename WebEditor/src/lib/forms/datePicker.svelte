<script lang="ts">
	
	export let name: string;
	export let required = false;
	export let value: Date;
	export let label: string;
	export let classes = '';

	export let onChange = (e: Event) => {};

	$: classesToApply = `input ${classes}`;
	$: displayDate = value ? value.toISOString().split('T')[0] : '';

	const onInput = (e: Event) => {
		const { target } = e;
		if (target) {
			value = new Date((target as HTMLInputElement).value);
		}
	};
</script>

<label for={name} class="label"
	><span>{label}</span>
	<input
		value={displayDate}
		on:change={onChange}
		on:input={onInput}
		type="date"
		{name}
		id={name}
		{required}
		class={classesToApply}
	/>
</label>
{#if required}
	{#if !value}
		<span class="text-sm text-yellow-200">{label} must not be blank</span>
	{/if}
{/if}
