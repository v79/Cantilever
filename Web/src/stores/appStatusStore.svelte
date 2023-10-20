<script context="module" lang="ts">
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
