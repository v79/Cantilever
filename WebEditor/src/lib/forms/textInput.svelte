<script lang="ts">
	import { createEventDispatcher } from 'svelte';
	import { Icon } from 'svelte-google-materialdesign-icons';
	import type { iconConfigType } from './textInputIconType';
	export let disabled = false;
	export let name: string;
	export let value: string;
	export let label: string;
	export let required = false;
	export let readonly = false;
	export let placeholder = '';
	export let onChange = (e: Event) => {};
	export let classes = '';
	export let iconLeft: iconConfigType | undefined = undefined;
	export let iconRight: iconConfigType | undefined = undefined;

	$: classesToApply = `input ${classes}`;
	$: hasIcons = iconLeft || iconRight;
	export let onInput: (e: Event) => void = (e: Event) => {
		const { target } = e;
		if (target) {
			value = (target as HTMLInputElement).value;
		}
	};

	// forward message events to parent as we don't want to handle icon clicks here
	const dispatch = createEventDispatcher();
	function handleIconClick(button: string) {
		dispatch('message', button);
	}
</script>

<label class="label" for={name}>
	<span>{label}</span>
	<div class={hasIcons ? 'input-group input-group-divider grid-cols-[1fr_auto]' : ''}>
		{#if iconLeft}
			<button
				class="input-group-shim"
				on:click={(e) => {
					handleIconClick('left');
				}}>
				<Icon icon={iconLeft.icon} variation={iconLeft.variation} />
			</button>
		{/if}
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
			on:input={onInput} />
		{#if iconRight}
			<button
				class="input-group-shim"
				on:click={(e) => {
					handleIconClick('right');
				}}>
				<Icon icon={iconRight.icon} variation={iconRight.variation} />
			</button>
		{/if}
	</div>
</label>
{#if required}
	{#if value === ''}
		<span class="text-sm text-yellow-200">{label} must not be blank</span>
	{/if}
{/if}
