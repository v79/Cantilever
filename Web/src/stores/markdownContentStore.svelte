<script context="module" lang="ts">
	import { writable } from 'svelte/store';
	import { MarkdownContent } from '../models/structure';

	function createMarkdownStore() {
		//@ts-ignore
		const { subscribe, set, update } = writable<MarkdownContent>(new MarkdownContent(null, ''));
		return {
			subscribe,
			set,
			update,
			clear: () => set(CLEAR)
		};
	}

	export const markdownStore = createMarkdownStore();

	export function createSlug(title: string) {
		// const invalid: RegExp = new RegExp(';/?:@&=+$, ', 'g');
		const invalid = /[;\/?:@%&=+$, ]/g;
		return title.toLowerCase().replaceAll(invalid, '-');
	}

	//@ts-ignore
	export const CLEAR = new MarkdownContent(null, '');
</script>
