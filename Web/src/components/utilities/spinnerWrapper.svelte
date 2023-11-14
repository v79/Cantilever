<script lang="ts" context="module">
	export const spinnerStore = writable({ shown: false, message: '' });
	/* 	function createSpinnerStore() {
		const { subscribe, set, update } = writable({ shown: false, message: '' });
		return {
			subscribe,
			set,
			update,
			clear: () => set({ shown: false, message: '' })
		};
	}

	export const spinnerStore = createSpinnerStore(); */
</script>

<script lang="ts">
	import { Spinner } from 'flowbite-svelte';
	import { writable } from 'svelte/store';
	import { fade } from 'svelte/transition';
	export let spinnerID = 'globalSpinner';
</script>

{#if $spinnerStore.shown}
	<div
		out:fade={{ duration: 500 }}
		id={spinnerID}
		aria-hidden="true"
		class="absolute left-1/3 top-[50%] z-50 flex h-40 w-1/3 items-center justify-center overflow-y-auto overflow-x-hidden rounded-md bg-white bg-opacity-90">
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
