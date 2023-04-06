<script context="module" lang="ts">
    import {writable} from 'svelte/store';
    import {type MarkdownPost, Post} from '../models/structure';

    function createMarkdownStore() {
		const { subscribe, set, update } = writable<MarkdownPost>();
		return {
			subscribe,
			set,
			update,
			clear: () => set(CLEAR_POST)
		};
	}

	export const markdownStore = createMarkdownStore();

	export function createSlug(title: string) {
		// const invalid: RegExp = new RegExp(';/?:@&=+$, ', 'g');
		const invalid = /[;\/?:@%&=+$, ]/g;
		return title.toLowerCase().replaceAll(invalid, '-');
	}

	export const CLEAR_POST = {
		post: new Post('', '', 'post', '', new Date(), new Date()),
		body: ''
	};
</script>
