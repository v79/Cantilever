<script context="module" lang="ts">
	/** This store manages the active file and the current page, plus a few other useful statuses */
	import { writable } from 'svelte/store';
	import type { AppStatus } from '../models/appStatus';

	export const activeStore = writable<AppStatus>({
		activeFile: '',
		isNewFile: false,
		hasChanged: false,
		isValid: false,
		currentPage: 'Posts',
		newSlug: '',
		fileType: null,
		folder: null
	});

	export const AS_CLEAR: AppStatus = {
		activeFile: '',
		isNewFile: false,
		hasChanged: false,
		isValid: true,
		currentPage: '',
		newSlug: '',
		fileType: null,
		folder: null
	};
</script>
