<script lang="ts">
	import { Delete_forever, Icon } from 'svelte-google-materialdesign-icons';

	export let title: string;
	export let count: number;
	export let srcKey: string;
	export let onDelete: (srcKey: string) => {};

	let isHovered = false;
	$: showDeleteIcon = isHovered && count === 0;
	let disabledColor = '';
	$: {
		if (count === 0) {
			disabledColor = 'text-surface-600';
		} else {
			disabledColor = '';
		}
	}
</script>

<div
	class="flex flex-row justify-between w-full"
	on:focus={(e) => {}}
	on:mouseover={(e) => {
		isHovered = true;
	}}
	on:mouseleave={(e) => {
		isHovered = false;
	}}
	role="list"
>
	<span class="text-left font-bold {disabledColor}">{title}</span>
	{#if showDeleteIcon}
		<Icon icon={Delete_forever} variation="two-tone" class="text-error-600" on:click={(e) => onDelete(srcKey)}/>
	{:else}
		<span class="text-secondary-200 text-right text-sm {disabledColor}">{count}</span>
	{/if}
</div>
