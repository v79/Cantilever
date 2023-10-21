<script lang="ts">
	import type { FolderNode } from '../../models/structure';

	export let rootFolder: FolderNode;
	export let onClickFn: (srcKey: string) => void;

	$: sorted = rootFolder.depthSort();
</script>

<ul class="w-96 rounded-lg border border-gray-400 bg-white text-slate-900">
	{#each sorted as node}
		{#if node.nodeType === 'folder'}
			{@const folder = node}
			<li
				id={folder.srcKey}
				class="border-grey-800 relative w-full cursor-pointer rounded-lg border-b bg-slate-200 px-6 py-2 hover:bg-slate-200">
				{folder.srcKey}
			</li>
			<svelte:self rootFolder={folder} {onClickFn} />
		{:else}
			{@const page = node}
			<li
				id={page.srcKey}
				on:keyup={() => onClickFn(page.srcKey)}
				on:click={() => onClickFn(page.srcKey)}
				class="border-grey-400 relative w-full cursor-pointer border-b px-10 py-2
hover:bg-slate-200">
				{page.title}
			</li>
		{/if}
	{/each}
</ul>
