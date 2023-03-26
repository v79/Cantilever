<script context="module" lang="ts">
    import {writable} from 'svelte/store';
    import type {AppStatus} from '../models/appStatus';

    export const activeStore = writable<AppStatus>({
		activeFile: '',
		isNewFile: false,
		hasChanged: false,
		isValid: false,
		currentPage: 'Posts',
		newSlug: ''
	});
</script>
