<script context="module" lang="ts">
	import { writable } from 'svelte/store';
	import { HandlebarsContent } from '../models/structure';

	function createHandlebarStore() {
		const { subscribe, set, update } = writable<HandlebarsContent>(new HandlebarsContent(null, ''));
		return {
			subscribe,
			set,
			update,
			clear: () => set(HB_CLEAR)
		};
	}

	export const handlebarStore = createHandlebarStore();

	export const HB_CLEAR = new HandlebarsContent(null, '');
</script>
