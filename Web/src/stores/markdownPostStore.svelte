<script context="module" lang="ts">
    import {writable} from 'svelte/store';
    import type {MarkdownPost} from '../models/structure';

    export const markdownStore = writable<MarkdownPost>();

	export function createSlug(title: string) {
		// const invalid: RegExp = new RegExp(';/?:@&=+$, ', 'g');
		const invalid = /[;\/?:@%&=+$, ]/g;
		return title.toLowerCase().replaceAll(invalid, '-');
	}

	export const CLEAR_POST = {
		body: '',
		post: { title: '', srcKey: '', url: '', templateKey: '', date: '', lastUpdated: '' }
	};
</script>
