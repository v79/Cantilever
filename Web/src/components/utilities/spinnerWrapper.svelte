<script lang="ts" context="module">
	export const spinnerStore = writable({ shown: false, message: '' });
</script>

<script lang="ts">
	import { Spinner } from 'flowbite-svelte';
	import { writable } from 'svelte/store';
	import { fade } from 'svelte/transition';
	export let spinnerID: string = 'globalSpinner';
</script>

{#if $spinnerStore.shown}
	<div
		out:fade={{ duration: 1000 }}
		id={spinnerID}
		aria-hidden="true"
		class="absolute top-[50%] left-0 z-50 flex h-40 w-full items-center justify-center overflow-y-auto overflow-x-hidden bg-white bg-opacity-70">
		<Spinner />
		<p class="px-2 text-lg font-bold">
			{#if $spinnerStore.message}{$spinnerStore.message}{:else}Processing...{/if}
		</p>
	</div>
{/if}

<!--	<div
			class="spinner-border inline-block h-8 w-8 animate-spin rounded-full border-4 pr-4"
			role="status" />
		<div class="pl-4">
			{#if message}
				<span>{message}</span>
			{:else}
				<span class="visually-hidden">Loading...</span>
			{/if}
		</div>
	

-->
