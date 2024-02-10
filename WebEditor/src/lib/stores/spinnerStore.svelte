<script lang="ts" context="module">
	import { ProgressBar } from '@skeletonlabs/skeleton';
	import { writable } from 'svelte/store';

	function createSpinnerStore() {
		const { subscribe, set, update } = writable({ value: false, label: 'Loading...' });

		return {
			subscribe,
			set,
			update,
			show: (label: string) => set({ value: true, label }),
			hide: () => set({ value: false, label: 'Loading...' })
		};
	}

	export const spinner = createSpinnerStore();
</script>

{#if $spinner.value}
	<ProgressBar value={undefined} />
	<p>{$spinner.label}</p>
{/if}
