<script lang="ts">
	import type { Post } from '../models/structure';
	import { activeStore } from '../stores/appStatusStore.svelte';
	export let post: Post;
	export let onClickFn: (srcKey: string) => void;

	let hovering = false;
	let postDateString = new Date(post.date).toLocaleDateString('en-GB');
</script>

<li
	id={post.srcKey}
	class="border-grey-400 relative w-full cursor-pointer border-b px-6 py-2 hover:bg-slate-200 {$activeStore.activeFile ===
	post.srcKey
		? 'bg-slate-100'
		: ''} "
	on:mouseover={() => {
		hovering = true;
	}}
	on:mouseleave={() => {
		hovering = false;
	}}
	on:focus={null}
	on:keyup={() => onClickFn(post.srcKey)}
	on:click={() => onClickFn(post.srcKey)}>
	{post.title}{#if hovering}<span class="absolute top-0 right-4"
			><small class="text-sm text-slate-400">{postDateString}</small></span
		>{/if}
</li>
