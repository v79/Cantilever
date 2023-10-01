<script lang="ts">
	import type { FolderNode } from '../../models/structure';

	export let rootFolder: FolderNode;
</script>

<ul class="w-96 rounded-lg border border-gray-400 bg-white text-slate-900">
	{#each rootFolder.children as node}
		{#if node.type === 'folder'}
			{@const folder = node}
			<li>{folder.srcKey}</li>
		{/if}
	{/each}
</ul>
