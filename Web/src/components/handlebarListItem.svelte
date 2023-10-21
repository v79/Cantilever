<script lang="ts">
	import { activeStore } from '../stores/appStatusStore.svelte';
	import type { HandlebarsItem } from '../models/structure';
	export let item: HandlebarsItem;
	export let onClickFn: (srcKey: string) => void;

	let hovering = false;
</script>

<li
	id={item.key}
	class="border-grey-400 relative w-full cursor-pointer border-b px-6 py-2 hover:bg-slate-200 {$activeStore.activeFile ===
	item.key
		? 'bg-slate-100'
		: ''} "
	on:mouseover={() => {
		hovering = true;
	}}
	on:mouseleave={() => {
		hovering = false;
	}}
	on:focus={null}
	on:keyup={() => onClickFn(item.key)}
	on:click={() => onClickFn(item.key)}>
	{item.name}
	<br />
	<span class="text sm text-slate-400">{item.shortName}</span>
	{#if hovering}<span class="absolute right-4 top-0"
			><small class="text-sm text-slate-400">{item.getDateString()}</small></span
		>{/if}
</li>
