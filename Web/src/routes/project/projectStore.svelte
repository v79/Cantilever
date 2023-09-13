<script context="module" lang="ts">
	import { writable } from 'svelte/store';
	import type { CantileverProject, ImgRes } from '../../models/structure';

	export const projectStore = writable<CantileverProject>({
		projectName: '',
		author: '',
		dateFormat: 'dd/MM/yyyy',
		dateTimeFormat: 'HH:mm dd/MM/yyyy',
		imageResolutions: new Map<string, ImgRes>()
	});
</script>
