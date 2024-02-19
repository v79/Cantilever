<script lang="ts">
	import { project } from '$lib/stores/projectStore.svelte';

	export let name: string;
	export let srcKey: string;

	let displayKey = srcKey.split($project.domain + '/')[1];
</script>

<div class="flex flex-row justify-between w-full">
	<span class="text-left font-bold">{name}</span>
</div>
<span class="text-sm">{displayKey}</span>
