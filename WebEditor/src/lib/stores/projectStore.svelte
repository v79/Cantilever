<script context="module" lang="ts">
	import { writable } from 'svelte/store';
	import type { CantileverProject, ImgRes } from '$lib/models/project';

	export const project = writable<CantileverProject>({
		name: '',
		author: '',
		dateFormat: 'dd/MM/yyyy',
		dateTimeFormat: 'HH:mm dd/MM/yyyy',
		imageResolutions: new Map<string, ImgRes>(),
		attributes: new Map<string, string>()
	});

    export async function fetchProject(token: string): Promise<CantileverProject | Error> {
        return new Error('Not implemented');
    }
</script>
