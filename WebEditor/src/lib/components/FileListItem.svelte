<script lang="ts">
	export let title: string;
	export let date: string;
	export let srcKey: string;
</script>

<div class="flex flex-row justify-between w-full">
	<span class="text-left font-bold">{title}</span>
	<span class="text-secondary-200 text-right text-sm">{date}</span>
</div>
<span class="text-sm">{srcKey}</span>
