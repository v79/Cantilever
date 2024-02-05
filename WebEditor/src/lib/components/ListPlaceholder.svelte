<script lang="ts">
	export let rows = 5;
    export let label: string | undefined;
</script>

<div class="card w-full">
	<div class="p-4 space-y-4">
        {#if label} <div class="placeholder animate-pulse text-center text-sm">{label}</div>{/if}
		{#each Array(rows) as _, i}
			<div class="grid grid-cols-2 gap-2 w-56">
				<div class="placeholder animate-pulse w-9/12"></div>
				<div class="placeholder w-3/12" />
			</div>
			<div class="placeholder animate-pulse w-full" />
		{/each}
	</div>
</div>
