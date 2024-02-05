<script lang="ts" context="module">
	import { writable } from 'svelte/store';
	import { MarkdownContent } from '$lib/models/markdown';
	import { TemplateNode } from '$lib/models/templates.svelte';

	//@ts-ignore
	export const CLEAR_MARKDOWN = new MarkdownContent(null, '');
	export const CLEAR_HANDLEBARS = new TemplateNode('', new Date(), '', [], '');

	function createMarkdownStore() {
		//@ts-ignore
		const { subscribe, set, update } = writable<MarkdownContent>(new MarkdownContent(null, ''));
		return {
			subscribe,
			set,
			update,
			clear: () => set(CLEAR_MARKDOWN)
		};
	}

	/** This store manages the markdown content for the editor, i.e the content of the current Page or Post */
	export const markdownStore = createMarkdownStore();

	export function createSlug(title: string) {
		// const invalid: RegExp = new RegExp(';/?:@&=+$, ', 'g');
		const invalid = /[;\/?:@%&=+$, ]/g;
		return title.toLowerCase().replaceAll(invalid, '-');
	}

	function createHandlebarsStore() {
		//@ts-ignore
		const { subscribe, set, update } = writable<TemplateNode>(CLEAR_HANDLEBARS);
		return {
			subscribe,
			set,
			update,
			clear: () => set(CLEAR_HANDLEBARS)
		};
	}
	/** This store manages the handlebars content for the editor, i.e the content of the current Template */
	export const handlebars = createHandlebarsStore();
</script>
