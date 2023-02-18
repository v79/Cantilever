<script lang="ts">
	export let shown = false;
	export let spinnerId: string;
	export let message: string = '';
</script>

{#if shown}
	<div
		id={spinnerId}
		aria-hidden="true"
		class="absolute top-[50%] left-0 flex h-40 w-full items-center justify-center overflow-y-auto overflow-x-hidden bg-white bg-opacity-70">
		<div
			class="spinner-border inline-block h-8 w-8 animate-spin rounded-full border-4 pr-4"
			role="status" />
		<div class="pl-4">
			{#if message}
				<span>{message}</span>
			{:else}
				<span class="visually-hidden">Loading...</span>
			{/if}
		</div>
	</div>
{/if}
