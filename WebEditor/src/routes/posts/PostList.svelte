<script lang="ts">
	import { RecursiveTreeView, type TreeViewNode } from '@skeletonlabs/skeleton';

	export let nodes: TreeViewNode[];
	export let onClickFn: (srcKey: string) => void;

	function onClick(event: CustomEvent<{ id: string }>) {
		onClickFn(event.detail.id);
	}
</script>

<RecursiveTreeView on:click={onClick} {nodes} spacing="space-y-0" padding="py-2 px-2" />
