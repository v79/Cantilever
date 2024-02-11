<script lang="ts">
	export let name: string;
	export let srcKey: string;
</script>

<div class="flex flex-row justify-between w-full">
	<span class="text-left font-bold">{name}</span>
</div>
<span class="text-sm">{srcKey}</span>
