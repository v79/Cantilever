<script lang="ts">
	import { RecursiveTreeView, type TreeViewNode } from '@skeletonlabs/skeleton';

	export let nodes: TreeViewNode[];
	export let expandedNodes: string[];
	export let onClickFn: (srcKey: string) => void;

	function onClick(event: CustomEvent<{ id: string }>) {
		onClickFn(event.detail.id);
	}
</script>

<div class="listHeight overflow-y-auto">
	<RecursiveTreeView on:click={onClick} {nodes} {expandedNodes} spacing="space-y-0" padding="py-2 pr-4" />
</div>

<style>
	.listHeight {
		max-height: 700px;
	}
</style>
