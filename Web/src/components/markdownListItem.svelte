<script lang="ts">
	import { activeStore } from '../stores/appStatusStore.svelte';
	import type { Post, Page } from '../models/structure';
	export let item: Post | Page;
	export let onClickFn: (srcKey: string) => void;

	let hovering = false;
</script>

<li
	id={item.srcKey}
	class="border-grey-400 relative w-full cursor-pointer border-b px-6 py-2 hover:bg-slate-200 {$activeStore.activeFile ===
	item.srcKey
		? 'bg-slate-100'
		: ''} "
	on:mouseover={() => {
		hovering = true;
	}}
	on:mouseleave={() => {
		hovering = false;
	}}
	on:focus={null}
	on:keyup={() => onClickFn(item.srcKey)}
	on:click={() => onClickFn(item.srcKey)}>
	{item.title}
	{#if hovering}<span class="absolute top-0 right-4"
			><small class="text-sm text-slate-400">{item.getDateString()}</small></span
		>{/if}
</li>
